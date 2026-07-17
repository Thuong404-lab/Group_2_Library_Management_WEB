package com.lms.controller.member;
import com.lms.exception.ApplicationException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.UnauthorizedException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.dto.response.BorrowFeeViewData;
import com.lms.dto.response.MemberTransactionHistoryRow;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.PayOsPayment;
import com.lms.entity.Transaction;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.PayOsPaymentRepository;
import com.lms.repository.PayOsPaymentFineItemRepository;
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
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;

/**
 * Member financial UC flows maintained by Pham Kien Quoc:
 * UC-8.1, UC-8.2, UC-8.3, UC-8.4, UC-8.5.
 */
@Controller
@RequestMapping("/member/financial")
public class FinancialController extends LocalizedControllerSupport {
    private static final String FINE_TYPE = "FINE";
    private static final String DAMAGE_FEE_TYPE = "DAMAGE_FEE";
    private static final String TOP_UP_NOTIFICATION_KEYWORD_VI = "nạp tiền";
    private static final String TOP_UP_NOTIFICATION_KEYWORD_EN = "top-up";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MARK_ALL_READ_LIMIT = 1000;

    private final TransactionRepository transactionRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final FinancialService financialService;
    private final PayOsPaymentRepository payOsPaymentRepository;
    private final PayOsPaymentFineItemRepository payOsPaymentFineItemRepository;

    public FinancialController(TransactionRepository transactionRepository,
                               MemberNotificationRepository memberNotificationRepository,
                               MemberRepository memberRepository,
                               WalletRepository walletRepository,
                               BorrowRepository borrowRepository,
                               BorrowDetailRepository borrowDetailRepository,
                               FinancialService financialService,
                               PayOsPaymentRepository payOsPaymentRepository,
                               PayOsPaymentFineItemRepository payOsPaymentFineItemRepository) {
        this.transactionRepository = transactionRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.financialService = financialService;
        this.payOsPaymentRepository = payOsPaymentRepository;
        this.payOsPaymentFineItemRepository = payOsPaymentFineItemRepository;
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
            redirectAttributes.addFlashAttribute("success", message("backend.financial.finePaid"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/member/financial/transactions";
    }

    @GetMapping("/fees")
    public String viewBorrowingFees(Principal principal, Model model) {
        Member member = getCurrentMember(principal);
        List<BorrowFeeViewData> pendingFees = new ArrayList<>();
        List<BorrowFeeViewData> payableFees = new ArrayList<>();
        List<BorrowFeeViewData> paidFees = new ArrayList<>();
        loadBorrowingFeeGroups(member.getMemberId(), pendingFees, payableFees, paidFees);

        model.addAttribute("walletBalance", getWalletBalance(member.getMemberId()));
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
            redirectAttributes.addFlashAttribute("message", message("backend.financial.borrowFeePaid"));
        } catch (ApplicationException e) {
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
            redirectAttributes.addFlashAttribute("successMessage", message("backend.financial.depositPaid"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/member/borrow/reservations";
    }

    @PostMapping("/deposit/{reservationId}/refund-request")
    public String requestReservationDepositRefund(@PathVariable Integer reservationId,
                                                  Principal principal,
                                                  RedirectAttributes redirectAttributes) {
        Member member = getCurrentMember(principal);

        try {
            financialService.requestReservationDepositRefund(member.getMemberId(), reservationId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    message("backend.financial.refundRequested"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
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
        List<Transaction> unpaidFines = getUnpaidFines(member.getMemberId());
        List<PayOsPayment> kqpayPayments = page == 0
                ? getKqPayHistory(member.getMemberId(), type)
                : List.of();
        Set<Integer> aggregatedFineIds = getAggregatedFineIds(kqpayPayments);
        List<Transaction> visibleTransactions = transactionPage.getContent().stream()
                .filter(transaction -> !aggregatedFineIds.contains(transaction.getTransactionId()))
                .toList();
        List<MemberTransactionHistoryRow> historyRows = buildHistoryRows(visibleTransactions, kqpayPayments);

        model.addAttribute("walletBalance", getWalletBalance(member.getMemberId()));
        model.addAttribute("unpaidFines", unpaidFines);
        model.addAttribute("totalUnpaidFines", totalAbsAmount(unpaidFines));
        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("transactions", visibleTransactions);
        model.addAttribute("historyRows", historyRows);
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedType", type);
        model.addAttribute("kqpayPayments", kqpayPayments);

        return "member/wallet";
    }

    private List<MemberTransactionHistoryRow> buildHistoryRows(List<Transaction> transactions,
                                                                 List<PayOsPayment> payments) {
        List<MemberTransactionHistoryRow> rows = new ArrayList<>();
        transactions.stream().map(this::toHistoryRow).forEach(rows::add);
        payments.stream().map(this::toHistoryRow).forEach(rows::add);
        rows.sort(Comparator.comparing(
                MemberTransactionHistoryRow::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    private MemberTransactionHistoryRow toHistoryRow(Transaction transaction) {
        boolean completed = "Completed".equalsIgnoreCase(transaction.getStatus())
                || "Paid".equalsIgnoreCase(transaction.getStatus());
        return new MemberTransactionHistoryRow(
                "#TXN-" + transaction.getTransactionId(),
                transaction.getTransactionDate(),
                transactionTypeMessageKey(transaction.getTransactionType()),
                transaction.getAmount(),
                transactionStatusMessageKey(transaction.getStatus()),
                completed);
    }

    private MemberTransactionHistoryRow toHistoryRow(PayOsPayment payment) {
        String typeLabel = switch (payment.getPurpose()) {
            case "TOP_UP" -> "transaction.type.kqpayTopUp";
            case "BORROW_FEE" -> "transaction.type.kqpayBorrowFee";
            case "FINE" -> "transaction.type.kqpayFine";
            case "FINE_BATCH" -> "transaction.type.kqpayFineBatch";
            default -> "transaction.type.kqpay";
        };
        BigDecimal amount = payment.getAmount();
        if (!"TOP_UP".equalsIgnoreCase(payment.getPurpose()) && amount != null) {
            amount = amount.abs().negate();
        }
        return new MemberTransactionHistoryRow(
                "#KQ-" + payment.getPaymentId(),
                payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt(),
                typeLabel,
                amount,
                "transaction.status.completed",
                true);
    }

    private String transactionTypeMessageKey(String transactionType) {
        if (transactionType == null) {
            return "transaction.type.other";
        }
        return switch (transactionType.toUpperCase()) {
            case "TOP_UP" -> "transaction.type.topUp";
            case "BORROW_FEE" -> "transaction.type.borrowFee";
            case "DEPOSIT" -> "transaction.type.deposit";
            case "FINE" -> "transaction.type.fine";
            case "DAMAGE_FEE" -> "transaction.type.damageFee";
            case "REFUND" -> "transaction.type.refund";
            default -> "transaction.type.other";
        };
    }

    private String transactionStatusMessageKey(String status) {
        if (status == null) {
            return "transaction.status.pending";
        }
        return switch (status.trim().toUpperCase()) {
            case "COMPLETED", "PAID" -> "transaction.status.completed";
            case "FAILED" -> "transaction.status.failed";
            case "CANCELED", "CANCELLED" -> "transaction.status.canceled";
            default -> "transaction.status.pending";
        };
    }

    private List<PayOsPayment> getKqPayHistory(Integer memberId, String selectedType) {
        return payOsPaymentRepository
                .findTop10ByMemberMemberIdAndStatusOrderByPaidAtDesc(memberId, "PAID")
                .stream()
                .filter(payment -> "FINE_BATCH".equalsIgnoreCase(payment.getPurpose()))
                .filter(payment -> matchesKqPayHistoryFilter(payment.getPurpose(), selectedType))
                .toList();
    }

    private Set<Integer> getAggregatedFineIds(List<PayOsPayment> payments) {
        List<Long> paymentIds = payments.stream()
                .map(PayOsPayment::getPaymentId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (paymentIds.isEmpty()) {
            return Set.of();
        }
        Set<Integer> transactionIds = new HashSet<>();
        payOsPaymentFineItemRepository.findByPaymentPaymentIdIn(paymentIds).forEach(item -> {
            if (item.getFineTransaction() != null && item.getFineTransaction().getTransactionId() != null) {
                transactionIds.add(item.getFineTransaction().getTransactionId());
            }
        });
        return transactionIds;
    }

    private boolean matchesKqPayHistoryFilter(String purpose, String selectedType) {
        if (selectedType == null || selectedType.isBlank()) {
            return true;
        }
        return switch (selectedType.trim().toUpperCase()) {
            case "TOP_UP" -> "TOP_UP".equalsIgnoreCase(purpose);
            case "BORROW_FEE" -> "BORROW_FEE".equalsIgnoreCase(purpose);
            case "FINE", "DAMAGE_FEE" -> "FINE".equalsIgnoreCase(purpose)
                    || "FINE_BATCH".equalsIgnoreCase(purpose);
            default -> false;
        };
    }

    @GetMapping("/topup-notifications")
    public String viewTopupNotifications(Principal principal,
                                         @RequestParam(defaultValue = "0") int page,
                                         Model model) {
        Member member = getCurrentMember(principal);
        Page<MemberNotification> notificationPage = memberNotificationRepository.findTopupNotifications(
                member.getMemberId(),
                TOP_UP_NOTIFICATION_KEYWORD_VI,
                TOP_UP_NOTIFICATION_KEYWORD_EN,
                pageRequest(page, DEFAULT_PAGE_SIZE));

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

        redirectAttributes.addFlashAttribute("success", message("backend.notification.markedRead"));
        return "redirect:/member/financial/topup-notifications?page=" + Math.max(page, 0);
    }

    @PostMapping("/topup-notifications/mark-all-read")
    public String markAllTopupNotificationsAsRead(@RequestParam(defaultValue = "0") int page,
                                                  Principal principal,
                                                  RedirectAttributes redirectAttributes) {
        Member member = getCurrentMember(principal);
        Page<MemberNotification> notificationPage = memberNotificationRepository.findTopupNotifications(
                member.getMemberId(),
                TOP_UP_NOTIFICATION_KEYWORD_VI,
                TOP_UP_NOTIFICATION_KEYWORD_EN,
                pageRequest(0, MARK_ALL_READ_LIMIT));

        for (MemberNotification memberNotification : notificationPage.getContent()) {
            if (!Boolean.TRUE.equals(memberNotification.getIsRead())) {
                memberNotification.setIsRead(true);
                memberNotification.setReadDate(LocalDateTime.now());
                memberNotificationRepository.save(memberNotification);
            }
        }

        redirectAttributes.addFlashAttribute("success", message("backend.notification.topupAllRead"));
        return "redirect:/member/financial/topup-notifications?page=" + Math.max(page, 0);
    }

    private void loadBorrowingFeeGroups(Integer memberId,
                                        List<BorrowFeeViewData> pendingFees,
                                        List<BorrowFeeViewData> payableFees,
                                        List<BorrowFeeViewData> paidFees) {
        List<Borrow> borrows = borrowRepository.findByMember_MemberIdOrderByBorrowDateDesc(memberId);
        for (Borrow borrow : borrows) {
            BorrowFeeViewData feeViewData = buildBorrowFeeViewData(memberId, borrow);
            if (feeViewData != null) {
                addToFeeGroup(feeViewData, pendingFees, payableFees, paidFees);
            }
        }
    }

    private void addToFeeGroup(BorrowFeeViewData feeViewData,
                               List<BorrowFeeViewData> pendingFees,
                               List<BorrowFeeViewData> payableFees,
                               List<BorrowFeeViewData> paidFees) {
        if ("Paid".equalsIgnoreCase(feeViewData.getPaymentStatus())) {
            paidFees.add(feeViewData);
        } else if ("Pending".equalsIgnoreCase(feeViewData.getBorrowStatus())) {
            pendingFees.add(feeViewData);
        } else if (feeViewData.isPayable()) {
            payableFees.add(feeViewData);
        }
    }

    private BigDecimal getWalletBalance(Integer memberId) {
        return walletRepository.findByMemberMemberId(memberId)
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);
    }

    private List<Transaction> getUnpaidFines(Integer memberId) {
        return transactionRepository.findUnpaidFineTransactions(memberId, List.of(FINE_TYPE, DAMAGE_FEE_TYPE));
    }

    private BigDecimal totalAbsAmount(List<Transaction> transactions) {
        return transactions.stream()
                .map(transaction -> transaction.getAmount() == null
                        ? BigDecimal.ZERO
                        : transaction.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), size);
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

        feeViewData.setAmount(financialService.calculateBorrowingFeeAmount(borrow.getBorrowId()));

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
            throw new UnauthorizedException(message("backend.financial.loginRequired"));
        }

        String usernameOrEmail = principal.getName();
        return memberRepository.findByAccountUsername(usernameOrEmail)
                .or(() -> memberRepository.findByUserEmail(usernameOrEmail))
                .or(() -> memberRepository.findByUserPhone(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException(message("backend.member.currentNotFound")));
    }
}
