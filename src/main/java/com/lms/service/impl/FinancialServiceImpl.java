package com.lms.service.impl;

import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.Notification;
import com.lms.entity.Reservation;
import com.lms.entity.SystemSetting;
import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.exception.ConflictException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.FinancialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Financial transaction rules maintained by Pham Kien Quoc for member fees,
 * fines, deposits, top-ups, and librarian transaction history.
 */
@Service
public class FinancialServiceImpl implements FinancialService {
    private static final String BORROW_FEE_TYPE = "BORROW_FEE";
    private static final String FINE_TYPE = "FINE";
    private static final String DAMAGE_FEE_TYPE = "DAMAGE_FEE";
    private static final String TOP_UP_TYPE = "TOP_UP";
    private static final String DEPOSIT_TYPE = "DEPOSIT";
    private static final String REFUND_TYPE = "REFUND";
    private static final String COMPLETED_STATUS = "Completed";
    private static final String PAID_STATUS = "Paid";
    private static final String BORROW_FEE_SETTING_KEY = "BORROW_FEE_PER_BOOK";
    private static final String DEPOSIT_SETTING_KEY = "Deposit_Amount";
    private static final BigDecimal DEFAULT_DEPOSIT_AMOUNT = BigDecimal.valueOf(50000);
    private static final int DEFAULT_BORROW_FEE_DAYS = 10;
    private static final BigDecimal DEFAULT_BORROW_FEE_PER_BOOK = BigDecimal.valueOf(5000);
    private static final int MEMBER_TRANSACTION_PAGE_SIZE = 10;
    private static final int LIBRARIAN_TRANSACTION_PAGE_SIZE = 12;

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final ReservationRepository reservationRepository;

    public FinancialServiceImpl(TransactionRepository transactionRepository,
                                WalletRepository walletRepository,
                                BorrowRepository borrowRepository,
                                BorrowDetailRepository borrowDetailRepository,
                                SystemSettingRepository systemSettingRepository,
                                MemberRepository memberRepository,
                                NotificationRepository notificationRepository,
                                MemberNotificationRepository memberNotificationRepository,
                                ReservationRepository reservationRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOverdueFine(Integer memberId, Integer fineId) {
        Transaction fine = transactionRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoản phạt với ID: " + fineId));

        validateTransactionOwner(fine, memberId);

        String type = normalize(fine.getTransactionType());
        if (!FINE_TYPE.equals(type) && !DAMAGE_FEE_TYPE.equals(type)) {
            throw new ValidationException("Giao dịch này không phải khoản phạt.");
        }

        if (isCompletedStatus(fine.getStatus())) {
            throw new ConflictException("Khoản phạt này đã được thanh toán.");
        }

        BigDecimal fineAmount = amountOrZero(fine.getAmount()).abs();
        if (fineAmount.signum() <= 0) {
            throw new ValidationException("Số tiền phạt không hợp lệ.");
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
            throw new ConflictException("Phí mượn của phiếu này đã được thanh toán.");
        }

        String borrowStatus = normalize(borrow.getStatus());
        if (!"ACTIVE".equals(borrowStatus) && !"BORROWING".equals(borrowStatus) && !"OVERDUE".equals(borrowStatus)) {
            throw new ConflictException("Chỉ có thể thanh toán phí cho phiếu mượn đã được duyệt.");
        }

        BigDecimal feeAmount = calculateBorrowingFeeAmount(borrowId);
        if (feeAmount.signum() <= 0) {
            throw new ValidationException("Phí mượn không hợp lệ.");
        }

        var wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của thành viên."));

        BigDecimal currentBalance = balanceOf(wallet.getBalance());
        ensureSufficientBalance(currentBalance, feeAmount, "phí mượn");

        wallet.setBalance(currentBalance.subtract(feeAmount));
        walletRepository.save(wallet);

        saveWalletTransaction(wallet, borrow, BORROW_FEE_TYPE, feeAmount.negate(), COMPLETED_STATUS);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBorrowingFeeAmount(Integer borrowId) {
        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        if (details == null || details.isEmpty()) {
            throw new ConflictException("Phiếu mượn không có chi tiết.");
        }

        BigDecimal perBookPerDay = getBorrowFeePerBookPerDay();
        return details.stream()
                .map(detail -> BigDecimal.valueOf(calculateBorrowDays(detail)).multiply(perBookPerDay))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPaidBorrowingFee(Integer memberId, Integer borrowId) {
        return transactionRepository.hasCompletedBorrowFee(memberId, borrowId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payReservationDeposit(Integer memberId, Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đặt trước với ID: " + reservationId));

        if (reservation.getMember() == null
                || reservation.getMember().getMemberId() == null
                || !reservation.getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException("Yêu cầu đặt trước không thuộc về thành viên hiện tại.");
        }

        String reservationStatus = normalize(reservation.getStatus());
        if ("DEPOSIT_PAID".equals(reservationStatus) || "PAID".equals(reservationStatus)) {
            throw new ConflictException("Tiền cọc cho yêu cầu đặt trước này đã được thanh toán.");
        }
        if ("COMPLETED".equals(reservationStatus) || "CANCELED".equals(reservationStatus)
                || "CANCELLED".equals(reservationStatus) || "REFUNDED".equals(reservationStatus)
                || "REFUND_PENDING".equals(reservationStatus)) {
            throw new ConflictException("Yêu cầu đặt trước này không thể thanh toán tiền cọc.");
        }

        Wallet wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của thành viên."));
        BigDecimal depositAmount = getReservationDepositAmount();
        BigDecimal currentBalance = balanceOf(wallet.getBalance());
        ensureSufficientBalance(currentBalance, depositAmount, "tiền cọc đặt trước");

        wallet.setBalance(currentBalance.subtract(depositAmount));
        walletRepository.save(wallet);

        saveWalletTransaction(wallet, null, DEPOSIT_TYPE, depositAmount.negate(), COMPLETED_STATUS);

        reservation.setStatus("Deposit_Paid");
        reservationRepository.save(reservation);

        createMemberNotification(
                reservation.getMember(),
                "Thanh toán tiền cọc thành công",
                "Thư viện đã ghi nhận tiền cọc đặt trước " + formatMoney(depositAmount)
                        + " cho sách \"" + (reservation.getBook() == null ? "" : reservation.getBook().getTitle()) + "\".");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistory(Integer memberId, int page, String type) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), MEMBER_TRANSACTION_PAGE_SIZE);
        if (type == null || type.trim().isEmpty()) {
            return transactionRepository.findByWalletMemberMemberIdOrderByTransactionDateDesc(memberId, pageable);
        }
        return transactionRepository
                .findByWalletMemberMemberIdAndTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
                        memberId, type.trim(), pageable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFine(Integer memberId, Double amount, String reason) {
        if (amount == null || amount <= 0) {
            throw new ValidationException("Số tiền phạt phải lớn hơn 0.");
        }

        var wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của thành viên."));

        Transaction transaction = saveWalletTransaction(
                wallet,
                null,
                FINE_TYPE,
                BigDecimal.valueOf(amount).abs().negate(),
                "Pending");

        Member member = wallet.getMember();
        createMemberNotification(
                member,
                "Phí phạt mới",
                "Thư viện đã ghi nhận khoản phạt " + formatMoney(transaction.getAmount().abs())
                        + ". Lý do: " + (reason == null || reason.isBlank() ? "Vi phạm quy định thư viện" : reason.trim())
                        + ".");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getAllTransactions(int page, String type) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), LIBRARIAN_TRANSACTION_PAGE_SIZE);
        if (type == null || type.trim().isEmpty()) {
            return transactionRepository.findAllByOrderByTransactionDateDesc(pageable);
        }
        return transactionRepository.findByTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
                type.trim(), pageable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topUpMemberAccount(String memberPhone, Double amount) {
        if (memberPhone == null || memberPhone.trim().isEmpty()) {
            throw new ValidationException("Vui lòng nhập email, số điện thoại hoặc ID thành viên.");
        }
        if (amount == null || amount <= 0) {
            throw new ValidationException("Số tiền nạp phải lớn hơn 0.");
        }

        Member member = findMemberByLookup(memberPhone.trim());
        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseGet(() -> createWalletForMember(member));

        BigDecimal topUpAmount = BigDecimal.valueOf(amount).abs();
        BigDecimal newBalance = balanceOf(wallet.getBalance()).add(topUpAmount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        saveWalletTransaction(wallet, null, TOP_UP_TYPE, topUpAmount, COMPLETED_STATUS);

        createMemberNotification(
                member,
                "Nạp tiền thành công",
                "Tài khoản ví của bạn vừa được nạp " + formatMoney(topUpAmount)
                        + ". Số dư hiện tại: " + formatMoney(newBalance) + ".");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestReservationDepositRefund(Integer memberId, Integer reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu đặt trước với ID: " + reservationId));

        if (reservation.getMember() == null
                || reservation.getMember().getMemberId() == null
                || !reservation.getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException("Phiếu đặt trước không thuộc về thành viên hiện tại.");
        }

        String reservationStatus = normalize(reservation.getStatus());
        if ("REFUND_PENDING".equals(reservationStatus)) {
            throw new ConflictException("Yêu cầu hoàn tiền đang chờ thủ thư duyệt.");
        }
        if ("REFUNDED".equals(reservationStatus)) {
            throw new ConflictException("Tiền cọc của phiếu này đã được hoàn trước đó.");
        }
        if (!"DEPOSIT_PAID".equals(reservationStatus)
                && !"ACTIVE".equals(reservationStatus)
                && !"READY".equals(reservationStatus)) {
            throw new ConflictException("Chỉ phiếu đã thanh toán tiền cọc mới được yêu cầu hoàn tiền.");
        }

        reservation.setStatus("Refund_Pending");
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundReservationDeposit(Integer memberId, Integer reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu đặt trước với ID: " + reservationId));

        if (reservation.getMember() == null
                || reservation.getMember().getMemberId() == null
                || !reservation.getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException("Phiếu đặt trước không thuộc về thành viên này.");
        }

        String reservationStatus = normalize(reservation.getStatus());
        if ("REFUNDED".equals(reservationStatus)) {
            throw new ConflictException("Tiền cọc của phiếu này đã được hoàn trước đó.");
        }
        if (!"REFUND_PENDING".equals(reservationStatus)) {
            throw new ConflictException("Chỉ có thể hoàn tiền cho yêu cầu đang chờ thủ thư duyệt.");
        }

        Wallet wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của thành viên."));
        BigDecimal refundAmount = getReservationDepositAmount();
        if (refundAmount.signum() <= 0) {
            throw new ValidationException("Số tiền hoàn cọc không hợp lệ.");
        }

        BigDecimal newBalance = balanceOf(wallet.getBalance()).add(refundAmount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
        saveWalletTransaction(wallet, null, REFUND_TYPE, refundAmount, COMPLETED_STATUS);

        reservation.setStatus("Refunded");
        reservationRepository.save(reservation);

        createMemberNotification(
                reservation.getMember(),
                "Hoàn tiền cọc thành công",
                "Thư viện đã hoàn " + formatMoney(refundAmount) + " vào ví cho phiếu đặt trước #"
                        + reservationId + ". Số dư hiện tại: " + formatMoney(newBalance) + ".");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getRefundableReservationDeposits(Integer memberId) {
        return reservationRepository.findByMemberMemberIdAndStatusInOrderByReservationDateDesc(
                memberId,
                List.of("Refund_Pending"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getPendingReservationDepositRefunds() {
        return reservationRepository.findByStatusInOrderByReservationDateAsc(List.of("Refund_Pending"));
    }

    private Borrow findBorrowForMember(Integer memberId, Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mượn với ID: " + borrowId));

        if (borrow.getMember() == null
                || borrow.getMember().getMemberId() == null
                || !borrow.getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException("Phiếu mượn không thuộc về thành viên hiện tại.");
        }

        return borrow;
    }

    private Member findMemberByLookup(String lookup) {
        if (lookup.matches("\\d+")) {
            try {
                Integer memberId = Integer.valueOf(lookup);
                return memberRepository.findById(memberId)
                        .or(() -> memberRepository.findByUserPhone(lookup))
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên với thông tin: " + lookup));
            } catch (NumberFormatException ignored) {
                return memberRepository.findByUserPhone(lookup)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên với thông tin: " + lookup));
            }
        }

        return memberRepository.findByUserPhone(lookup)
                .or(() -> memberRepository.findByUserEmail(lookup))
                .or(() -> memberRepository.findByAccountUsername(lookup))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên với thông tin: " + lookup));
    }

    private Wallet createWalletForMember(Member member) {
        Wallet wallet = new Wallet();
        wallet.setMember(member);
        wallet.setBalance(BigDecimal.ZERO);
        return walletRepository.save(wallet);
    }

    private Transaction saveWalletTransaction(Wallet wallet,
                                              Borrow borrow,
                                              String transactionType,
                                              BigDecimal amount,
                                              String status) {
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(borrow);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(status);
        return transactionRepository.save(transaction);
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
            return systemSettingRepository.findBySettingKeyIgnoreCase(BORROW_FEE_SETTING_KEY).stream()
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .filter(value -> value.signum() > 0)
                    .findFirst()
                    .orElse(DEFAULT_BORROW_FEE_PER_BOOK);
        } catch (NumberFormatException ignored) {
            return DEFAULT_BORROW_FEE_PER_BOOK;
        }
    }

    private int calculateBorrowDays(BorrowDetail detail) {
        LocalDateTime start = detail.getBorrow() == null ? null : detail.getBorrow().getBorrowDate();
        LocalDateTime end = detail.getDueDate();
        if (start == null || end == null || !end.isAfter(start)) {
            return DEFAULT_BORROW_FEE_DAYS;
        }

        long hours = Duration.between(start, end).toHours();
        long days = (hours + 23) / 24;
        return (int) Math.max(days, 1);
    }

    @Override
    public BigDecimal getReservationDepositAmount() {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(DEPOSIT_SETTING_KEY).stream()
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .filter(value -> value.signum() > 0)
                    .findFirst()
                    .orElse(DEFAULT_DEPOSIT_AMOUNT);
        } catch (NumberFormatException ignored) {
            return DEFAULT_DEPOSIT_AMOUNT;
        }
    }

    private void validateTransactionOwner(Transaction transaction, Integer memberId) {
        if (transaction.getWallet() == null
                || transaction.getWallet().getMember() == null
                || transaction.getWallet().getMember().getMemberId() == null
                || !transaction.getWallet().getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException("Giao dịch không thuộc về thành viên hiện tại.");
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
            throw new ConflictException("Số dư ví không đủ để thanh toán " + paymentName
                    + ". Số dư hiện tại: " + formatMoney(safeBalance)
                    + ", cần thanh toán: " + formatMoney(safeRequiredAmount) + ".");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
