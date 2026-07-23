package com.lms.controller.librarian;
import com.lms.entity.*;
import com.lms.exception.ApplicationException;
import com.lms.exception.ValidationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.service.LoanService;
import com.lms.service.FinancialService;
import com.lms.service.PayOsPaymentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.security.Principal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Controller
@RequestMapping("/librarian/loan")
public class LoanController extends LocalizedControllerSupport {

    private static final BigDecimal MAX_DAMAGE_FINE = new BigDecimal("500000000");

    private final LoanService loanService;
    private final FinancialService financialService;
    private final com.lms.repository.MemberRepository memberRepository;
    private final com.lms.repository.BorrowRepository borrowRepository;
    private final com.lms.repository.BorrowDetailRepository borrowDetailRepository;
    private final com.lms.repository.TransactionRepository transactionRepository;
    private final com.lms.repository.WalletRepository walletRepository;
    private final com.lms.repository.MemberAccountRepository memberAccountRepository;
    private final PayOsPaymentService paymentService;

    public LoanController(LoanService loanService,
                          FinancialService financialService,
                          com.lms.repository.MemberRepository memberRepository,
                          com.lms.repository.BorrowRepository borrowRepository,
                          com.lms.repository.BorrowDetailRepository borrowDetailRepository,
                          com.lms.repository.TransactionRepository transactionRepository,
                          com.lms.repository.WalletRepository walletRepository,
                          com.lms.repository.MemberAccountRepository memberAccountRepository,
                          PayOsPaymentService paymentService) {
        this.loanService = loanService;
        this.financialService = financialService;
        this.memberRepository = memberRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.paymentService = paymentService;
    }

    @GetMapping("/{id}")
    public String showLoanDetail(@PathVariable("id") Integer borrowId, Model model,
                                 HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        Borrow borrow = loanService.getLoanDetails(borrowId);
        model.addAttribute("borrow", borrow);
        model.addAttribute("details", borrowDetailRepository.findByBorrowId(borrowId));
        model.addAttribute("transactions", transactionRepository.findByBorrow_BorrowId(borrowId));
        model.addAttribute("activeMenu", "borrow-list");
        return "librarian/loan-detail";
    }

    /**
     * GET: /librarian/loan/returns
     * Màn hình mặc định của Bàn Trả Sách - Tự động nạp danh sách sách đã được trả thành công hôm nay.
     */
    @GetMapping("/returns")
    public String showReturnDesk(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        model.addAttribute("todayReturned", loanService.getTodayReturnedBooks());
        model.addAttribute("defaultReturnDate", LocalDate.now());
        model.addAttribute("damageCompensationAmount", financialService.getDamageCompensationAmount());
        return "librarian/return-desk";
    }

    /**
     * GET: /librarian/loan/returns/search
     * Xử lý tìm kiếm lượt mượn hoạt động (Borrowed/Overdue/Return_Pending) CHỈ theo mã Barcode vừa quét.
     */
    @GetMapping("/returns/search")
    public String searchActiveReturnLoans(@RequestParam("barcode") String barcode, Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        String trimmedQuery = (barcode != null) ? barcode.trim() : "";

        // CHỈNH SỬA TẠI ĐÂY: Thay thế searchActiveLoansByQuery bằng findActiveLoansByBarcode để chặn tìm kiếm bằng email, sđt, mã đơn mượn
        List<BorrowDetail> searchResults = loanService.findActiveLoansByBarcode(trimmedQuery);

        model.addAttribute("searchResults", searchResults);
        model.addAttribute("searchedBarcode", trimmedQuery);
        model.addAttribute("searchedQuery", trimmedQuery);
        model.addAttribute("todayReturned", loanService.getTodayReturnedBooks());
        model.addAttribute("defaultReturnDate", LocalDate.now());
        model.addAttribute("damageCompensationAmount", financialService.getDamageCompensationAmount());

        if (searchResults.isEmpty()) {
            model.addAttribute("errorMessage", message("backend.return.activeQueryNotFound", trimmedQuery));
        } else {
            model.addAttribute("successMessage", message("backend.return.activeFound"));
        }
        return "librarian/return-desk";
    }

    /**
     * GET: /librarian/loan/returns/api/search
     * API Ajax tìm kiếm lượt mượn hoạt động CHỈ theo mã Barcode.
     */
    @GetMapping("/returns/api/search")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> apiSearchActiveReturnLoans(@RequestParam("barcode") String barcode) {
        String trimmedQuery = (barcode != null) ? barcode.trim() : "";
        if (trimmedQuery.isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", "Barcode cannot be empty"));
        }
        List<BorrowDetail> searchResults = loanService.findActiveLoansByBarcode(trimmedQuery);
        if (searchResults.isEmpty()) {
            return org.springframework.http.ResponseEntity.status(404).body(java.util.Map.of("message", message("librarian.returnDesk.noActiveLoan")));
        }

        List<java.util.Map<String, Object>> dto = searchResults.stream().map(detail -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("borrowId", detail.getBorrow().getBorrowId());
            map.put("borrowDetailId", detail.getBorrowDetailId());
            map.put("needsBarcode", detail.getBookItem() == null || detail.getBookItem().getBarcode() == null || detail.getBookItem().getBarcode().isBlank());
            map.put("barcode", detail.getBookItem() != null ? detail.getBookItem().getBarcode() : "");
            map.put("currentBookCondition", detail.getBookItem() != null
                    && detail.getBookItem().getBookCondition() != null
                    ? detail.getBookItem().getBookCondition()
                    : "New");
            map.put("bookTitle", detail.getBook().getTitle());
            map.put("memberName", detail.getBorrow().getMember() != null && detail.getBorrow().getMember().getUser() != null ? detail.getBorrow().getMember().getUser().getFullName() : "");
            map.put("memberEmail", detail.getBorrow().getMember() != null && detail.getBorrow().getMember().getUser() != null ? detail.getBorrow().getMember().getUser().getEmail() : "");
            map.put("borrowDate", detail.getBorrow().getBorrowDate() != null ? detail.getBorrow().getBorrowDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
            map.put("dueDate", detail.getDueDate() != null ? detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
            map.put("isOverdue", detail.getDueDate() != null && detail.getDueDate().isBefore(java.time.LocalDateTime.now()));
            map.put("renewCount", detail.getRenewCount());
            return map;
        }).toList();

        return org.springframework.http.ResponseEntity.ok(dto);
    }


    @PostMapping("/returns/api/recover-barcode")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> recoverMissingBarcode(
            @RequestParam("borrowDetailId") Integer borrowDetailId,
            @RequestParam("barcode") String barcode) {
        try {
            BorrowDetail recovered = loanService.recoverMissingBookItem(borrowDetailId, barcode);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                    "message", message("librarian.returnDesk.recoverySuccess", recovered.getBookItem().getBarcode()),
                    "barcode", recovered.getBookItem().getBarcode(),
                    "currentBookCondition", recovered.getBookItem().getBookCondition() != null
                            ? recovered.getBookItem().getBookCondition()
                            : "New"));
        } catch (ApplicationException exception) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", exception.getMessage()));
        }
    }
    /**
     * POST: /librarian/loan/returns/confirm
     * Tiếp nhận dữ liệu từ Modal gửi lên: cập nhật ngày trả thực tế, tình trạng vật lý, ghi chú, tính phạt quá hạn.
     */
    @PostMapping("/returns/confirm")
    public String confirmReturnBookWithDetails(@RequestParam("barcodes") List<String> barcodes,
                                               @RequestParam("returnDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
                                               @RequestParam("conditionNote") String conditionNote,
                                               @RequestParam(value = "conditionNoteAdditional", required = false) String conditionNoteAdditional,
                                               @RequestParam(value = "damageFine", required = false) BigDecimal damageFine,
                                               @RequestParam(value = "paymentMethod", required = false, defaultValue = "cash") String paymentMethod,
                                               Principal principal,
                                               RedirectAttributes redirectAttributes) {
        try {
            if (barcodes == null || barcodes.isEmpty()) {
                throw new ValidationException(message("backend.return.invalidBarcodes"));
            }
            if (returnDate == null || !returnDate.equals(LocalDate.now())) {
                throw new ValidationException(message("backend.return.invalidReturnDate"));
            }
            if (barcodes.size() > 1 && !isGoodCondition(conditionNote)) {
                throw new ValidationException(message("backend.return.minorDamageSingleOnly"));
            }
            if (!isGoodCondition(conditionNote)
                    && (conditionNoteAdditional == null || conditionNoteAdditional.trim().isEmpty())) {
                throw new ValidationException(message("backend.return.damageDescriptionRequired"));
            }
            if (damageFine != null && (damageFine.signum() < 0
                    || damageFine.stripTrailingZeros().scale() > 0
                    || damageFine.compareTo(MAX_DAMAGE_FINE) > 0)) {
                throw new ValidationException(message("librarian.returnDesk.fineInvalid"));
            }

            // Minor damage uses the manually entered repair fine. Severe damage and
            // lost books are charged by issueDamageCompensation() using the amount
            // configured in system settings, so requiring damageFine for them would
            // either block the form or charge the member twice.
            if (isMinorDamage(conditionNote)) {
                if (damageFine == null || damageFine.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException(message("librarian.returnDesk.fineRequired"));
                }
            }

            String staffUsername = (principal != null) ? principal.getName() : "admin";
            Set<Integer> borrowIds = new LinkedHashSet<>();
            for (String barcode : barcodes) {
                for (BorrowDetail detail : loanService.findActiveLoansByBarcode(barcode)) {
                    if (detail.getBorrow() != null && detail.getBorrow().getBorrowId() != null) {
                        borrowIds.add(detail.getBorrow().getBorrowId());
                    }
                }
            }

            // Chuyển LocalDate sang LocalDateTime (giữ nguyên giờ, phút, giây hiện hành của ngày làm việc)
            LocalDateTime actualReturnDateTime = LocalDateTime.now();

            // Thực thi nghiệp vụ lõi trong LoanService
            BigDecimal fine = isMinorDamage(conditionNote) && damageFine != null
                    ? damageFine
                    : BigDecimal.ZERO;
            Transaction transaction = loanService.confirmBatchReturnWithDetails(barcodes, actualReturnDateTime, conditionNote, conditionNoteAdditional, fine, paymentMethod, staffUsername);

            if (transaction != null && "bank".equalsIgnoreCase(paymentMethod)) {
                Member member = transaction.getWallet().getMember();
                PayOsPayment payment = paymentService.createFinePaymentForLibrarian(member, transaction.getTransactionId());
                return "redirect:/librarian/payments/payos/" + payment.getOrderCode();
            }

            if (borrowIds.size() == 1) {
                Integer borrowId = borrowIds.iterator().next();
                if (!transactionRepository.findPendingFineTransactionsByBorrowId(
                        borrowId, List.of("FINE", "DAMAGE_FEE")).isEmpty()) {
                    redirectAttributes.addFlashAttribute("successMessage",
                            message("backend.return.redirectedToPayment"));
                    return "redirect:/librarian/members/fines/payment/" + borrowId;
                }
            }

            if (requiresDamageCompensation(conditionNote)) {
                redirectAttributes.addFlashAttribute("successMessage",
                        message("backend.return.confirmedWithCompensation"));
            } else {
                redirectAttributes.addFlashAttribute("successMessage", message("backend.return.confirmed"));
            }
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.return.failed", e));
        }
        return "redirect:/librarian/loan/returns";
    }

    private boolean requiresDamageCompensation(String conditionNote) {
        String normalized = conditionNote == null ? "" : conditionNote.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("hư hỏng nặng")
                || normalized.contains("mất sách")
                || normalized.contains("severe damage")
                || normalized.contains("lost");
    }

    private boolean isMinorDamage(String conditionNote) {
        String normalized = conditionNote == null ? "" : conditionNote.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("hư hỏng nhẹ") || normalized.contains("minor damage");
    }

    private boolean isGoodCondition(String conditionNote) {
        String normalized = conditionNote == null ? "" : conditionNote.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("tốt") || normalized.startsWith("good");
    }

    /**
     * GET: /librarian/loan/borrow-schedule
     * Tích hợp điều hướng tab xem danh sách lịch mượn trả chi tiết.
     */
    @GetMapping("/borrow-schedule")
    public String showBorrowSchedule(@RequestParam(value = "keyword", required = false) String keyword,
                                     @RequestParam(value = "page", defaultValue = "0") int page) {
        if (keyword != null && !keyword.isBlank()) {
            return "redirect:/librarian/borrow/list?tab=members&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&page=" + page;
        }
        return "redirect:/librarian/borrow/list?tab=members&page=" + page;
    }

    /**
     * POST: /librarian/loan/renew/approve/{id}
     * Duyệt yêu cầu gia hạn từ độc giả.
     */
    @PostMapping("/renew/approve/{id}")
    public String approveRenewal(@PathVariable("id") Integer borrowDetailId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            loanService.approveRenewal(borrowDetailId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.renewal.approved"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.renewal.approveFailed", e));
        }
        return "redirect:/librarian/borrow/create";
    }

    /**
     * POST: /librarian/loan/renew/reject/{id}
     * Từ chối yêu cầu gia hạn từ độc giả.
     */
    @PostMapping("/renew/reject/{id}")
    public String rejectRenewal(@PathVariable("id") Integer borrowDetailId,
                                @RequestParam("reasonCode") String reasonCode,
                                @RequestParam("reason") String reason,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            loanService.rejectRenewal(borrowDetailId, staffUsername, reasonCode, reason);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.renewal.rejected"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.renewal.rejectFailed", e));
        }
        return "redirect:/librarian/borrow/create";
    }
}
