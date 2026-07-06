package com.lms.controller.member;

import com.lms.dto.response.BorrowFeeViewData;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.Transaction;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.FinancialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/member/financial")
public class FinancialController {
    private static final int BORROW_FEE_DAYS = 10;
    private static final String BORROW_FEE_TYPE = "BORROW_FEE";
    private static final String FINE_TYPE = "FINE";
    private static final String DAMAGE_FEE_TYPE = "DAMAGE_FEE";
    private static final List<String> TOP_UP_NOTIFICATION_KEYWORDS = List.of("nap tien", "nạp tiền");

    private final TransactionRepository transactionRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final FinancialService financialService;

    public FinancialController(TransactionRepository transactionRepository,
                               MemberNotificationRepository memberNotificationRepository,
                               MemberRepository memberRepository,
                               WalletRepository walletRepository,
                               BorrowRepository borrowRepository,
                               BorrowDetailRepository borrowDetailRepository,
                               FinancialService financialService) {
        this.transactionRepository = transactionRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.financialService = financialService;
    }

    @GetMapping("/fines")
    public String viewOverdueFines(Principal principal, Model model) {
        return viewTransactionHistory(principal, 0, FINE_TYPE, model);
    }

    @PostMapping("/fines/pay/{fineId}")
    public String payOverdueFine(@PathVariable Integer fineId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        Member member = getCurrentMember(principal);

        try {
            financialService.payOverdueFine(member.getMemberId(), fineId);
            redirectAttributes.addFlashAttribute("success", "Đã thanh toán phí phạt thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/member/financial/transactions";
    }

    @GetMapping("/fees")
    public String viewBorrowingFees(Principal principal, Model model) {
        Member member = getCurrentMember(principal);
        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);

        List<BorrowFeeViewData> pendingFees = new ArrayList<>();
        List<BorrowFeeViewData> payableFees = new ArrayList<>();
        List<BorrowFeeViewData> paidFees = new ArrayList<>();

        List<Borrow> borrows = borrowRepository.findByMember_MemberIdOrderByBorrowDateDesc(member.getMemberId());
        for (Borrow borrow : borrows) {
            BorrowFeeViewData feeViewData = buildBorrowFeeViewData(member.getMemberId(), borrow);
            if (feeViewData == null) {
                continue;
            }

            if ("Paid".equalsIgnoreCase(feeViewData.getPaymentStatus())) {
                paidFees.add(feeViewData);
            } else if ("Pending".equalsIgnoreCase(feeViewData.getBorrowStatus())) {
                pendingFees.add(feeViewData);
            } else if (feeViewData.isPayable()) {
                payableFees.add(feeViewData);
            }
        }

        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("pendingFees", pendingFees);
        model.addAttribute("payableFees", payableFees);
        model.addAttribute("paidFees", paidFees);
        model.addAttribute("fees", payableFees);

        return "member/fees";
    }

    @PostMapping("/fees/pay/{borrowId}")
    public String payBorrowingFee(@PathVariable Integer borrowId,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        Member member = getCurrentMember(principal);

        try {
            financialService.payBorrowingFee(member.getMemberId(), borrowId);
            redirectAttributes.addFlashAttribute("message", "Đã thanh toán phí mượn thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/member/financial/fees";
    }

    @PostMapping("/deposit/{reservationId}")
    public String payReservationDeposit(@PathVariable Integer reservationId,
                                        Principal principal,
                                        RedirectAttributes redirectAttributes) {
        Member member = getCurrentMember(principal);

        try {
            financialService.payReservationDeposit(member.getMemberId(), reservationId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thanh toán tiền cọc đặt trước thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/member/borrow/reservations";
    }

    @GetMapping("/transactions")
    public String viewTransactionHistory(Principal principal,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(required = false) String type,
                                         Model model) {
        Member member = getCurrentMember(principal);
        Page<Transaction> transactionPage = financialService.getTransactionHistory(member.getMemberId(), page, type);

        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);
        List<Transaction> unpaidFines = transactionRepository.findUnpaidFineTransactions(
                member.getMemberId(), List.of(FINE_TYPE, DAMAGE_FEE_TYPE));
        BigDecimal totalUnpaidFines = unpaidFines.stream()
                .map(transaction -> transaction.getAmount() == null
                        ? BigDecimal.ZERO
                        : transaction.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("unpaidFines", unpaidFines);
        model.addAttribute("totalUnpaidFines", totalUnpaidFines);
        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedType", type);

        return "member/wallet";
    }

    @GetMapping("/topup-notifications")
    public String viewTopupNotifications(Principal principal,
                                         @RequestParam(defaultValue = "0") int page,
                                         Model model) {
        Member member = getCurrentMember(principal);
        Page<MemberNotification> notificationPage = memberNotificationRepository.findTopupNotifications(
                member.getMemberId(),
                "nạp tiền",
                PageRequest.of(Math.max(page, 0), 10));

        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("notifications", notificationPage.getContent());
        model.addAttribute("currentPage", page);
        return "member/topup-notifications";
    }

    @PostMapping("/topup-notifications/{notificationId}/mark-read")
    public String markTopupNotificationAsRead(@PathVariable Integer notificationId,
                                              @RequestParam(defaultValue = "0") int page,
                                              Principal principal,
                                              RedirectAttributes redirectAttributes) {
        Member member = getCurrentMember(principal);
        MemberNotificationId id = new MemberNotificationId(member.getMemberId(), notificationId);
        memberNotificationRepository.findById(id).ifPresent(memberNotification -> {
            memberNotification.setIsRead(true);
            memberNotification.setReadDate(LocalDateTime.now());
            memberNotificationRepository.save(memberNotification);
        });

        redirectAttributes.addFlashAttribute("success", "Đã đánh dấu thông báo là đã đọc.");
        return "redirect:/member/financial/topup-notifications?page=" + Math.max(page, 0);
    }

    @PostMapping("/topup-notifications/mark-all-read")
    public String markAllTopupNotificationsAsRead(@RequestParam(defaultValue = "0") int page,
                                                  Principal principal,
                                                  RedirectAttributes redirectAttributes) {
        Member member = getCurrentMember(principal);
        Page<MemberNotification> notificationPage = memberNotificationRepository.findTopupNotifications(
                member.getMemberId(),
                "nạp tiền",
                PageRequest.of(0, 1000));

        for (MemberNotification memberNotification : notificationPage.getContent()) {
            if (!Boolean.TRUE.equals(memberNotification.getIsRead())) {
                memberNotification.setIsRead(true);
                memberNotification.setReadDate(LocalDateTime.now());
                memberNotificationRepository.save(memberNotification);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Đã đánh dấu tất cả thông báo nạp tiền là đã đọc.");
        return "redirect:/member/financial/topup-notifications?page=" + Math.max(page, 0);
    }

    private BorrowFeeViewData buildBorrowFeeViewData(Integer memberId, Borrow borrow) {
        if (borrow == null || borrow.getBorrowId() == null) {
            return null;
        }

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrow.getBorrowId());
        if (details == null || details.isEmpty()) {
            return null;
        }

        BorrowFeeViewData feeViewData = new BorrowFeeViewData();
        feeViewData.setBorrowId(borrow.getBorrowId());
        feeViewData.setQuantity(details.size());
        feeViewData.setDays(calculateDisplayBorrowDays(details));
        feeViewData.setBorrowStatus(borrow.getStatus());
        feeViewData.setBorrowDate(borrow.getBorrowDate());

        try {
            feeViewData.setAmount(financialService.calculateBorrowingFeeAmount(borrow.getBorrowId()));
        } catch (Exception ignored) {
            feeViewData.setAmount(BigDecimal.ZERO);
        }

        transactionRepository.findLatestCompletedBorrowFee(memberId, borrow.getBorrowId())
                .ifPresentOrElse(transaction -> markBorrowFeeAsPaid(feeViewData, transaction),
                        () -> markBorrowFeeAsUnpaid(feeViewData, borrow.getStatus()));

        return feeViewData;
    }

    private void markBorrowFeeAsPaid(BorrowFeeViewData feeViewData, Transaction transaction) {
        feeViewData.setPaymentStatus("Paid");
        feeViewData.setPaymentDate(transaction.getTransactionDate());
        feeViewData.setTransactionId(transaction.getTransactionId());
        feeViewData.setPayable(false);
    }

    private void markBorrowFeeAsUnpaid(BorrowFeeViewData feeViewData, String borrowStatus) {
        if ("Pending".equalsIgnoreCase(borrowStatus)) {
            feeViewData.setPaymentStatus("Waiting for librarian approval");
            feeViewData.setPayable(false);
        } else if (isApprovedBorrowStatus(borrowStatus)) {
            feeViewData.setPaymentStatus("Unpaid");
            feeViewData.setPayable(true);
        } else {
            feeViewData.setPaymentStatus("Not required");
            feeViewData.setPayable(false);
        }
    }

    private boolean isApprovedBorrowStatus(String borrowStatus) {
        return "Active".equalsIgnoreCase(borrowStatus)
                || "Borrowing".equalsIgnoreCase(borrowStatus)
                || "Overdue".equalsIgnoreCase(borrowStatus);
    }

    private int calculateDisplayBorrowDays(List<BorrowDetail> details) {
        int maxDays = 1;
        for (BorrowDetail detail : details) {
            LocalDateTime start = detail.getBorrow() == null ? null : detail.getBorrow().getBorrowDate();
            LocalDateTime end = detail.getDueDate();
            if (start == null || end == null || !end.isAfter(start)) {
                continue;
            }

            long hours = Duration.between(start, end).toHours();
            int days = (int) Math.max((hours + 23) / 24, 1);
            maxDays = Math.max(maxDays, days);
        }
        return maxDays;
    }

    private Member getCurrentMember(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Bạn cần đăng nhập để xem thông tin tài chính");
        }

        String usernameOrEmail = principal.getName();
        return memberRepository.findByUserEmail(usernameOrEmail)
                .or(() -> memberRepository.findByUserPhone(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thành viên hiện tại"));
    }
}
