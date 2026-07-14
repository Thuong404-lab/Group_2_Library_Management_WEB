package com.lms.service;

import com.lms.entity.*;
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
    private static final String COMPLETED = "Completed";

    private final PayOsWalletRepository walletRepository;
    private final PayOsTransactionRepository transactionRepository;
    private final PayOsBorrowRepository borrowRepository;
    private final PayOsPaymentFineItemRepository fineItemRepository;
    private final FinancialService financialService;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public PayOsSettlementService(PayOsWalletRepository walletRepository,
                                  PayOsTransactionRepository transactionRepository,
                                  PayOsBorrowRepository borrowRepository,
                                  PayOsPaymentFineItemRepository fineItemRepository,
                                  FinancialService financialService,
                                  NotificationRepository notificationRepository,
                                  MemberNotificationRepository memberNotificationRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.borrowRepository = borrowRepository;
        this.fineItemRepository = fineItemRepository;
        this.financialService = financialService;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction settle(PayOsPayment payment) {
        return switch (payment.getPurpose()) {
            case PayOsPaymentService.TOP_UP -> settleTopUp(payment);
            case PayOsPaymentService.FINE -> settleFine(payment);
            case PayOsPaymentService.FINE_BATCH -> settleFineBatch(payment);
            case PayOsPaymentService.BORROW_FEE -> settleBorrowFee(payment);
            default -> throw new RuntimeException("Loại thanh toán KQPay không được hỗ trợ.");
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
        createNotification(member, "Nạp tiền qua KQPay thành công",
                "Ví của bạn đã được nạp " + formatMoney(amount)
                        + ". Số dư hiện tại: " + formatMoney(newBalance) + ".");
        return transaction;
    }

    private Transaction settleFine(PayOsPayment payment) {
        Transaction fine = transactionRepository.findByIdForUpdate(payment.getReferenceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khoản phạt."));
        validateOwner(fine, payment.getMember().getMemberId());
        String type = normalize(fine.getTransactionType());
        if (!"FINE".equals(type) && !"DAMAGE_FEE".equals(type)) {
            throw new RuntimeException("Giao dịch không phải khoản phạt.");
        }
        if (isCompleted(fine.getStatus())) {
            throw new RuntimeException("Khoản phạt đã được thanh toán.");
        }
        BigDecimal amount = requirePositiveWholeVnd(payment.getAmount());
        if (fine.getAmount() == null || fine.getAmount().abs().compareTo(amount) != 0) {
            throw new RuntimeException("Số tiền khoản phạt đã thay đổi.");
        }
        fine.setAmount(amount.negate());
        fine.setStatus(COMPLETED);
        fine.setTransactionDate(LocalDateTime.now());
        return transactionRepository.save(fine);
    }

    private Transaction settleFineBatch(PayOsPayment payment) {
        List<PayOsPaymentFineItem> items = fineItemRepository
                .findByPaymentPaymentIdOrderByFineTransactionTransactionId(payment.getPaymentId());
        if (items.isEmpty()) {
            throw new RuntimeException("Đơn thanh toán không có khoản phạt.");
        }

        BigDecimal total = BigDecimal.ZERO;
        Transaction first = null;
        for (PayOsPaymentFineItem item : items) {
            Integer fineId = item.getFineTransaction().getTransactionId();
            Transaction fine = transactionRepository.findByIdForUpdate(fineId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khoản phạt #" + fineId + "."));
            validateOwner(fine, payment.getMember().getMemberId());
            String type = normalize(fine.getTransactionType());
            if ((!"FINE".equals(type) && !"DAMAGE_FEE".equals(type)) || isCompleted(fine.getStatus())) {
                throw new RuntimeException("Khoản phạt #" + fineId + " không còn khả dụng.");
            }
            BigDecimal snapshot = requirePositiveWholeVnd(item.getAmountSnapshot());
            if (fine.getAmount() == null || fine.getAmount().abs().compareTo(snapshot) != 0) {
                throw new RuntimeException("Số tiền khoản phạt #" + fineId + " đã thay đổi.");
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
            throw new RuntimeException("Tổng tiền phạt không khớp với đơn KQPay.");
        }
        return first;
    }

    private Transaction settleBorrowFee(PayOsPayment payment) {
        Integer memberId = payment.getMember().getMemberId();
        Integer borrowId = payment.getReferenceId();
        Borrow borrow = borrowRepository.findByIdForUpdate(borrowId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu mượn."));
        if (borrow.getMember() == null || !memberId.equals(borrow.getMember().getMemberId())) {
            throw new RuntimeException("Phiếu mượn không thuộc về thành viên.");
        }
        String status = normalize(borrow.getStatus());
        if (!"ACTIVE".equals(status) && !"BORROWING".equals(status) && !"OVERDUE".equals(status)) {
            throw new RuntimeException("Phiếu mượn không thể thanh toán.");
        }
        if (transactionRepository.hasCompletedBorrowFee(memberId, borrowId)) {
            throw new RuntimeException("Phí mượn đã được thanh toán.");
        }
        BigDecimal amount = requirePositiveWholeVnd(payment.getAmount());
        if (financialService.calculateBorrowingFeeAmount(borrowId).compareTo(amount) != 0) {
            throw new RuntimeException("Phí mượn đã thay đổi, vui lòng tạo lại mã QR.");
        }
        Wallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví thành viên."));
        return saveTransaction(wallet, borrow, "BORROW_FEE", amount.negate());
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
            throw new RuntimeException("Khoản phí không thuộc về thành viên.");
        }
    }

    private BigDecimal requirePositiveWholeVnd(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.stripTrailingZeros();
        if (value.signum() <= 0 || value.scale() > 0) {
            throw new RuntimeException("Số tiền KQPay không hợp lệ.");
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
        return String.format("%,.0f VNĐ", amount);
    }
}
