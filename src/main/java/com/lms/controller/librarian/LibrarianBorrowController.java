package com.lms.controller.librarian;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.Borrow;
import com.lms.entity.Member;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Transaction;
import com.lms.entity.MemberAccount;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.LoanService;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
public class LibrarianBorrowController {

    private final LoanService loanService;
    private final BorrowRepository borrowRepository;
    private final MemberRepository memberRepository;
    private final com.lms.repository.BookItemRepository bookItemRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final MemberAccountRepository memberAccountRepository;

    public LibrarianBorrowController(LoanService loanService,
                                     BorrowRepository borrowRepository,
                                     MemberRepository memberRepository,
                                     com.lms.repository.BookItemRepository bookItemRepository,
                                     BorrowDetailRepository borrowDetailRepository,
                                     TransactionRepository transactionRepository,
                                     WalletRepository walletRepository,
                                     MemberAccountRepository memberAccountRepository) {
        this.loanService = loanService;
        this.borrowRepository = borrowRepository;
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.memberAccountRepository = memberAccountRepository;
    }

    @GetMapping("/librarian/loan/borrow-schedule")
    public String listAllBorrows(@RequestParam(value = "tab", defaultValue = "history") String tab,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 Model model) {

        if ("slips".equalsIgnoreCase(tab)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("borrowId").descending());
            Page<Borrow> borrowPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                borrowPage = borrowRepository.findByKeyword(keyword.trim(), pageable);
            } else {
                borrowPage = borrowRepository.findAll(pageable);
            }

            java.util.Map<Integer, List<BorrowDetail>> detailsByBorrowId = new java.util.HashMap<>();
            for (Borrow b : borrowPage.getContent()) {
                detailsByBorrowId.put(b.getBorrowId(), loanService.getBorrowDetailsByBorrowId(b.getBorrowId()));
            }

            model.addAttribute("borrows", borrowPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", borrowPage.getTotalPages());
            model.addAttribute("totalItems", borrowPage.getTotalElements());
            model.addAttribute("keyword", keyword);
            model.addAttribute("detailsByBorrowId", detailsByBorrowId);
            model.addAttribute("activeTab", "slips");
        } else {
            Pageable pageable = PageRequest.of(page, size, Sort.by("memberId").ascending());
            Page<Member> memberPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = keyword.trim();
                memberPage = memberRepository.findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(kw, kw, kw, pageable);
            } else {
                memberPage = memberRepository.findAll(pageable);
            }

            model.addAttribute("members", memberPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", memberPage.getTotalPages());
            model.addAttribute("totalItems", memberPage.getTotalElements());
            model.addAttribute("keyword", keyword);
            model.addAttribute("activeTab", "history");
        }

        return "librarian/borrow-schedule";
    }

    @PostMapping("/librarian/borrow/collect/{borrowId}")
    public String collectBorrowBooks(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            loanService.confirmCollection(borrowId);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận độc giả đã nhận sách vật lý thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/loan/borrow-schedule?tab=slips";
    }

    @GetMapping("/librarian/borrow/member/{memberId}")
    public String viewMemberHistory(
            @PathVariable("memberId") Integer memberId,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            Model model) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy độc giả với ID: " + memberId));

        String fullName = "N/A";
        String email = "N/A";
        String phone = "N/A";

        if (member.getUser() != null) {
            fullName = member.getUser().getFullName() != null ? member.getUser().getFullName() : "N/A";
            email = member.getUser().getEmail() != null ? member.getUser().getEmail() : "N/A";
            phone = member.getUser().getPhone() != null ? member.getUser().getPhone() : "Chưa cập nhật";
        }

        // Tìm mốc thời gian tạo tài khoản (dựa trên hoạt động sớm nhất của member)
        java.time.LocalDateTime earliestBorrow = borrowRepository.findMinBorrowDateByMemberId(memberId);
        java.time.LocalDateTime earliestTx = transactionRepository.findMinTransactionDateByMemberId(memberId);
        java.time.LocalDateTime accountCreatedDate = java.time.LocalDateTime.now();
        if (earliestBorrow != null && earliestBorrow.isBefore(accountCreatedDate)) {
            accountCreatedDate = earliestBorrow;
        }
        if (earliestTx != null && earliestTx.isBefore(accountCreatedDate)) {
            accountCreatedDate = earliestTx;
        }

        java.time.LocalDateTime minAvailableDateTime = accountCreatedDate.with(java.time.LocalTime.MIN);
        java.time.LocalDateTime maxAvailableDateTime = java.time.LocalDateTime.now().with(java.time.LocalTime.MAX);
        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String minDateStr = minAvailableDateTime.format(dateFormatter);
        String maxDateStr = maxAvailableDateTime.format(dateFormatter);

        java.time.LocalDateTime startDateTime = minAvailableDateTime;
        java.time.LocalDateTime endDateTime = maxAvailableDateTime;

        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            try {
                java.time.LocalDateTime parsedStart = java.time.LocalDate.parse(startDateStr.trim(), dateFormatter).atStartOfDay();
                if (!parsedStart.isBefore(minAvailableDateTime) && !parsedStart.isAfter(maxAvailableDateTime)) {
                    startDateTime = parsedStart;
                }
            } catch (Exception e) {
                // Bỏ qua lỗi định dạng
            }
        }
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            try {
                java.time.LocalDateTime parsedEnd = java.time.LocalDate.parse(endDateStr.trim(), dateFormatter).atTime(java.time.LocalTime.MAX);
                if (!parsedEnd.isBefore(minAvailableDateTime) && !parsedEnd.isAfter(maxAvailableDateTime)) {
                    endDateTime = parsedEnd;
                }
            } catch (Exception e) {
                // Bỏ qua lỗi định dạng
            }
        }

        if (startDateTime.isAfter(endDateTime)) {
            java.time.LocalDateTime temp = startDateTime;
            startDateTime = endDateTime.with(java.time.LocalTime.MIN);
            endDateTime = temp.with(java.time.LocalTime.MAX);
        }

        String selectedStartDate = startDateTime.format(dateFormatter);
        String selectedEndDate = endDateTime.format(dateFormatter);

        // 1. Tải danh sách tất cả các đơn Borrow của member theo thứ tự mới nhất lên đầu và lọc theo ngày
        List<Borrow> borrows = borrowRepository.findByMember_MemberIdOrderByBorrowDateDesc(memberId);
        if (borrows == null) {
            borrows = new ArrayList<>();
        }
        final java.time.LocalDateTime finalStart = startDateTime;
        final java.time.LocalDateTime finalEnd = endDateTime;
        borrows = borrows.stream()
                .filter(b -> b.getBorrowDate() != null && !b.getBorrowDate().isBefore(finalStart) && !b.getBorrowDate().isAfter(finalEnd))
                .collect(Collectors.toList());

        List<BorrowDetail> details = loanService.getMemberBorrowDetailsByDateRange(memberId, startDateTime, endDateTime);
        List<Transaction> transactions = loanService.getMemberTransactionsByDateRange(memberId, startDateTime, endDateTime);

        BigDecimal walletBalance = walletRepository.findByMemberMemberId(memberId)
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                .orElse(BigDecimal.ZERO);

        String accountUsername = "N/A";
        String accountStatus = "N/A";

        try {
            java.util.Optional<MemberAccount> optAccount = memberAccountRepository.findByMember(member);
            if (optAccount.isPresent()) {
                accountUsername = optAccount.get().getUsername();
                accountStatus = optAccount.get().getStatus() != null ? optAccount.get().getStatus().toString() : "N/A";
            } else if (member.getUser() != null) {
                java.util.Optional<MemberAccount> optAccountByUserId = memberAccountRepository.findByMember_User_Id(member.getUser().getId());
                if (optAccountByUserId.isPresent()) {
                    accountUsername = optAccountByUserId.get().getUsername();
                    accountStatus = optAccountByUserId.get().getStatus() != null ? optAccountByUserId.get().getStatus().toString() : "N/A";
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi nạp Account: " + e.getMessage());
        }

        Map<Integer, String> authorMap = new HashMap<>();
        if (details != null) {
            for (BorrowDetail d : details) {
                String authorName = "Không rõ tác giả";
                try {
                    if (d.getBookItem() != null && d.getBookItem().getBook() != null
                            && d.getBookItem().getBook().getAuthors() != null
                            && !d.getBookItem().getBook().getAuthors().isEmpty()) {
                        authorName = d.getBookItem().getBook().getAuthors().iterator().next().getAuthorName();
                    } else if (d.getBook() != null && d.getBook().getAuthors() != null
                            && !d.getBook().getAuthors().isEmpty()) {
                        authorName = d.getBook().getAuthors().iterator().next().getAuthorName();
                    }
                } catch (Exception e) {
                    // Tránh lỗi Lazy Loading
                }
                if (d.getBorrowDetailId() != null) {
                    authorMap.put(d.getBorrowDetailId(), authorName);
                }
            }
        }

        // 2. Phân nhóm BorrowDetail và Transaction theo Borrow ID cha để truyền ra View
        Map<Integer, List<BorrowDetail>> detailsByBorrowId = (details != null) ?
                details.stream().filter(d -> d.getBorrow() != null).collect(Collectors.groupingBy(d -> d.getBorrow().getBorrowId())) : new HashMap<>();

        Map<Integer, List<Transaction>> transactionsByBorrowId = (transactions != null) ?
                transactions.stream().filter(t -> t.getBorrow() != null).collect(Collectors.groupingBy(t -> t.getBorrow().getBorrowId())) : new HashMap<>();

        model.addAttribute("member", member);
        model.addAttribute("fullName", fullName);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone);

        // Đăng ký các biến Model yêu cầu bởi view borrow-member-detail.html
        model.addAttribute("borrows", borrows);
        model.addAttribute("detailsByBorrowId", detailsByBorrowId);
        model.addAttribute("transactionsByBorrowId", transactionsByBorrowId);

        model.addAttribute("details", details != null ? details : new ArrayList<>());
        model.addAttribute("authorMap", authorMap);
        model.addAttribute("transactions", transactions != null ? transactions : new ArrayList<>());
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("accountUsername", accountUsername);
        model.addAttribute("accountStatus", accountStatus);

        model.addAttribute("minDate", minDateStr);
        model.addAttribute("maxDate", maxDateStr);
        model.addAttribute("startDate", selectedStartDate);
        model.addAttribute("endDate", selectedEndDate);

        long completedCount = 0;
        long activeCount = 0;
        if (details != null) {
            completedCount = details.stream().filter(d -> "Returned".equalsIgnoreCase(d.getStatus())).count();
            activeCount = details.stream().filter(d -> !"Returned".equalsIgnoreCase(d.getStatus())).count();
        }

        model.addAttribute("totalBorrows", borrows.size());
        model.addAttribute("completedBorrows", completedCount);
        model.addAttribute("activeBorrows", activeCount);

        return "librarian/borrow-member-detail";
    }

    @GetMapping("/librarian/borrow/create")
    public String showCreateBorrowForm(@RequestParam(value = "requestId", required = false) Integer requestId,
                                       @RequestParam(value = "renewId", required = false) Integer renewId,
                                       @RequestParam(value = "reservationId", required = false) Integer reservationId,
                                       Model model) {

        model.addAttribute("borrowRequest", new BorrowRequest());
        model.addAttribute("maxBorrowDays", 14);

        Page<Borrow> pendingPage = borrowRepository.findByStatus("Pending", Pageable.unpaged());
        List<Borrow> pendingRequests = pendingPage != null ? pendingPage.getContent() : new ArrayList<>();
        model.addAttribute("pendingRequests", pendingRequests);

        model.addAttribute("pendingRenewals", loanService.getAllPendingRenewals() != null ? loanService.getAllPendingRenewals() : new ArrayList<>());
        model.addAttribute("pendingReturnRequests", new ArrayList<>());
        model.addAttribute("activeReservations", new ArrayList<>());

        if (requestId != null) {
            try {
                Borrow selectedBorrow = loanService.getLoanDetails(requestId);
                model.addAttribute("selectedRequest", selectedBorrow);
                model.addAttribute("requestDetails", loanService.getBorrowDetailsByBorrowId(requestId));
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không thể lấy thông tin chi tiết: " + e.getMessage());
            }
        }

        if (renewId != null) {
            try {
                model.addAttribute("selectedRenewal", borrowDetailRepository.findById(renewId).orElse(null));
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không thể lấy thông tin gia hạn: " + e.getMessage());
            }
        }

        return "librarian/create-borrow";
    }

    @PostMapping("/librarian/borrow/approve/{borrowId}")
    public String approveMemberRequest(@PathVariable("borrowId") Integer borrowId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            loanService.processBorrowRequest(borrowId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt và cấp sách vật lý thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phê duyệt thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    @PostMapping("/librarian/borrow/reject/{borrowId}")
    public String rejectMemberRequest(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            Borrow borrow = borrowRepository.findById(borrowId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mượn"));
            borrow.setStatus("Rejected");
            borrowRepository.save(borrow);

            List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
            for (BorrowDetail detail : details) {
                detail.setStatus("Rejected");
                borrowDetailRepository.save(detail);
                if (detail.getBookItem() != null) {
                    com.lms.entity.BookItem item = detail.getBookItem();
                    item.setStatus("Available");
                    bookItemRepository.save(item);
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối cấp sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Từ chối thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    @PostMapping("/librarian/borrow/review")
    public String reviewCreateBorrow(@RequestParam("memberIdentifier") String memberIdentifier,
                                     @RequestParam("numberOfDays") Integer numberOfDays,
                                     @RequestParam("rawBarcodes") String rawBarcodes,
                                     RedirectAttributes redirectAttributes) {
        try {
            java.util.Optional<com.lms.entity.Member> optMember = memberRepository.findByUserEmail(memberIdentifier.trim());
            if (optMember.isEmpty()) {
                optMember = memberRepository.findByUserPhone(memberIdentifier.trim());
            }
            if (optMember.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thành viên với SĐT/Email này!");
                return "redirect:/librarian/borrow/create";
            }
            com.lms.entity.Member member = optMember.get();
            if (member.getUser().getStatus() != com.lms.enums.UserStatus.Active) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tài khoản thành viên này đang bị khóa hoặc không hoạt động!");
                return "redirect:/librarian/borrow/create";
            }

            if (rawBarcodes == null || rawBarcodes.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập ít nhất một mã vạch!");
                return "redirect:/librarian/borrow/create";
            }
            java.util.List<String> barcodeList = java.util.Arrays.asList(rawBarcodes.split("\\s*,\\s*"));
            java.util.List<com.lms.entity.BookItem> validItems = new java.util.ArrayList<>();

            for (String barcode : barcodeList) {
                barcode = barcode.trim();
                if (barcode.isEmpty()) continue;
                java.util.Optional<com.lms.entity.BookItem> optItem = bookItemRepository.findByBarcode(barcode);
                if (optItem.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sách có mã vạch: " + barcode);
                    return "redirect:/librarian/borrow/create";
                }
                com.lms.entity.BookItem item = optItem.get();
                if (!"Available".equalsIgnoreCase(item.getStatus())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Sách có mã vạch " + barcode + " hiện không sẵn sàng (Trạng thái: " + item.getStatus() + ")");
                    return "redirect:/librarian/borrow/create";
                }
                validItems.add(item);
            }

            redirectAttributes.addFlashAttribute("showBillPopup", true);
            redirectAttributes.addFlashAttribute("billMember", member);
            redirectAttributes.addFlashAttribute("billBookItems", validItems);
            redirectAttributes.addFlashAttribute("billDays", numberOfDays != null ? numberOfDays : 14);
            redirectAttributes.addFlashAttribute("billRawBarcodes", rawBarcodes);
            redirectAttributes.addFlashAttribute("billMemberIdentifier", memberIdentifier);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi kiểm tra thông tin: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    @PostMapping("/librarian/borrow/create")
    public String processCreateBorrow(@ModelAttribute("borrowRequest") BorrowRequest request,
                                      @RequestParam(value = "rawBarcodes", required = false) String rawBarcodes,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            String raw = (rawBarcodes != null) ? rawBarcodes : request.getMemberIdentifier();
            List<String> barcodes = Arrays.asList(raw.split("\\s*,\\s*"));
            String librarianUsername = (principal != null) ? principal.getName() : "admin";

            loanService.processBorrowDesk(request.getMemberIdentifier(), barcodes, librarianUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu mượn trực tiếp tại quầy thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    @GetMapping("/librarian/api/book-lookup")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> lookupBooks(@RequestParam("barcodes") String barcodes) {
        java.util.List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
        if (barcodes == null || barcodes.trim().isEmpty()) {
            return results;
        }

        String[] barcodeArray = barcodes.split(",");
        for (String bc : barcodeArray) {
            String cleanBc = bc.trim();
            if (cleanBc.isEmpty()) continue;

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("barcode", cleanBc);

            java.util.Optional<com.lms.entity.BookItem> itemOpt = bookItemRepository.findByBarcode(cleanBc);
            if (itemOpt.isPresent()) {
                com.lms.entity.BookItem item = itemOpt.get();
                data.put("found", true);
                data.put("title", item.getBook().getTitle());
                String rawStatus = item.getStatus();
                String vnStatus = "Có sẵn";
                if ("Borrowed".equalsIgnoreCase(rawStatus)) vnStatus = "Đang mượn";
                else if ("Lost".equalsIgnoreCase(rawStatus)) vnStatus = "Bị mất";
                else if ("Maintenance".equalsIgnoreCase(rawStatus)) vnStatus = "Bảo trì";

                data.put("status", vnStatus);
                data.put("rawStatus", rawStatus);
            } else {
                data.put("found", false);
                data.put("error", "Không tồn tại");
            }
            results.add(data);
        }
        return results;
    }
}