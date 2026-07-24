package com.lms.controller.librarian;

import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

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
import com.lms.util.BorrowCodeFormatter;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Reservation;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class LibrarianBorrowController extends LocalizedControllerSupport {

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
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            Model model) {

        page = Math.max(0, page);
        size = Math.max(1, Math.min(size, 100));

        if ("members".equalsIgnoreCase(tab)) {
            String displayKeyword = keyword == null ? "" : keyword.trim();
            String searchKeyword = displayKeyword;
            if (searchKeyword.regionMatches(true, 0, "MEM-", 0, 4)) {
                searchKeyword = searchKeyword.substring(4).trim();
            }

            Page<com.lms.entity.MemberAccount> accountPage = searchKeyword.isBlank()
                    ? memberAccountRepository.findAll(PageRequest.of(page, 20, Sort.by("member.memberId").ascending()))
                    : memberAccountRepository.searchMemberAccounts(
                            searchKeyword, PageRequest.of(page, 20, Sort.by("member.memberId").ascending()));

            model.addAttribute("accounts", accountPage.getContent());
            model.addAttribute("accountPage", accountPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", 20);
            model.addAttribute("totalPages", accountPage.getTotalPages());
            model.addAttribute("totalItems", accountPage.getTotalElements());
            model.addAttribute("keyword", displayKeyword);
            model.addAttribute("activeTab", "members");
            model.addAttribute("activeMenu", "borrow-list");
            return "librarian/borrow-list";
        }

        java.util.Set<String> allowedStatuses = java.util.Set.of("Active", "Waiting_Pickup", "Returned",
                "Overdue", "Pending", "Rejected", "Return_Pending", "Canceled", "Cancelled",
                "Payment_Pending", "Payment_Expired");
        if (status != null && !status.isBlank() && !allowedStatuses.contains(status.trim())) {
            status = null;
        }
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
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", borrowPage.getTotalPages());
        model.addAttribute("totalItems", borrowPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("activeTab", "all");
        model.addAttribute("activeMenu", "borrow-list");

        return "librarian/borrow-list";
    }

    // Quầy mượn sách tích hợp đồng bộ danh sách online (Master-Detail) đúng mô hình
    // chia đôi màn hình
    @GetMapping("/librarian/borrow/create")
    public String showCreateBorrowForm(@RequestParam(value = "requestId", required = false) Integer requestId,
            @RequestParam(value = "pickupId", required = false) Integer pickupId,
            @RequestParam(value = "renewId", required = false) Integer renewId,
            @RequestParam(value = "reservationId", required = false) Integer reservationId,
            Model model,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        java.util.Map<Integer, String> usernameByMemberId = memberAccountRepository.findAll().stream()
                .filter(ma -> ma.getMember() != null)
                .collect(Collectors.toMap(
                        ma -> ma.getMember().getMemberId(),
                        ma -> ma.getUsername(),
                        (existing, replacement) -> existing));
        model.addAttribute("usernameByMemberId", usernameByMemberId);

        model.addAttribute("borrowRequest", new BorrowRequest());
        model.addAttribute("maxBorrowDays", borrowService.getMaxBorrowDays());

        // Bước 1: Lấy danh sách hàng chờ xử lý
        model.addAttribute("pendingRequests", borrowService.getAllPendingRequests());
        model.addAttribute("pendingPickups", borrowService.getWaitingPickupRequests());
        model.addAttribute("pendingRenewals", borrowService.getPendingRenewalRequests());

        // Đổi tên thuộc tính từ 'returnRequests' -> 'pendingReturnRequests' và bọc qua
        // DTO phù hợp với template
        model.addAttribute("pendingReturnRequests", borrowService.getPendingReturnRequestDTOs());

        // Đổi tên thuộc tính từ 'pendingReservations' -> 'activeReservations' và bọc
        // qua DTO phù hợp với template
        model.addAttribute("activeReservations", borrowService.getPendingReservationDTOs());

        // Bước 2 & 3: Khi click chọn 1 member, nạp thông tin chi tiết và danh sách sách
        // muốn mượn qua cột phải
        if (requestId != null) {
            try {
                Borrow selectedBorrow = borrowService.getBorrowById(requestId);
                if ("Pending".equalsIgnoreCase(selectedBorrow.getStatus())) {
                    model.addAttribute("selectedRequest", selectedBorrow);
                    model.addAttribute("requestDetails", borrowService.getBorrowDetailsByBorrowId(requestId));
                    populateMemberStats(model, selectedBorrow.getMember(), usernameByMemberId);
                }
            } catch (ApplicationException e) {
                model.addAttribute("errorMessage", messageWithDetail("backend.borrow.detailsFailed", e));
            }
        }

        if (pickupId != null) {
            try {
                Borrow selectedPickup = borrowService.getBorrowById(pickupId);
                if (selectedPickup.getStatus() != null && ("Waiting_Pickup".equalsIgnoreCase(selectedPickup.getStatus())
                        || "WAITING_PICKUP".equalsIgnoreCase(selectedPickup.getStatus())
                        || "APPROVED".equalsIgnoreCase(selectedPickup.getStatus()))) {
                    model.addAttribute("selectedPickup", selectedPickup);
                    model.addAttribute("pickupDetails", borrowService.getBorrowDetailsByBorrowId(pickupId));
                    populateMemberStats(model, selectedPickup.getMember(), usernameByMemberId);
                }
            } catch (ApplicationException e) {
                model.addAttribute("errorMessage", messageWithDetail("backend.borrow.detailsFailed", e));
            }
        }

        if (renewId != null) {
            try {
                BorrowDetail selectedRenew = borrowService.getBorrowDetailById(renewId);
                if ("Renew_Pending".equalsIgnoreCase(selectedRenew.getStatus())) {
                    model.addAttribute("selectedRenewal", selectedRenew);
                    transactionRepository
                            .findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
                                    renewId, "RENEWAL_FEE", "Pending")
                            .ifPresent(hold -> {
                                int requestedDays = hold.getRenewalDays() == null ? 0 : hold.getRenewalDays();
                                model.addAttribute("selectedRenewalDays", requestedDays);
                                model.addAttribute("selectedRenewalFee",
                                        hold.getAmount() == null ? BigDecimal.ZERO : hold.getAmount().abs());
                                model.addAttribute("selectedRenewalNewDueDate",
                                        selectedRenew.getDueDate().plusDays(requestedDays));
                            });
                    populateMemberStats(model, selectedRenew.getBorrow().getMember(), usernameByMemberId);
                }
            } catch (ApplicationException e) {
                model.addAttribute("errorMessage", messageWithDetail("backend.borrow.renewalDetailsFailed", e));
            }
        }

        if (reservationId != null) {
            try {
                Reservation selectedReservation = borrowService.getReservationById(reservationId);
                if ("Pending".equalsIgnoreCase(selectedReservation.getStatus())
                        || "Deposit_Paid".equalsIgnoreCase(selectedReservation.getStatus())
                        || "Ready".equalsIgnoreCase(selectedReservation.getStatus())) {
                    model.addAttribute("selectedReservation", selectedReservation);
                    populateMemberStats(model, selectedReservation.getMember(), usernameByMemberId);
                }
            } catch (ApplicationException e) {
                model.addAttribute("errorMessage", messageWithDetail("backend.borrow.reservationDetailsFailed", e));
            }
        }

        model.addAttribute("activeMenu", "borrow-desk");
        return "librarian/create-borrow";
    }

    private void populateMemberStats(Model model, com.lms.entity.Member member, java.util.Map<Integer, String> usernameByMemberId) {
        if (member == null) return;
        Integer memberId = member.getMemberId();
        
        List<String> fineTypes = java.util.Arrays.asList("OVERDUE_FINE", "DAMAGE_COMPENSATION");
        List<com.lms.entity.Transaction> unpaidFinesList = transactionRepository.findUnpaidFineTransactions(memberId, fineTypes);
        java.math.BigDecimal totalUnpaidFines = java.math.BigDecimal.ZERO;
        for (com.lms.entity.Transaction t : unpaidFinesList) {
            if (t.getAmount() != null) {
                totalUnpaidFines = totalUnpaidFines.add(t.getAmount());
            }
        }
        model.addAttribute("unpaidFines", totalUnpaidFines);

        String username = usernameByMemberId.get(memberId);
        if (username != null) {
            int activeBorrowCount = borrowService.getMemberCurrentBorrows(username).size();
            model.addAttribute("activeBorrowCount", activeBorrowCount);
        } else {
            model.addAttribute("activeBorrowCount", 0);
        }
        
        int maxAllowed = (member.getTier() != null && member.getTier().getBorrowLimit() != null) 
                         ? member.getTier().getBorrowLimit() : 0;
        model.addAttribute("maxBorrowLimit", maxAllowed);
    }

    // Approving reserves physical copies; the loan only becomes Active after
    // handover.
    @PostMapping("/librarian/borrow/approve/{borrowId}")
    public String approveMemberRequest(@PathVariable("borrowId") Integer borrowId,
            @RequestParam("barcodes") List<String> barcodes,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.approvePendingRequest(borrowId, barcodes, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.loan.approved"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.action.approveFailed", e));
        }
        return "redirect:/librarian/borrow/create?tab=pickups&pickupId=" + borrowId;
    }

    // CHỨC NĂNG TỪ CHỐI DUYỆT ĐƠN MƯỢN ONLINE
    @PostMapping("/librarian/borrow/reject/{borrowId}")
    public String rejectMemberRequest(@PathVariable("borrowId") Integer borrowId,
            @RequestParam("reasonCode") String reasonCode,
            @RequestParam("reason") String reason,
            RedirectAttributes redirectAttributes) {
        try {
            borrowService.rejectPendingRequest(borrowId, reasonCode, reason);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.loan.rejected"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.action.rejectFailed", e));
        }
        return "redirect:/librarian/borrow/create";
    }

    // XÁC NHẬN NHẬN SÁCH VẬT LÝ: Chuyển trạng thái Waiting_Pickup -> Active + bắt
    // đầu tính giờ mượn
    @PostMapping("/librarian/borrow/pickup/{borrowId}")
    public String confirmPhysicalPickup(@PathVariable("borrowId") Integer borrowId, Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.confirmPhysicalPickup(borrowId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.loan.pickupConfirmed"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.action.failed", e));
        }
        return "redirect:/librarian/borrow/create?tab=pickups";
    }

    // CHỨC NĂNG TỪ CHỐI ĐƠN ĐẶT TRƯỚC SÁCH
    @PostMapping("/librarian/reservations/reject/{id}")
    public String rejectReservationRequest(@PathVariable("id") Integer id,
            @RequestParam("reasonCode") String reasonCode,
            @RequestParam("reason") String reason,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.rejectReservationRequest(id, staffUsername, reasonCode, reason);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.reservation.rejected"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.action.rejectFailed", e));
        }
        return "redirect:/librarian/borrow/create";
    }

    // Xem trước thông tin phiếu mượn (Review Bill)
    @GetMapping("/librarian/borrow/review")
    public String redirectReviewWithoutPreviewData() {
        return "redirect:/librarian/borrow/create";
    }

    @PostMapping("/librarian/borrow/review")
    public String reviewCreateBorrow(@RequestParam("memberIdentifier") String memberIdentifier,
            @RequestParam(value = "numberOfDays", required = false) Integer numberOfDays,
            @RequestParam("rawBarcodes") String rawBarcodes,
            @RequestParam(value = "paymentMethod", required = false, defaultValue = "CASH") String paymentMethod,
            Model model,
            RedirectAttributes redirectAttributes) {
        int maxBorrowDays = borrowService.getMaxBorrowDays();
        if (numberOfDays == null || numberOfDays < 1 || numberOfDays > maxBorrowDays) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.invalidDays", maxBorrowDays));
            return "redirect:/librarian/borrow/create";
        }
        try {
            if (memberIdentifier == null || memberIdentifier.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        message("backend.borrow.memberIdentifierRequired"));
                return "redirect:/librarian/borrow/create";
            }
            // 1. Tìm thành viên
            java.util.Optional<com.lms.entity.Member> optMember = memberRepository
                    .findByUserEmail(memberIdentifier.trim());
            if (optMember.isEmpty()) {
                optMember = memberRepository.findByUserPhone(memberIdentifier.trim());
            }
            if (optMember.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.member.notFoundByIdentifier"));
                return "redirect:/librarian/borrow/create";
            }
            com.lms.entity.Member member = optMember.get();
            if (member.getUser() == null || member.getUser().getStatus() != com.lms.enums.UserStatus.Active) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.member.inactive"));
                return "redirect:/librarian/borrow/create";
            }
            List<String> eligibilityReasons = getBorrowIneligibilityReasons(member);
            if (!eligibilityReasons.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", eligibilityReasons.get(0));
                return "redirect:/librarian/borrow/create";
            }

            // 2. Kiểm tra mã vạch
            if (rawBarcodes == null || rawBarcodes.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.barcode.required"));
                return "redirect:/librarian/borrow/create";
            }
            Set<String> barcodeSet = new LinkedHashSet<>();
            for (String rawBarcode : rawBarcodes.split("\\s*,\\s*")) {
                String barcode = rawBarcode.trim();
                if (barcode.isEmpty())
                    continue;
                if (!barcodeSet.add(barcode)) {
                    redirectAttributes.addFlashAttribute("errorMessage", message("backend.barcode.duplicate", barcode));
                    return "redirect:/librarian/borrow/create";
                }
            }
            java.util.List<com.lms.entity.BookItem> validItems = new java.util.ArrayList<>();

            for (String barcode : barcodeSet) {
                java.util.Optional<com.lms.entity.BookItem> optItem = bookItemRepository.findByBarcode(barcode);
                if (optItem.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", message("backend.barcode.notFound", barcode));
                    return "redirect:/librarian/borrow/create";
                }
                com.lms.entity.BookItem item = optItem.get();
                if (!"Available".equalsIgnoreCase(item.getStatus())) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            message("backend.book.unavailable", barcode, item.getStatus()));
                    return "redirect:/librarian/borrow/create";
                }
                validItems.add(item);
            }

            if (validItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.barcode.noneValid"));
                return "redirect:/librarian/borrow/create";
            }

            long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
            int configuredLimit = getPositiveSetting("Max_Books_Per_Member",
                    getPositiveSetting("MAX_BOOKS_PER_MEMBER", 10));
            int maxLimit = member.getTier() != null && member.getTier().getBorrowLimit() != null
                    ? member.getTier().getBorrowLimit()
                    : configuredLimit;
            if (currentBorrowCount + validItems.size() > Math.max(1, maxLimit)) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.tierLimitExceeded"));
                return "redirect:/librarian/borrow/create";
            }

            // Tính phí mượn sách
            int days = (numberOfDays != null ? numberOfDays : 14);
            List<BigDecimal> itemDailyFees = validItems.stream()
                    .map(this::borrowFeeForCondition)
                    .toList();
            BigDecimal dailyFeeTotal = itemDailyFees.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalBaseFee = dailyFeeTotal.multiply(BigDecimal.valueOf(days));

            BigDecimal discount = BigDecimal.ZERO;
            if (member.getTier() != null && member.getTier().getDiscountPercent() != null) {
                discount = member.getTier().getDiscountPercent();
            }

            BigDecimal finalFee = totalBaseFee.multiply(new BigDecimal("100").subtract(discount))
                    .divide(new BigDecimal("100"), java.math.RoundingMode.HALF_UP);

            // 3. Truyền dữ liệu sang trang Review
            model.addAttribute("billMember", member);
            model.addAttribute("billBookItems", validItems);
            model.addAttribute("billDays", days);
            model.addAttribute("billRawBarcodes", rawBarcodes);
            model.addAttribute("billMemberIdentifier", memberIdentifier);
            model.addAttribute("billPaymentMethod", paymentMethod);
            model.addAttribute("billItemDailyFees", itemDailyFees);
            model.addAttribute("billDailyFeeTotal", dailyFeeTotal);
            model.addAttribute("billBaseFee", totalBaseFee);
            model.addAttribute("billDiscount", discount);
            model.addAttribute("billFinalFee", finalFee);

            return "librarian/review-borrow";

        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.validation.failed", e));
            return "redirect:/librarian/borrow/create";
        }
    }

    private BigDecimal borrowFeeForCondition(com.lms.entity.BookItem item) {
        String condition = item == null || item.getBookCondition() == null
                ? ""
                : item.getBookCondition().trim().toLowerCase(java.util.Locale.ROOT);
        if (condition.contains("severely")) {
            return moneySetting("SEVERE_DAMAGE_BORROW_FEE", 3000);
        }
        if (condition.contains("minor")) {
            return moneySetting("MINOR_DAMAGE_BORROW_FEE", 4000);
        }
        return moneySetting("BORROW_FEE_PER_BOOK", 5000);
    }

    private BigDecimal moneySetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .filter(value -> value.signum() >= 0)
                    .orElse(BigDecimal.valueOf(defaultValue));
        } catch (NumberFormatException ignored) {
            return BigDecimal.valueOf(defaultValue);
        }
    }

    // Xử lý tạo phiếu mượn trực tiếp bằng mã vạch (Barcode) quét tại quầy sau khi
    // xác nhận Bill
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
                    PayOsPayment payment = payOsPaymentService.createBorrowFeeForLibrarian(borrow.getMember(),
                            borrow.getBorrowId());
                    return "redirect:/librarian/payments/payos/" + payment.getOrderCode();
                }
                borrowService.activatePendingBankBorrow(borrow.getBorrowId());
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    message("backend.loan.createdAtDeskWithCode", BorrowCodeFormatter.format(borrow.getBorrowId())));
        } catch (ApplicationException e) {
            if (borrow != null && "BANK".equalsIgnoreCase(request.getPaymentMethod())) {
                try {
                    borrowService.cancelPendingBankBorrow(borrow.getBorrowId(), "FAILED");
                } catch (ApplicationException ignored) {
                    // Preserve the original payment error shown to the librarian.
                }
            }
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.action.failed", e));
            redirectAttributes.addFlashAttribute("billMemberIdentifier", request.getMemberIdentifier());
            redirectAttributes.addFlashAttribute("billDays", request.getNumberOfDays());
            redirectAttributes.addFlashAttribute("billRawBarcodes", rawBarcodes);
        }
        return "redirect:/librarian/borrow/create";
    }

    // ==========================================
    // ĐÃ XÓA PHƯƠNG THỨC memberRequestReturn BỊ TRÙNG MAPPING TẠI ĐÂY
    // ==========================================

    // ĐỒNG BỘ ENDPOINT THEO FORM HTML PHẦN 2: Phê duyệt đặt giữ chỗ trước từ Thủ
    // thư
    @PostMapping("/librarian/reservations/approve/{reservationId}")
    public String approveReservationRequest(@PathVariable("reservationId") Integer reservationId, Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.approveReservationRequest(reservationId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.reservation.approved"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.action.approveFailed", e));
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
            if (cleanBc.isEmpty())
                continue;

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("barcode", cleanBc);

            java.util.Optional<com.lms.entity.BookItem> itemOpt = bookItemRepository.findByBarcode(cleanBc);
            if (itemOpt.isPresent()) {
                com.lms.entity.BookItem item = itemOpt.get();
                data.put("found", true);
                data.put("bookId", item.getBook().getBookId());
                data.put("title", item.getBook().getTitle());
                data.put("coverImageUrl", item.getBook().getCoverImageUrl());
                String rawStatus = item.getStatus();

                long availableCount = bookItemRepository
                        .countByBook_BookIdAndStatusIgnoreCase(item.getBook().getBookId(), "Available");
                data.put("availableCount", availableCount);

                // Debug logging to console
                try {
                    java.util.List<com.lms.entity.BookItem> allItems = bookItemRepository
                            .findByBook_BookId(item.getBook().getBookId());
                    System.out.println("DEBUG BOOK LOOKUP: Book ID = " + item.getBook().getBookId() + ", Title = "
                            + item.getBook().getTitle() + ", Available Count in DB = " + availableCount);
                    for (com.lms.entity.BookItem bi : allItems) {
                        System.out.println(
                                "DEBUG BOOK ITEM: Barcode = " + bi.getBarcode() + ", Status = " + bi.getStatus());
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG BOOK LOOKUP ERROR: " + e.getMessage());
                }

                String vnStatus = message("book.status.available");
                if ("Borrowed".equalsIgnoreCase(rawStatus))
                    vnStatus = message("loan.status.borrowed");
                else if ("Lost".equalsIgnoreCase(rawStatus))
                    vnStatus = message("loan.status.lost");
                else if ("Maintenance".equalsIgnoreCase(rawStatus))
                    vnStatus = message("backend.book.status.maintenance");

                data.put("status", vnStatus);
                data.put("rawStatus", rawStatus);
            } else {
                data.put("found", false);
                data.put("error", message("backend.common.notFound"));
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
            List<String> eligibilityReasons = getBorrowIneligibilityReasons(member);
            boolean eligible = member.getUser().getStatus() == com.lms.enums.UserStatus.Active
                    && eligibilityReasons.isEmpty();
            response.put("eligible", eligible);
            response.put("reasons", eligibilityReasons);

            int maxBorrowLimit = getPositiveSetting("Max_Books_Per_Member",
                    getPositiveSetting("MAX_BOOKS_PER_MEMBER", 10));
            if (member.getTier() != null) {
                response.put("memberLevel", member.getTier().getTierName());
                maxBorrowLimit = member.getTier().getBorrowLimit() != null ? member.getTier().getBorrowLimit()
                        : maxBorrowLimit;
            } else {
                response.put("memberLevel", message("backend.member.defaultTier"));
            }
            response.put("maxBorrowLimit", maxBorrowLimit);

            long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
            response.put("currentBorrowCount", currentBorrowCount);
        } else {
            response.put("found", false);
        }

        return response;
    }

    private List<String> getBorrowIneligibilityReasons(com.lms.entity.Member member) {
        List<String> reasons = new java.util.ArrayList<>();
        if (borrowDetailRepository.countByBorrow_Member_MemberIdAndStatusIgnoreCase(member.getMemberId(),
                "Overdue") > 0) {
            reasons.add(message("backend.borrow.blockedByOverdue"));
        }
        if (!transactionRepository.findUnpaidFineTransactions(member.getMemberId(), List.of("FINE", "DAMAGE_FEE"))
                .isEmpty()) {
            reasons.add(message("backend.borrow.blockedByUnpaidFine"));
        }
        return reasons;
    }

    private int getPositiveSetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .filter(value -> value > 0)
                    .orElse(defaultValue);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    @GetMapping("/librarian/borrow/member/{memberId}")
    public String viewMemberBorrowHistory(@PathVariable Integer memberId,
            @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "borrowPage", defaultValue = "0") int borrowPageNumber,
            Model model) {
        com.lms.entity.Member member = memberRepository.findById(memberId)
                .orElseThrow(
                        () -> new com.lms.exception.ForbiddenException(message("backend.member.notFoundOrForbidden")));

        // Determine user account creation date
        java.time.LocalDateTime minBorrow = borrowRepository.findMinBorrowDateByMemberId(memberId);
        java.time.LocalDateTime minTx = transactionRepository.findMinTransactionDateByMemberId(memberId);
        java.time.LocalDateTime creationDateTime = java.time.LocalDateTime.now();
        if (minBorrow != null && minTx != null) {
            creationDateTime = minBorrow.isBefore(minTx) ? minBorrow : minTx;
        } else if (minBorrow != null) {
            creationDateTime = minBorrow;
        } else if (minTx != null) {
            creationDateTime = minTx;
        }
        LocalDate creationDate = creationDateTime.toLocalDate();
        LocalDate today = LocalDate.now();

        boolean invalidDateRange = (startDate != null && endDate != null && startDate.isAfter(endDate))
                || (startDate != null && (startDate.isBefore(creationDate) || startDate.isAfter(today)))
                || (endDate != null && (endDate.isBefore(creationDate) || endDate.isAfter(today)));

        // Enforce safe date bounds after retaining an explicit validation message.
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
            filterStart = creationDate;
            filterEnd = today;
        }

        // Load one page and batch-fetch its details to avoid N+1 queries.
        List<Borrow> allBorrows = borrowRepository.findByMember_MemberIdOrderByBorrowDateDesc(memberId);
        java.time.LocalDateTime rangeStart = filterStart.atStartOfDay();
        java.time.LocalDateTime rangeEndExclusive = filterEnd.plusDays(1).atStartOfDay();
        String historyKeyword = keyword == null ? "" : keyword.trim();
        if (historyKeyword.regionMatches(true, 0, "BOR-", 0, 4)) {
            historyKeyword = historyKeyword.substring(4).trim();
        }
        Set<String> allowedHistoryStatuses = Set.of("PENDING", "PAYMENT_PENDING", "PAYMENT_CANCELLED", "PAYMENT_EXPIRED",
                "WAITING_PICKUP", "APPROVED", "BORROWED", "ACTIVE", "OVERDUE", "RETURN_PENDING",
                "RENEW_PENDING", "RETURNED", "REJECTED", "CANCELED", "CANCELLED");
        String historyStatus = status == null ? "" : normalizeStatus(status);
        if (!historyStatus.isBlank() && !allowedHistoryStatuses.contains(historyStatus)) {
            historyStatus = "";
        }

        Page<Borrow> borrowPage = borrowRepository.searchMemberBorrowHistory(
                memberId, rangeStart, rangeEndExclusive, historyKeyword, historyStatus,
                PageRequest.of(Math.max(0, borrowPageNumber), 10));
        List<Borrow> filteredBorrows = borrowPage.getContent();

        List<Integer> borrowIds = filteredBorrows.stream().map(Borrow::getBorrowId).toList();
        java.util.Map<Integer, List<BorrowDetail>> detailsByBorrowId = borrowIds.isEmpty()
                ? java.util.Map.of()
                : borrowDetailRepository.findByBorrow_BorrowIdIn(borrowIds).stream()
                        .collect(Collectors.groupingBy(detail -> detail.getBorrow().getBorrowId()));

        java.util.Map<Integer, List<Transaction>> transactionsByBorrowId = borrowIds.isEmpty()
                ? java.util.Map.of()
                : transactionRepository.findByBorrow_BorrowIdInOrderByTransactionDateDesc(borrowIds).stream()
                        .collect(Collectors.groupingBy(transaction -> transaction.getBorrow().getBorrowId()));

        // Stats are receipt counts, matching the labels shown to librarians.
        int totalBorrows = allBorrows.size();
        int completedBorrows = (int) allBorrows.stream()
                .filter(borrow -> "RETURNED".equals(normalizeStatus(borrow.getStatus())))
                .count();
        Set<String> activeStatuses = Set.of("ACTIVE", "BORROWED", "OVERDUE", "RETURN_PENDING", "RENEW_PENDING");
        int activeBorrows = (int) allBorrows.stream()
                .filter(borrow -> activeStatuses.contains(normalizeStatus(borrow.getStatus())))
                .count();
        int pendingBorrows = (int) allBorrows.stream()
                .filter(borrow -> Set.of("PENDING", "PAYMENT_PENDING", "WAITING_PICKUP", "APPROVED")
                        .contains(normalizeStatus(borrow.getStatus())))
                .count();

        List<Integer> allBorrowIds = allBorrows.stream().map(Borrow::getBorrowId).toList();
        List<BorrowDetail> allDetails = allBorrowIds.isEmpty()
                ? List.of()
                : borrowDetailRepository.findByBorrow_BorrowIdIn(allBorrowIds);
        java.util.Map<Integer, List<BorrowDetail>> allDetailsByBorrowId = allDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getBorrow().getBorrowId()));
        completedBorrows = (int) allBorrows.stream()
                .filter(borrow -> {
                    List<BorrowDetail> details = allDetailsByBorrowId.getOrDefault(borrow.getBorrowId(), List.of());
                    return !details.isEmpty() && details.stream()
                            .allMatch(detail -> "RETURNED".equals(normalizeStatus(detail.getStatus())));
                })
                .count();
        long totalBooks = allDetails.size();
        long overdueBooks = allDetails.stream()
                .filter(detail -> "OVERDUE".equals(normalizeStatus(detail.getStatus()))
                        || (detail.getReturnDate() == null && detail.getDueDate() != null
                            && detail.getDueDate().isBefore(java.time.LocalDateTime.now())
                            && Set.of("BORROWED", "RETURN_PENDING", "RENEW_PENDING")
                                .contains(normalizeStatus(detail.getStatus()))))
                .count();
        java.time.LocalDateTime nearestDueDate = allDetails.stream()
                .filter(detail -> detail.getReturnDate() == null && detail.getDueDate() != null
                        && !detail.getDueDate().isBefore(java.time.LocalDateTime.now())
                        && Set.of("BORROWED", "RETURN_PENDING", "RENEW_PENDING")
                            .contains(normalizeStatus(detail.getStatus())))
                .map(BorrowDetail::getDueDate)
                .min(java.time.LocalDateTime::compareTo)
                .orElse(null);
        List<Transaction> unpaidFines = transactionRepository.findUnpaidFineTransactions(
                memberId, List.of("FINE", "DAMAGE_FEE"));
        BigDecimal unpaidFineAmount = unpaidFines.stream()
                .map(Transaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        com.lms.entity.MemberAccount account = memberAccountRepository.findByMemberMemberId(memberId).orElse(null);
        com.lms.entity.Wallet wallet = walletRepository.findByMemberMemberId(memberId).orElse(null);

        model.addAttribute("member", member);
        model.addAttribute("accountUsername",
                account != null ? account.getUsername() : message("librarian.memberDetail.noAccount"));
        String accountStatus = account != null ? account.getStatus() : "Inactive";
        if (member.getUser() == null || member.getUser().getStatus() == null
                || member.getUser().getStatus() != com.lms.enums.UserStatus.Active) {
            accountStatus = member.getUser() != null && member.getUser().getStatus() != null
                    ? member.getUser().getStatus().name()
                    : "Inactive";
        }
        model.addAttribute("accountStatus", accountStatus);
        model.addAttribute("walletBalance",
                wallet != null && wallet.getBalance() != null ? wallet.getBalance() : java.math.BigDecimal.ZERO);

        model.addAttribute("totalBorrows", totalBorrows);
        model.addAttribute("completedBorrows", completedBorrows);
        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("pendingBorrows", pendingBorrows);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("overdueBooks", overdueBooks);
        model.addAttribute("nearestDueDate", nearestDueDate);
        model.addAttribute("unpaidFineCount", unpaidFines.size());
        model.addAttribute("unpaidFineAmount", unpaidFineAmount);

        model.addAttribute("startDate", filterStart.toString());
        model.addAttribute("endDate", filterEnd.toString());
        model.addAttribute("minDate", creationDate.toString());
        model.addAttribute("maxDate", today.toString());
        model.addAttribute("historyKeyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("historyStatus", historyStatus);

        model.addAttribute("borrows", filteredBorrows);
        model.addAttribute("borrowPage", borrowPage);
        model.addAttribute("historyResultCount", borrowPage.getTotalElements());
        model.addAttribute("historyFiltersActive", !historyKeyword.isBlank() || !historyStatus.isBlank()
                || startDate != null || endDate != null);
        model.addAttribute("detailsByBorrowId", detailsByBorrowId);
        model.addAttribute("transactionsByBorrowId", transactionsByBorrowId);
        model.addAttribute("dateFilterError",
                invalidDateRange ? message("librarian.borrowMember.invalidDateRange") : null);
        model.addAttribute("activeMenu", "borrow-schedule");

        return "librarian/borrow-member-detail";
    }

    private String normalizeStatus(String status) {
        return status == null ? "UNKNOWN" : status.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_');
    }
}
