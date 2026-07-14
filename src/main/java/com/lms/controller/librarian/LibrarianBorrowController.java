package com.lms.controller.librarian;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.Borrow;
import com.lms.entity.Member;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Transaction;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.service.BorrowService;
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
import java.util.Map;
import java.util.HashMap;

@Controller
public class LibrarianBorrowController {

    private final BorrowService borrowService;
    private final BorrowRepository borrowRepository;
    private final MemberRepository memberRepository;
    private final com.lms.repository.BookItemRepository bookItemRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final MemberAccountRepository memberAccountRepository;

    public LibrarianBorrowController(BorrowService borrowService, BorrowRepository borrowRepository,
                                     MemberRepository memberRepository,
                                     com.lms.repository.BookItemRepository bookItemRepository,
                                     BorrowDetailRepository borrowDetailRepository,
                                     TransactionRepository transactionRepository,
                                     WalletRepository walletRepository,
                                     MemberAccountRepository memberAccountRepository) {
        this.borrowService = borrowService;
        this.borrowRepository = borrowRepository;
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.memberAccountRepository = memberAccountRepository;
    }

    @GetMapping("/librarian/borrow/list")
    public String listAllBorrows(@RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "50") int size,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("memberId").ascending());
        Page<Member> memberPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            memberPage = memberRepository.findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(kw, kw, kw, pageable);
        } else {
            memberPage = memberRepository.findAll(pageable);
        }

        // Đổ toàn bộ tham số đồng bộ ra view borrow-list.html
        model.addAttribute("members", memberPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", memberPage.getTotalPages());
        model.addAttribute("totalItems", memberPage.getTotalElements());
        model.addAttribute("keyword", keyword);

        return "librarian/borrow-list";
    }

    // Xem chi tiết lịch sử mượn trả & giao dịch tài chính của thành viên cụ thể
    @GetMapping("/librarian/borrow/member/{memberId}")
    public String viewMemberHistory(@PathVariable("memberId") Integer memberId, Model model) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy độc giả với ID: " + memberId));

        // Lấy danh sách phiếu mượn của thành viên
        List<Borrow> borrowHistory = borrowRepository.findByMember_MemberIdOrderByBorrowDateDesc(memberId);

        // Lưu bản đồ các chi tiết sách và giao dịch theo từng phiếu mượn để Thymeleaf dễ lặp
        Map<Integer, List<BorrowDetail>> detailsByBorrowId = new HashMap<>();
        Map<Integer, List<Transaction>> transactionsByBorrowId = new HashMap<>();

        for (Borrow borrow : borrowHistory) {
            List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrow.getBorrowId());
            detailsByBorrowId.put(borrow.getBorrowId(), details);

            List<Transaction> transactions = transactionRepository.findByBorrow_BorrowId(borrow.getBorrowId());
            transactionsByBorrowId.put(borrow.getBorrowId(), transactions);
        }

        // Lấy số dư ví thành viên
        BigDecimal walletBalance = walletRepository.findByMemberMemberId(memberId)
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                .orElse(BigDecimal.ZERO);

        // Lấy tên đăng nhập tài khoản thành viên
        String accountUsername = memberAccountRepository
                .findByMember_User_Id(member.getUser().getId())
                .map(acc -> acc.getUsername())
                .orElse("N/A");
        String accountStatus = memberAccountRepository
                .findByMember_User_Id(member.getUser().getId())
                .map(acc -> acc.getStatus())
                .orElse("N/A");

        model.addAttribute("member", member);
        model.addAttribute("borrows", borrowHistory);
        model.addAttribute("detailsByBorrowId", detailsByBorrowId);
        model.addAttribute("transactionsByBorrowId", transactionsByBorrowId);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("accountUsername", accountUsername);
        model.addAttribute("accountStatus", accountStatus);
        model.addAttribute("totalBorrows", borrowHistory.size());
        model.addAttribute("completedBorrows", borrowHistory.stream()
                .filter(b -> "Returned".equalsIgnoreCase(b.getStatus())).count());
        model.addAttribute("activeBorrows", borrowHistory.stream()
                .filter(b -> !"Returned".equalsIgnoreCase(b.getStatus())).count());

        return "librarian/borrow-member-detail";
    }

    // Quầy mượn sách tích hợp đồng bộ danh sách online (Master-Detail) đúng mô hình chia đôi màn hình
    @GetMapping("/librarian/borrow/create")
    public String showCreateBorrowForm(@RequestParam(value = "requestId", required = false) Integer requestId,
                                       @RequestParam(value = "renewId", required = false) Integer renewId,
                                       @RequestParam(value = "reservationId", required = false) Integer reservationId,
                                       Model model) {
        model.addAttribute("borrowRequest", new BorrowRequest());
        model.addAttribute("maxBorrowDays", borrowService.getMaxBorrowDays());

        // Bước 1: Lấy danh sách thành viên gửi đơn mượn trực tuyến đang chờ phê duyệt (Hiện ở cột trái)
        model.addAttribute("pendingRequests", borrowService.getAllPendingRequests());

        // Lấy danh sách yêu cầu gia hạn chờ duyệt
        model.addAttribute("pendingRenewals", borrowService.getPendingRenewalRequests());

        // Đổi tên thuộc tính từ 'returnRequests' -> 'pendingReturnRequests' và bọc qua DTO phù hợp với template
        model.addAttribute("pendingReturnRequests", borrowService.getPendingReturnRequestDTOs());

        // Đổi tên thuộc tính từ 'pendingReservations' -> 'activeReservations' và bọc qua DTO phù hợp với template
        model.addAttribute("activeReservations", borrowService.getPendingReservationDTOs());

        // Bước 2 & 3: Khi click chọn 1 member, nạp thông tin chi tiết và danh sách sách muốn mượn qua cột phải
        if (requestId != null) {
            try {
                Borrow selectedBorrow = borrowService.getBorrowById(requestId);
                model.addAttribute("selectedRequest", selectedBorrow);
                model.addAttribute("requestDetails", borrowService.getBorrowDetailsByBorrowId(requestId));
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không thể lấy thông tin chi tiết: " + e.getMessage());
            }
        }

        if (renewId != null) {
            try {
                model.addAttribute("selectedRenewal", borrowService.getBorrowDetailById(renewId));
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không thể lấy thông tin gia hạn: " + e.getMessage());
            }
        }

        if (reservationId != null) {
            try {
                model.addAttribute("selectedReservation", borrowService.getReservationById(reservationId));
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không thể lấy thông tin đặt trước: " + e.getMessage());
            }
        }

        return "librarian/create-borrow";
    }

    // CHỨC NĂNG PHÊ DUYỆT ĐƠN MƯỢN ONLINE: Chuyển đổi trạng thái từ Pending -> Active
    @PostMapping("/librarian/borrow/approve/{borrowId}")
    public String approveMemberRequest(@PathVariable("borrowId") Integer borrowId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.approvePendingRequest(borrowId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt và cấp sách vật lý thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phê duyệt thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // CHỨC NĂNG TỪ CHỐI DUYỆT ĐƠN MƯỢN ONLINE
    @PostMapping("/librarian/borrow/reject/{borrowId}")
    public String rejectMemberRequest(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            borrowService.updateStatus(borrowId, "Rejected");
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối cấp sách cho yêu cầu trực tuyến này.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Từ chối thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }


    // CHỨC NĂNG TỪ CHỐI ĐƠN ĐẶT TRƯỚC SÁCH
    @PostMapping("/librarian/reservations/reject/{id}")
    public String rejectReservationRequest(@PathVariable("id") Integer id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.rejectReservationRequest(id, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đặt trước thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Từ chối thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }



    // Xem trước thông tin phiếu mượn (Review Bill)
    @PostMapping("/librarian/borrow/review")
    public String reviewCreateBorrow(@RequestParam("memberIdentifier") String memberIdentifier,
                                     @RequestParam("numberOfDays") Integer numberOfDays,
                                     @RequestParam("rawBarcodes") String rawBarcodes,
                                     RedirectAttributes redirectAttributes) {
        try {
            // 1. Tìm thành viên
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

            // 2. Kiểm tra mã vạch
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

            if (validItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không có mã vạch hợp lệ nào được nhập!");
                return "redirect:/librarian/borrow/create";
            }

            // 3. Truyền dữ liệu sang giao diện popup
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

    // Xử lý tạo phiếu mượn trực tiếp bằng mã vạch (Barcode) quét tại quầy sau khi xác nhận Bill
    @PostMapping("/librarian/borrow/create")
    public String processCreateBorrow(@ModelAttribute("borrowRequest") BorrowRequest request,
                                      @RequestParam(value = "rawBarcodes", required = false) String rawBarcodes,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            if (rawBarcodes != null && !rawBarcodes.trim().isEmpty()) {
                request.setBarcodes(Arrays.asList(rawBarcodes.split("\\s*,\\s*")));
            }
            String librarianUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.processBorrowing(request, librarianUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu mượn trực tiếp tại quầy thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // ==========================================
    // ĐÃ XÓA PHƯƠNG THỨC memberRequestReturn BỊ TRÙNG MAPPING TẠI ĐÂY
    // ==========================================

    // ĐỒNG BỘ ENDPOINT THEO FORM HTML PHẦN 2: Phê duyệt đặt giữ chỗ trước từ Thủ thư
    @PostMapping("/librarian/reservations/approve/{reservationId}")
    public String approveReservationRequest(@PathVariable("reservationId") Integer reservationId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.approveReservationRequest(reservationId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt đơn đặt trước sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Duyệt thất bại: " + e.getMessage());
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
