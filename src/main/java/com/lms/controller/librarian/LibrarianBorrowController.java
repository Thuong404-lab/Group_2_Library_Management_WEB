package com.lms.controller.librarian;
import com.lms.exception.ApplicationException;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.Borrow;
import com.lms.entity.Transaction;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.service.BorrowService;
import com.lms.repository.SystemSettingRepository;
import com.lms.entity.SystemSetting;
import com.lms.service.PayOsPaymentService;
import com.lms.service.FinancialService;
import com.lms.entity.PayOsPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.lms.entity.BorrowDetail;

@Controller
public class LibrarianBorrowController {

    private final BorrowService borrowService;
    private final BorrowRepository borrowRepository;
    private final MemberRepository memberRepository;
    private final com.lms.repository.BookItemRepository bookItemRepository;
    private final com.lms.repository.BorrowDetailRepository borrowDetailRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PayOsPaymentService payOsPaymentService;
    private final FinancialService financialService;
    private final com.lms.repository.TransactionRepository transactionRepository;
    private final com.lms.repository.WalletRepository walletRepository;
    private final com.lms.repository.MemberAccountRepository memberAccountRepository;

    public LibrarianBorrowController(BorrowService borrowService,
                                     BorrowRepository borrowRepository,
                                     MemberRepository memberRepository,
                                     com.lms.repository.BookItemRepository bookItemRepository,
                                     com.lms.repository.BorrowDetailRepository borrowDetailRepository,
                                     SystemSettingRepository systemSettingRepository,
                                     PayOsPaymentService payOsPaymentService,
                                     FinancialService financialService,
                                     com.lms.repository.TransactionRepository transactionRepository,
                                     com.lms.repository.WalletRepository walletRepository,
                                     com.lms.repository.MemberAccountRepository memberAccountRepository) {
        this.borrowService = borrowService;
        this.borrowRepository = borrowRepository;
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.payOsPaymentService = payOsPaymentService;
        this.financialService = financialService;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.memberAccountRepository = memberAccountRepository;
    }

    // Xem danh sách toàn cục các phiếu mượn trả
    @GetMapping("/librarian/borrow/list")
    public String listAllBorrows(@RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("borrowDate").descending());
        Page<Borrow> borrowPage;

        // Xử lý bộ lọc dữ liệu kết hợp điều kiện tìm kiếm động từ Repository
        if (status != null && !status.trim().isEmpty()) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                borrowPage = borrowRepository.findByStatusAndKeyword(status.trim(), keyword.trim(), pageable);
            } else {
                borrowPage = borrowRepository.findByStatus(status.trim(), pageable);
            }
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            borrowPage = borrowRepository.findByKeyword(keyword.trim(), pageable);
        } else {
            borrowPage = borrowRepository.findAll(pageable);
        }

        // Đổ toàn bộ tham số đồng bộ ra view borrow-list.html
        model.addAttribute("borrows", borrowPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", borrowPage.getTotalPages());
        model.addAttribute("totalItems", borrowPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "librarian/borrow-list";
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
            } catch (ApplicationException e) {
                model.addAttribute("errorMessage", "Không thể lấy thông tin chi tiết: " + e.getMessage());
            }
        }
        
        if (renewId != null) {
            try {
                model.addAttribute("selectedRenewal", borrowService.getBorrowDetailById(renewId));
            } catch (ApplicationException e) {
                model.addAttribute("errorMessage", "Không thể lấy thông tin gia hạn: " + e.getMessage());
            }
        }

        if (reservationId != null) {
            try {
                model.addAttribute("selectedReservation", borrowService.getReservationById(reservationId));
            } catch (ApplicationException e) {
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
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phê duyệt thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // CHỨC NĂNG TỪ CHỐI DUYỆT ĐƠN MƯỢN ONLINE
    @PostMapping("/librarian/borrow/reject/{borrowId}")
    public String rejectMemberRequest(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            borrowService.rejectPendingRequest(borrowId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối cấp sách cho yêu cầu trực tuyến này.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Từ chối thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // XÁC NHẬN NHẬN SÁCH VẬT LÝ: Chuyển trạng thái Waiting_Pickup -> Active + bắt đầu tính giờ mượn
    @PostMapping("/librarian/borrow/pickup/{borrowId}")
    public String confirmPhysicalPickup(@PathVariable("borrowId") Integer borrowId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.confirmPhysicalPickup(borrowId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận nhận sách vật lý! Phiếu mượn đang hoạt động và bắt đầu tính thời gian.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/list";
    }


    // CHỨC NĂNG TỪ CHỐI ĐƠN ĐẶT TRƯỚC SÁCH
    @PostMapping("/librarian/reservations/reject/{id}")
    public String rejectReservationRequest(@PathVariable("id") Integer id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.rejectReservationRequest(id, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đặt trước thành công.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Từ chối thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }



    // Xem trước thông tin phiếu mượn (Review Bill)
    @PostMapping("/librarian/borrow/review")
    public String reviewCreateBorrow(@RequestParam("memberIdentifier") String memberIdentifier,
                                     @RequestParam(value = "numberOfDays", required = false) Integer numberOfDays,
                                     @RequestParam("rawBarcodes") String rawBarcodes,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (numberOfDays == null || numberOfDays <= 0) {
            numberOfDays = 14; // Giá trị mặc định nếu người dùng nhập rỗng
        }
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

            // Tính phí mượn sách
            BigDecimal baseFee = new BigDecimal("5000"); // Giá mặc định nếu không tìm thấy setting
            try {
                java.util.Optional<SystemSetting> setting = systemSettingRepository.findBySettingKeyIgnoreCase("BORROW_FEE_PER_BOOK");
                if (setting.isPresent()) {
                    baseFee = new BigDecimal(setting.get().getSettingValue().trim());
                }
            } catch (NumberFormatException ignored) {
                // Keep the documented default when the optional setting is malformed.
            }

            int days = (numberOfDays != null ? numberOfDays : 14);
            int books = validItems.size();
            BigDecimal totalBaseFee = baseFee.multiply(new BigDecimal(days)).multiply(new BigDecimal(books));
            
            BigDecimal discount = BigDecimal.ZERO;
            if (member.getTier() != null && member.getTier().getDiscountPercent() != null) {
                discount = member.getTier().getDiscountPercent();
            }
            
            BigDecimal finalFee = totalBaseFee.multiply(new BigDecimal("100").subtract(discount)).divide(new BigDecimal("100"), java.math.RoundingMode.HALF_UP);

            // 3. Truyền dữ liệu sang trang Review
            model.addAttribute("billMember", member);
            model.addAttribute("billBookItems", validItems);
            model.addAttribute("billDays", days);
            model.addAttribute("billRawBarcodes", rawBarcodes);
            model.addAttribute("billMemberIdentifier", memberIdentifier);
            model.addAttribute("billFeePerBookPerDay", baseFee);
            model.addAttribute("billBaseFee", totalBaseFee);
            model.addAttribute("billDiscount", discount);
            model.addAttribute("billFinalFee", finalFee);
            
            return "librarian/review-borrow";
            
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi kiểm tra thông tin: " + e.getMessage());
            return "redirect:/librarian/borrow/create";
        }
    }

    // Xử lý tạo phiếu mượn trực tiếp bằng mã vạch (Barcode) quét tại quầy sau khi xác nhận Bill
    @PostMapping("/librarian/borrow/create")
    public String processCreateBorrow(@ModelAttribute("borrowRequest") BorrowRequest request,
                                      @RequestParam(value = "rawBarcodes", required = false) String rawBarcodes,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        Borrow borrow = null;
        try {
            if (rawBarcodes != null && !rawBarcodes.trim().isEmpty()) {
                request.setBarcodes(Arrays.asList(rawBarcodes.split("\\s*,\\s*")));
            }
            String librarianUsername = (principal != null) ? principal.getName() : "admin";
            borrow = borrowService.processBorrowing(request, librarianUsername);
            
            if ("BANK".equalsIgnoreCase(request.getPaymentMethod())) {
                BigDecimal fee = financialService.calculateBorrowingFeeAmount(borrow.getBorrowId());
                if (fee.compareTo(BigDecimal.ZERO) > 0) {
                    PayOsPayment payment = payOsPaymentService.createBorrowFeeForLibrarian(borrow.getMember(), borrow.getBorrowId());
                    return "redirect:/librarian/payments/payos/" + payment.getOrderCode();
                }
                borrowService.activatePendingBankBorrow(borrow.getBorrowId());
            }
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu mượn trực tiếp tại quầy thành công!");
        } catch (ApplicationException e) {
            if (borrow != null && "BANK".equalsIgnoreCase(request.getPaymentMethod())) {
                try {
                    borrowService.cancelPendingBankBorrow(borrow.getBorrowId(), "FAILED");
                } catch (ApplicationException ignored) {
                    // Preserve the original payment error shown to the librarian.
                }
            }
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
        } catch (ApplicationException e) {
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
                data.put("coverImageUrl", item.getBook().getCoverImageUrl());
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

    @GetMapping("/librarian/api/member-lookup")
    @ResponseBody
    public java.util.Map<String, Object> lookupMember(@RequestParam("identifier") String identifier) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        if (identifier == null || identifier.trim().isEmpty()) {
            response.put("found", false);
            return response;
        }
        
        java.util.Optional<com.lms.entity.Member> optMember = memberRepository.findByUserEmail(identifier.trim());
        if (optMember.isEmpty()) {
            optMember = memberRepository.findByUserPhone(identifier.trim());
        }
        
        if (optMember.isPresent()) {
            com.lms.entity.Member member = optMember.get();
            response.put("found", true);
            response.put("fullName", member.getUser().getFullName());
            response.put("email", member.getUser().getEmail());
            response.put("phone", member.getUser().getPhone());
            response.put("status", member.getUser().getStatus().name());
            
            int maxBorrowLimit = 0;
            if (member.getTier() != null) {
                response.put("memberLevel", member.getTier().getTierName());
                maxBorrowLimit = member.getTier().getBorrowLimit() != null ? member.getTier().getBorrowLimit() : 0;
            } else {
                response.put("memberLevel", "Mặc định");
            }
            response.put("maxBorrowLimit", maxBorrowLimit);
            
            long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
            response.put("currentBorrowCount", currentBorrowCount);
        } else {
            response.put("found", false);
        }
        
        return response;
    }

    @GetMapping("/librarian/borrow/member/{memberId}")
    public String viewMemberBorrowHistory(@PathVariable Integer memberId,
                                          @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                          @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
                                          Model model) {
        com.lms.entity.Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new com.lms.exception.ForbiddenException("Không tìm thấy thành viên hoặc bạn không có quyền xem thông tin này."));

        // Determine user account creation date
        java.time.LocalDateTime minBorrow = borrowRepository.findMinBorrowDateByMemberId(memberId);
        java.time.LocalDateTime minTx = transactionRepository.findMinTransactionDateByMemberId(memberId);
        java.time.LocalDateTime creationDateTime = java.time.LocalDateTime.now().minusDays(30); // fallback default
        if (minBorrow != null && minTx != null) {
            creationDateTime = minBorrow.isBefore(minTx) ? minBorrow : minTx;
        } else if (minBorrow != null) {
            creationDateTime = minBorrow;
        } else if (minTx != null) {
            creationDateTime = minTx;
        }
        LocalDate creationDate = creationDateTime.toLocalDate();
        LocalDate today = LocalDate.now();

        // Enforce date bounds
        LocalDate filterStart = startDate;
        if (filterStart == null) {
            filterStart = creationDate;
        } else {
            if (filterStart.isBefore(creationDate)) {
                filterStart = creationDate;
            }
            if (filterStart.isAfter(today)) {
                filterStart = today;
            }
        }

        LocalDate filterEnd = endDate;
        if (filterEnd == null) {
            filterEnd = today;
        } else {
            if (filterEnd.isBefore(creationDate)) {
                filterEnd = creationDate;
            }
            if (filterEnd.isAfter(today)) {
                filterEnd = today;
            }
        }

        if (filterStart.isAfter(filterEnd)) {
            filterStart = filterEnd;
        }

        // Load filtered list of borrows
        List<Borrow> allBorrows = borrowRepository.findByMember_MemberIdOrderByBorrowDateDesc(memberId);
        LocalDate finalFilterStart = filterStart;
        LocalDate finalFilterEnd = filterEnd;
        List<Borrow> filteredBorrows = allBorrows.stream()
                .filter(b -> {
                    java.time.LocalDateTime bd = b.getBorrowDate();
                    if (bd == null) return false;
                    return !bd.isBefore(finalFilterStart.atStartOfDay()) && !bd.isAfter(finalFilterEnd.atTime(23, 59, 59));
                })
                .toList();

        // Mappings
        java.util.Map<Integer, List<BorrowDetail>> detailsByBorrowId = new java.util.HashMap<>();
        java.util.Map<Integer, List<Transaction>> transactionsByBorrowId = new java.util.HashMap<>();
        for (Borrow borrow : filteredBorrows) {
            detailsByBorrowId.put(borrow.getBorrowId(), borrowDetailRepository.findByBorrowId(borrow.getBorrowId()));
            transactionsByBorrowId.put(borrow.getBorrowId(), transactionRepository.findByBorrow_BorrowId(borrow.getBorrowId()));
        }

        // Stats (all time)
        List<BorrowDetail> allMemberDetails = borrowDetailRepository.findBorrowHistoryLimit365Days(memberId, java.time.LocalDateTime.of(2000, 1, 1, 0, 0));
        int totalBorrows = allMemberDetails.size();
        int completedBorrows = 0;
        int activeBorrows = 0;
        for (BorrowDetail bd : allMemberDetails) {
            if ("Returned".equalsIgnoreCase(bd.getStatus())) {
                completedBorrows++;
            } else if ("Borrowed".equalsIgnoreCase(bd.getStatus()) || "Overdue".equalsIgnoreCase(bd.getStatus()) || "Return_Pending".equalsIgnoreCase(bd.getStatus()) || "Approved".equalsIgnoreCase(bd.getStatus()) || "Waiting_Pickup".equalsIgnoreCase(bd.getStatus())) {
                activeBorrows++;
            }
        }

        com.lms.entity.MemberAccount account = memberAccountRepository.findByMemberMemberId(memberId).orElse(null);
        com.lms.entity.Wallet wallet = walletRepository.findByMemberMemberId(memberId).orElse(null);

        model.addAttribute("member", member);
        model.addAttribute("accountUsername", account != null ? account.getUsername() : "Chưa có");
        model.addAttribute("accountStatus", account != null ? account.getStatus() : "Inactive");
        model.addAttribute("walletBalance", wallet != null && wallet.getBalance() != null ? wallet.getBalance() : java.math.BigDecimal.ZERO);
        
        model.addAttribute("totalBorrows", totalBorrows);
        model.addAttribute("completedBorrows", completedBorrows);
        model.addAttribute("activeBorrows", activeBorrows);

        model.addAttribute("startDate", filterStart.toString());
        model.addAttribute("endDate", filterEnd.toString());
        model.addAttribute("minDate", creationDate.toString());
        model.addAttribute("maxDate", today.toString());

        model.addAttribute("borrows", filteredBorrows);
        model.addAttribute("detailsByBorrowId", detailsByBorrowId);
        model.addAttribute("transactionsByBorrowId", transactionsByBorrowId);
        model.addAttribute("activeMenu", "borrow-schedule");

        return "librarian/borrow-member-detail";
    }
}
