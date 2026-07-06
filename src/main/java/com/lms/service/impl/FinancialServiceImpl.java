package com.lms.service.impl;

import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.Notification;
import com.lms.entity.SystemSetting;
import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.FinancialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinancialServiceImpl implements FinancialService {
    private static final String BORROW_FEE_TYPE = "BORROW_FEE";
    private static final String FINE_TYPE = "FINE";
    private static final String DAMAGE_FEE_TYPE = "DAMAGE_FEE";
    private static final String TOP_UP_TYPE = "TOP_UP";
    private static final String COMPLETED_STATUS = "Completed";
    private static final String PAID_STATUS = "Paid";
    private static final String BORROW_FEE_SETTING_KEY = "BORROW_FEE_PER_BOOK";
    private static final int DEFAULT_BORROW_FEE_DAYS = 10;
    private static final BigDecimal DEFAULT_BORROW_FEE_PER_BOOK = BigDecimal.valueOf(5000);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public FinancialServiceImpl(TransactionRepository transactionRepository,
                                WalletRepository walletRepository,
                                BorrowRepository borrowRepository,
                                BorrowDetailRepository borrowDetailRepository,
                                SystemSettingRepository systemSettingRepository,
                                MemberRepository memberRepository,
                                NotificationRepository notificationRepository,
                                MemberNotificationRepository memberNotificationRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOverdueFine(Integer memberId, Integer fineId) {
        Transaction fine = transactionRepository.findById(fineId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khoản phạt với ID: " + fineId));

        validateTransactionOwner(fine, memberId);

        String type = normalize(fine.getTransactionType());
        if (!FINE_TYPE.equals(type) && !DAMAGE_FEE_TYPE.equals(type)) {
            throw new RuntimeException("Giao dịch này không phải khoản phạt.");
        }

        if (isCompletedStatus(fine.getStatus())) {
            throw new RuntimeException("Khoản phạt này đã được thanh toán.");
        }

        BigDecimal fineAmount = amountOrZero(fine.getAmount()).abs();
        if (fineAmount.signum() <= 0) {
            throw new RuntimeException("Số tiền phạt không hợp lệ.");
        }

        var wallet = fine.getWallet();
        BigDecimal currentBalance = balanceOf(wallet.getBalance());
        ensureSufficientBalance(currentBalance, fineAmount, "phí phạt");
        wallet.setBalance(currentBalance.subtract(fineAmount));
        walletRepository.save(wallet);

        fine.setAmount(fineAmount.negate());
        fine.setTransactionDate(LocalDateTime.now());
        fine.setStatus(COMPLETED_STATUS);
        transactionRepository.save(fine);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payBorrowingFee(Integer memberId, Integer borrowId) {
        Borrow borrow = findBorrowForMember(memberId, borrowId);

        if (hasPaidBorrowingFee(memberId, borrowId)) {
            throw new RuntimeException("Phí mượn của phiếu này đã được thanh toán.");
        }

        String borrowStatus = normalize(borrow.getStatus());
        if (!"ACTIVE".equals(borrowStatus) && !"BORROWING".equals(borrowStatus) && !"OVERDUE".equals(borrowStatus)) {
            throw new RuntimeException("Chỉ có thể thanh toán phí cho phiếu mượn đã được duyệt.");
        }

        BigDecimal feeAmount = calculateBorrowingFeeAmount(borrowId);
        if (feeAmount.signum() <= 0) {
            throw new RuntimeException("Phí mượn không hợp lệ.");
        }

        var wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của thành viên."));

        BigDecimal currentBalance = balanceOf(wallet.getBalance());
        if (currentBalance.compareTo(feeAmount) < 0) {
            throw new RuntimeException("Số dư ví không đủ để thanh toán phí mượn.");
        }

        wallet.setBalance(currentBalance.subtract(feeAmount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(borrow);
        transaction.setTransactionType(BORROW_FEE_TYPE);
        transaction.setAmount(feeAmount.negate());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(COMPLETED_STATUS);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBorrowingFeeAmount(Integer borrowId) {
        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        if (details == null || details.isEmpty()) {
            throw new RuntimeException("Phiếu mượn không có chi tiết.");
        }

        BigDecimal perBookPerDay = getBorrowFeePerBookPerDay();
        return BigDecimal.valueOf(details.size())
                .multiply(BigDecimal.valueOf(DEFAULT_BORROW_FEE_DAYS))
                .multiply(perBookPerDay);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPaidBorrowingFee(Integer memberId, Integer borrowId) {
        return transactionRepository.hasCompletedBorrowFee(memberId, borrowId);
    }

    @Override
    public void payReservationDeposit(Integer memberId, Integer reservationId) {
        // TODO: Implement UC-8.3.
    }

    @Override
    public void getTransactionHistory(Integer memberId, int page) {
        // TODO: Implement UC-8.4 service-level pagination if needed.
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFine(Integer memberId, Double amount, String reason) {
        if (amount == null || amount <= 0) {
            throw new RuntimeException("Số tiền phạt phải lớn hơn 0.");
        }

        var wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của thành viên."));

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setTransactionType(FINE_TYPE);
        transaction.setAmount(BigDecimal.valueOf(amount).abs().negate());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("Pending");
        transactionRepository.save(transaction);

        Member member = wallet.getMember();
        createMemberNotification(
                member,
                "Phí phạt mới",
                "Thư viện đã ghi nhận khoản phạt " + formatMoney(transaction.getAmount().abs())
                        + ". Lý do: " + (reason == null || reason.isBlank() ? "Vi phạm quy định thư viện" : reason.trim())
                        + ".");
    }

    @Override
    public void getAllTransactions(int page, String type) {
        // TODO: Implement UC-14.3.
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topUpMemberAccount(String memberPhone, Double amount) {
        if (memberPhone == null || memberPhone.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập email, số điện thoại hoặc ID thành viên.");
        }
        if (amount == null || amount <= 0) {
            throw new RuntimeException("Số tiền nạp phải lớn hơn 0.");
        }

        Member member = findMemberByLookup(memberPhone.trim());
        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseGet(() -> createWalletForMember(member));

        BigDecimal topUpAmount = BigDecimal.valueOf(amount).abs();
        BigDecimal newBalance = balanceOf(wallet.getBalance()).add(topUpAmount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setTransactionType(TOP_UP_TYPE);
        transaction.setAmount(topUpAmount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(COMPLETED_STATUS);
        transactionRepository.save(transaction);

        createMemberNotification(
                member,
                "Nạp tiền thành công",
                "Tài khoản ví của bạn vừa được nạp " + formatMoney(topUpAmount)
                        + ". Số dư hiện tại: " + formatMoney(newBalance) + ".");
    }

    private Borrow findBorrowForMember(Integer memberId, Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu mượn với ID: " + borrowId));

        if (borrow.getMember() == null
                || borrow.getMember().getMemberId() == null
                || !borrow.getMember().getMemberId().equals(memberId)) {
            throw new RuntimeException("Phiếu mượn không thuộc về thành viên hiện tại.");
        }

        return borrow;
    }

    private Member findMemberByLookup(String lookup) {
        if (lookup.matches("\\d+")) {
            try {
                Integer memberId = Integer.valueOf(lookup);
                return memberRepository.findById(memberId)
                        .or(() -> memberRepository.findByUserPhone(lookup))
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên với thông tin: " + lookup));
            } catch (NumberFormatException ignored) {
                return memberRepository.findByUserPhone(lookup)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên với thông tin: " + lookup));
            }
        }

        return memberRepository.findByUserPhone(lookup)
                .or(() -> memberRepository.findByUserEmail(lookup))
                .or(() -> memberRepository.findByAccountUsername(lookup))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên với thông tin: " + lookup));
    }

    private Wallet createWalletForMember(Member member) {
        Wallet wallet = new Wallet();
        wallet.setMember(member);
        wallet.setBalance(BigDecimal.ZERO);
        return walletRepository.save(wallet);
    }

    private void createMemberNotification(Member member, String title, String content) {
        if (member == null || member.getMemberId() == null) {
            return;
        }

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

    private String formatMoney(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return String.format("%,.0f VNĐ", safeAmount);
    }

    private BigDecimal getBorrowFeePerBookPerDay() {
        try {
            return systemSettingRepository.findAll().stream()
                    .filter(setting -> setting.getSettingKey() != null)
                    .filter(setting -> BORROW_FEE_SETTING_KEY.equalsIgnoreCase(setting.getSettingKey()))
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .filter(value -> value.signum() > 0)
                    .findFirst()
                    .orElse(DEFAULT_BORROW_FEE_PER_BOOK);
        } catch (Exception ignored) {
            return DEFAULT_BORROW_FEE_PER_BOOK;
        }
    }

    private void validateTransactionOwner(Transaction transaction, Integer memberId) {
        if (transaction.getWallet() == null
                || transaction.getWallet().getMember() == null
                || transaction.getWallet().getMember().getMemberId() == null
                || !transaction.getWallet().getMember().getMemberId().equals(memberId)) {
            throw new RuntimeException("Giao dịch không thuộc về thành viên hiện tại.");
        }
    }

    private boolean isCompletedStatus(String status) {
        String normalizedStatus = normalize(status);
        return COMPLETED_STATUS.toUpperCase().equals(normalizedStatus)
                || PAID_STATUS.toUpperCase().equals(normalizedStatus);
    }

    private BigDecimal amountOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal balanceOf(BigDecimal balance) {
        return balance == null ? BigDecimal.ZERO : balance;
    }

    private void ensureSufficientBalance(BigDecimal currentBalance, BigDecimal requiredAmount, String paymentName) {
        BigDecimal safeBalance = balanceOf(currentBalance);
        BigDecimal safeRequiredAmount = amountOrZero(requiredAmount).abs();
        if (safeBalance.compareTo(safeRequiredAmount) < 0) {
            throw new RuntimeException("Số dư ví không đủ để thanh toán " + paymentName
                    + ". Số dư hiện tại: " + formatMoney(safeBalance)
                    + ", cần thanh toán: " + formatMoney(safeRequiredAmount) + ".");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
