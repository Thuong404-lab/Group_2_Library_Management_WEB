package com.lms.service;

import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.Notification;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import com.lms.exception.ConflictException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.payos.PayOsTransactionRepository;
import com.lms.repository.payos.PayOsWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Batch fine payment owned by the aggregate payment flow. */
@Service
public class FineBatchPaymentService {
    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();
    private final TransactionRepository transactionRepository;
    private final PayOsTransactionRepository lockedTransactionRepository;
    private final PayOsWalletRepository walletRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public FineBatchPaymentService(TransactionRepository transactionRepository,
                                   PayOsTransactionRepository lockedTransactionRepository,
                                   PayOsWalletRepository walletRepository,
                                   NotificationRepository notificationRepository,
                                   MemberNotificationRepository memberNotificationRepository) {
        this.transactionRepository = transactionRepository;
        this.lockedTransactionRepository = lockedTransactionRepository;
        this.walletRepository = walletRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void payAllFromWallet(Integer memberId) {
        List<Transaction> pending = transactionRepository.findUnpaidFineTransactions(
                memberId, List.of("FINE", "DAMAGE_FEE"));
        if (pending.isEmpty()) {
            throw new ConflictException(messages.get("backend.payment.noFinesDue"));
        }
        pending.sort(Comparator.comparing(Transaction::getTransactionId));

        BigDecimal total = BigDecimal.ZERO;
        List<Transaction> lockedFines = new java.util.ArrayList<>();
        for (Transaction candidate : pending) {
            Transaction fine = lockedTransactionRepository.findByIdForUpdate(candidate.getTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.fineNotFound")));
            validateFine(fine, memberId);
            total = total.add(fine.getAmount().abs());
            lockedFines.add(fine);
        }

        Wallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.financial.walletNotFound")));
        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        if (balance.compareTo(total) < 0) {
            throw new ConflictException(messages.get("backend.payment.batchInsufficientBalance"));
        }
        wallet.setBalance(balance.subtract(total));
        walletRepository.save(wallet);

        LocalDateTime paidAt = LocalDateTime.now();
        for (Transaction fine : lockedFines) {
            fine.setAmount(fine.getAmount().abs().negate());
            fine.setStatus("Completed");
            fine.setTransactionDate(paidAt);
            lockedTransactionRepository.save(fine);
        }

        createNotification(
                wallet.getMember(),
                NotificationType.FINANCE, NotificationEventType.FINE_PAID, NotificationSource.SYSTEM,
                "systemNotification.fine.walletPaid.title",
                "systemNotification.fine.walletBatchPaid.content",
                total, lockedFines.size(), wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public List<Transaction> getPendingForBorrow(Integer borrowId) {
        return transactionRepository.findPendingFineTransactionsByBorrowId(
                borrowId, List.of("FINE", "DAMAGE_FEE"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void payBorrowFinesByCash(Integer borrowId) {
        List<Transaction> fines = lockBorrowFines(borrowId);
        BigDecimal total = fines.stream()
                .map(fine -> fine.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime paidAt = LocalDateTime.now();
        for (Transaction fine : fines) {
            fine.setAmount(fine.getAmount().abs().negate());
            fine.setStatus("Completed");
            fine.setTransactionDate(paidAt);
            lockedTransactionRepository.save(fine);
        }
        createNotification(
                fines.get(0).getWallet().getMember(),
                NotificationType.FINANCE, NotificationEventType.FINE_PAID, NotificationSource.LIBRARIAN,
                "systemNotification.fine.cashPaid.title",
                "systemNotification.fine.cashBatchPaid.content",
                total, fines.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void payBorrowFinesFromWallet(Integer borrowId) {
        List<Transaction> fines = lockBorrowFines(borrowId);
        Integer memberId = fines.get(0).getWallet().getMember().getMemberId();
        BigDecimal total = fines.stream()
                .map(fine -> fine.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Wallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.financial.walletNotFound")));
        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        if (balance.compareTo(total) < 0) {
            throw new ConflictException(messages.get("backend.payment.batchInsufficientBalance"));
        }
        wallet.setBalance(balance.subtract(total));
        walletRepository.save(wallet);

        LocalDateTime paidAt = LocalDateTime.now();
        for (Transaction fine : fines) {
            fine.setAmount(fine.getAmount().abs().negate());
            fine.setStatus("Completed");
            fine.setTransactionDate(paidAt);
            lockedTransactionRepository.save(fine);
        }
        createNotification(
                wallet.getMember(),
                NotificationType.FINANCE, NotificationEventType.FINE_PAID, NotificationSource.LIBRARIAN,
                "systemNotification.fine.walletPaid.title",
                "systemNotification.fine.walletBatchPaid.content",
                total, fines.size(), wallet.getBalance());
    }

    private List<Transaction> lockBorrowFines(Integer borrowId) {
        List<Transaction> pending = getPendingForBorrow(borrowId);
        if (pending.isEmpty()) {
            throw new ConflictException(messages.get("backend.payment.noFinesDue"));
        }
        pending.sort(Comparator.comparing(Transaction::getTransactionId));
        List<Transaction> locked = new java.util.ArrayList<>();
        Integer memberId = null;
        for (Transaction candidate : pending) {
            Transaction fine = lockedTransactionRepository.findByIdForUpdate(candidate.getTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.fineNotFound")));
            Integer currentMemberId = fine.getWallet() == null || fine.getWallet().getMember() == null
                    ? null : fine.getWallet().getMember().getMemberId();
            if (currentMemberId == null) {
                throw new ResourceNotFoundException(messages.get("backend.financial.memberNotFound"));
            }
            if (memberId == null) {
                memberId = currentMemberId;
            }
            validateFine(fine, memberId);
            locked.add(fine);
        }
        return locked;
    }

    private void createNotification(Member member,
                                    NotificationType type,
                                    NotificationEventType eventType,
                                    NotificationSource source,
                                    String titleKey,
                                    String contentKey,
                                    Object... arguments) {
        if (member == null || member.getMemberId() == null) {
            return;
        }
        Notification notification = new Notification();
        messages.prepareNotification(notification, titleKey, contentKey, arguments);
        notification.setNotificationType(type);
        notification.setEventType(eventType);
        notification.setNotificationSource(source);
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

    private void validateFine(Transaction fine, Integer memberId) {
        if (fine.getWallet() == null || fine.getWallet().getMember() == null
                || !memberId.equals(fine.getWallet().getMember().getMemberId())) {
            throw new ForbiddenException(messages.get("backend.payment.fineMemberMismatch"));
        }
        String type = normalize(fine.getTransactionType());
        String status = normalize(fine.getStatus());
        if ((!"FINE".equals(type) && !"DAMAGE_FEE".equals(type))
                || "COMPLETED".equals(status) || "PAID".equals(status)
                || fine.getAmount() == null || fine.getAmount().signum() == 0) {
            throw new ConflictException(messages.get("backend.payment.fineListChanged"));
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
