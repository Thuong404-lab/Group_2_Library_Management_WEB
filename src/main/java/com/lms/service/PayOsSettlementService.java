package com.lms.service;

import com.lms.entity.*;
import com.lms.exception.ConflictException;
import com.lms.exception.DataProcessingException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.PayOsPaymentFineItemRepository;
import com.lms.repository.payos.PayOsBorrowRepository;
import com.lms.repository.payos.PayOsTransactionRepository;
import com.lms.repository.payos.PayOsWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;

/** Isolated settlement logic owned by the PayOS integration flow. */
@Service
public class PayOsSettlementService {

    private final LocalizedMessageService localizedMessageService;
    private static final String COMPLETED = "Completed";

    private final PayOsWalletRepository walletRepository;
    private final PayOsTransactionRepository transactionRepository;
    private final PayOsBorrowRepository borrowRepository;
    private final PayOsPaymentFineItemRepository fineItemRepository;
    private final FinancialService financialService;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final BorrowService borrowService;

    public PayOsSettlementService(PayOsWalletRepository walletRepository,
                                  PayOsTransactionRepository transactionRepository,
                                  PayOsBorrowRepository borrowRepository,
                                  PayOsPaymentFineItemRepository fineItemRepository,
                                  FinancialService financialService,
                                  NotificationRepository notificationRepository,
                                  MemberNotificationRepository memberNotificationRepository,
                                  BorrowService borrowService,
                                  LocalizedMessageService localizedMessageService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.borrowRepository = borrowRepository;
        this.fineItemRepository = fineItemRepository;
        this.financialService = financialService;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.borrowService = borrowService;
        this.localizedMessageService = localizedMessageService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction settle(PayOsPayment payment) {
        return switch (payment.getPurpose()) {
            case PayOsPaymentService.TOP_UP -> settleTopUp(payment);
            case PayOsPaymentService.FINE -> settleFine(payment);
            case PayOsPaymentService.FINE_BATCH -> settleFineBatch(payment);
            case PayOsPaymentService.BORROW_FEE -> settleBorrowFee(payment);
            default -> throw new DataProcessingException(localizedMessageService.get("backend.payment.unsupportedPurpose"));
        };
    }

    private Transaction settleTopUp(PayOsPayment payment) {
        Member member = payment.getMember();
        BigDecimal amount = requirePositiveWholeVnd(payment.getAmount());
        Wallet wallet = walletRepository.findByMemberIdForUpdate(member.getMemberId())
                .orElseGet(() -> createWallet(member));
        BigDecimal newBalance = balance(wallet).add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = saveTransaction(wallet, null, "TOP_UP", amount);
        createNotification(member,
                localizedMessageService.get("systemNotification.kqpay.topup.title"),
                localizedMessageService.get("systemNotification.topup.success.content", formatMoney(amount), formatMoney(newBalance)));
        return transaction;
    }

    private Transaction settleFine(PayOsPayment payment) {
        Transaction fine = transactionRepository.findByIdForUpdate(payment.getReferenceId())
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.payment.fineNotFound")));
        validateOwner(fine, payment.getMember().getMemberId());
        String type = normalize(fine.getTransactionType());
        if (!"FINE".equals(type) && !"DAMAGE_FEE".equals(type)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.notFineTransaction"));
        }
        if (isCompleted(fine.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.financial.fineAlreadyPaid"));
        }
        BigDecimal amount = requirePositiveWholeVnd(payment.getAmount());
        if (fine.getAmount() == null || fine.getAmount().abs().compareTo(amount) != 0) {
            throw new ConflictException(localizedMessageService.get("backend.payment.fineAmountChanged"));
        }
        fine.setAmount(amount.negate());
        fine.setStatus(COMPLETED);
        fine.setTransactionDate(LocalDateTime.now());
        Transaction saved = transactionRepository.save(fine);
        createNotification(payment.getMember(),
                localizedMessageService.get("systemNotification.fine.kqpayPaid.title"),
                localizedMessageService.get("systemNotification.fine.kqpayPaid.content",
                        formatMoney(amount), fine.getTransactionId()));
        return saved;
    }

    private Transaction settleFineBatch(PayOsPayment payment) {
        List<PayOsPaymentFineItem> items = fineItemRepository
                .findByPaymentPaymentIdOrderByFineTransactionTransactionId(payment.getPaymentId());
        if (items.isEmpty()) {
            throw new DataProcessingException(localizedMessageService.get("backend.payment.noFineItems"));
        }

        BigDecimal total = BigDecimal.ZERO;
        Transaction first = null;
        for (PayOsPaymentFineItem item : items) {
            Integer fineId = item.getFineTransaction().getTransactionId();
            Transaction fine = transactionRepository.findByIdForUpdate(fineId)
                    .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.fineNotFound", fineId)));
            validateOwner(fine, payment.getMember().getMemberId());
            String type = normalize(fine.getTransactionType());
            if ((!"FINE".equals(type) && !"DAMAGE_FEE".equals(type)) || isCompleted(fine.getStatus())) {
                throw new ConflictException(localizedMessageService.get("backend.payment.fineUnavailable", fineId));
            }
            BigDecimal snapshot = requirePositiveWholeVnd(item.getAmountSnapshot());
            if (fine.getAmount() == null || fine.getAmount().abs().compareTo(snapshot) != 0) {
                throw new ConflictException(localizedMessageService.get("backend.payment.fineItemAmountChanged", fineId));
            }
            total = total.add(snapshot);
            fine.setAmount(snapshot.negate());
            fine.setStatus(COMPLETED);
            fine.setTransactionDate(LocalDateTime.now());
            Transaction saved = transactionRepository.save(fine);
            if (first == null) {
                first = saved;
            }
        }
        if (total.compareTo(payment.getAmount()) != 0) {
            throw new ConflictException(localizedMessageService.get("backend.payment.fineTotalMismatch"));
        }
        createNotification(payment.getMember(),
                localizedMessageService.get("systemNotification.fine.kqpayPaid.title"),
                localizedMessageService.get("systemNotification.fine.kqpayBatchPaid.content",
                        formatMoney(total), items.size()));
        return first;
    }

    private Transaction settleBorrowFee(PayOsPayment payment) {
        Integer memberId = payment.getMember().getMemberId();
        Integer borrowId = payment.getReferenceId();
        Borrow borrow = borrowRepository.findByIdForUpdate(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.notFoundById", borrowId)));
        if (borrow.getMember() == null || !memberId.equals(borrow.getMember().getMemberId())) {
            throw new ForbiddenException(localizedMessageService.get("backend.financial.loanOwnerMismatch"));
        }
        String status = normalize(borrow.getStatus());
        if (!"ACTIVE".equals(status) && !"BORROWING".equals(status) && !"OVERDUE".equals(status)
                && !"PAYMENT_PENDING".equals(status)) {
            throw new ConflictException(localizedMessageService.get("backend.payment.loanNotPayable"));
        }
        if (transactionRepository.hasCompletedBorrowFee(memberId, borrowId)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.borrowFeeAlreadyPaid"));
        }
        BigDecimal amount = requirePositiveWholeVnd(payment.getAmount());
        if (financialService.calculateBorrowingFeeAmount(borrowId).compareTo(amount) != 0) {
            throw new ConflictException(localizedMessageService.get("backend.payment.borrowFeeChanged"));
        }
        Wallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.walletNotFound")));
        Transaction transaction = saveTransaction(wallet, borrow, "BORROW_FEE", amount.negate());
        borrowService.activatePendingBankBorrow(borrowId);
        return transaction;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingBorrow(PayOsPayment payment, String paymentStatus) {
        if (payment != null && PayOsPaymentService.BORROW_FEE.equals(payment.getPurpose())
                && payment.getReferenceId() != null) {
            borrowService.cancelPendingBankBorrow(payment.getReferenceId(), paymentStatus);
        }
    }

    private Transaction saveTransaction(Wallet wallet, Borrow borrow, String type, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(borrow);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setStatus(COMPLETED);
        transaction.setTransactionDate(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private Wallet createWallet(Member member) {
        Wallet wallet = new Wallet();
        wallet.setMember(member);
        wallet.setBalance(BigDecimal.ZERO);
        return walletRepository.save(wallet);
    }

    private void createNotification(Member member, String title, String content) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setStatus("Active");
        notification = notificationRepository.save(notification);

        MemberNotification memberNotification = new MemberNotification();
        memberNotification.setId(new MemberNotificationId(member.getMemberId(), notification.getNotificationId()));
        memberNotification.setMember(member);
        memberNotification.setNotification(notification);
        memberNotification.setIsRead(false);
        memberNotificationRepository.save(memberNotification);
    }

    private void validateOwner(Transaction transaction, Integer memberId) {
        if (transaction.getWallet() == null || transaction.getWallet().getMember() == null
                || !memberId.equals(transaction.getWallet().getMember().getMemberId())) {
            throw new ForbiddenException(localizedMessageService.get("backend.payment.feeOwnerMismatch"));
        }
    }

    private BigDecimal requirePositiveWholeVnd(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        if (value.signum() <= 0 || value.scale() > 0) {
            throw new DataProcessingException(localizedMessageService.get("backend.payment.invalidKqpayAmount"));
        }
        return value;
    }

    private BigDecimal balance(Wallet wallet) {
        return wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
    }

    private boolean isCompleted(String status) {
        String value = normalize(status);
        return "COMPLETED".equals(value) || "PAID".equals(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String formatMoney(BigDecimal amount) {
        return localizedMessageService.get("currency.vndAmount", String.format("%,.0f", amount));
    }
}
