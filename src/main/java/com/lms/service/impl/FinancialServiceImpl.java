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
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Financial transaction rules maintained by Pham Kien Quoc for member fees,
 * fines, deposits, top-ups, and librarian transaction history.
 */
@Service
public class FinancialServiceImpl implements FinancialService {

    @Autowired
    private LocalizedMessageService localizedMessageService = LocalizedMessageService.fallback();
    private static final String BORROW_FEE_TYPE = "BORROW_FEE";
    private static final String FINE_TYPE = "FINE";
    private static final String DAMAGE_FEE_TYPE = "DAMAGE_FEE";
    private static final String TOP_UP_TYPE = "TOP_UP";
    private static final String DEPOSIT_TYPE = "DEPOSIT";
    private static final String REFUND_TYPE = "REFUND";
    private static final String COMPLETED_STATUS = "Completed";
    private static final String PAID_STATUS = "Paid";
    private static final String PENDING_STATUS = "Pending";
    private static final String BORROW_FEE_SETTING_KEY = "BORROW_FEE_PER_BOOK";
    private static final String FINE_PER_DAY_SETTING_KEY = "Fine_Per_Day";
    private static final String DAMAGE_COMPENSATION_SETTING_KEY = "Damage_Compensation_Amount";
    private static final String DEPOSIT_SETTING_KEY = "Deposit_Amount";
    private static final BigDecimal DEFAULT_DEPOSIT_AMOUNT = BigDecimal.valueOf(50000);
    private static final BigDecimal DEFAULT_DAMAGE_COMPENSATION_AMOUNT = BigDecimal.valueOf(120000);
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
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.fineNotFound", fineId)));

        validateTransactionOwner(fine, memberId);

        String type = normalize(fine.getTransactionType());
        if (!FINE_TYPE.equals(type) && !DAMAGE_FEE_TYPE.equals(type)) {
            throw new ValidationException(localizedMessageService.get("backend.financial.notFineTransaction"));
        }

        if (isCompletedStatus(fine.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.financial.fineAlreadyPaid"));
        }

        BigDecimal fineAmount = amountOrZero(fine.getAmount()).abs();
        if (fineAmount.signum() <= 0) {
            throw new ValidationException(localizedMessageService.get("backend.financial.invalidFineAmount"));
        }

        var wallet = fine.getWallet();
        BigDecimal currentBalance = balanceOf(wallet.getBalance());
        ensureSufficientBalance(currentBalance, fineAmount, localizedMessageService.get("backend.financial.fineLabel"));
        wallet.setBalance(currentBalance.subtract(fineAmount));
        walletRepository.save(wallet);

        fine.setAmount(fineAmount.negate());
        fine.setTransactionDate(LocalDateTime.now());
        fine.setStatus(COMPLETED_STATUS);
        transactionRepository.save(fine);

        Member member = wallet.getMember();
        createMemberNotification(member,
                localizedMessageService.get("systemNotification.fine.walletPaid.title"),
                localizedMessageService.get("systemNotification.fine.walletPaid.content",
                        formatMoney(fineAmount), fineId, formatMoney(wallet.getBalance())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payBorrowingFee(Integer memberId, Integer borrowId) {
        Borrow borrow = findBorrowForMember(memberId, borrowId);

        if (hasPaidBorrowingFee(memberId, borrowId)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.borrowFeeAlreadyPaid"));
        }

        String borrowStatus = normalize(borrow.getStatus());
        if (!"ACTIVE".equals(borrowStatus) && !"BORROWING".equals(borrowStatus) && !"OVERDUE".equals(borrowStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.approvedLoanOnly"));
        }

        BigDecimal feeAmount = calculateBorrowingFeeAmount(borrowId);
        if (feeAmount.signum() <= 0) {
            throw new ValidationException(localizedMessageService.get("backend.financial.invalidBorrowFee"));
        }

        var wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.walletNotFound")));

        BigDecimal currentBalance = balanceOf(wallet.getBalance());
        ensureSufficientBalance(currentBalance, feeAmount, localizedMessageService.get("backend.financial.borrowFeeLabel"));

        wallet.setBalance(currentBalance.subtract(feeAmount));
        walletRepository.save(wallet);

        saveWalletTransaction(wallet, borrow, BORROW_FEE_TYPE, feeAmount.negate(), COMPLETED_STATUS);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateBorrowingFeeAmount(Integer borrowId) {
        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        if (details == null || details.isEmpty()) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.noDetails"));
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
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.reservationNotFound", reservationId)));

        if (reservation.getMember() == null
                || reservation.getMember().getMemberId() == null
                || !reservation.getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException(localizedMessageService.get("backend.financial.reservationOwnerMismatch"));
        }

        String reservationStatus = normalize(reservation.getStatus());
        if ("DEPOSIT_PAID".equals(reservationStatus) || "PAID".equals(reservationStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.depositAlreadyPaid"));
        }
        if ("COMPLETED".equals(reservationStatus) || "CANCELED".equals(reservationStatus)
                || "CANCELLED".equals(reservationStatus) || "REFUNDED".equals(reservationStatus)
                || "REFUND_PENDING".equals(reservationStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.depositNotPayable"));
        }

        Wallet wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.walletNotFound")));
        BigDecimal depositAmount = getReservationDepositAmount();
        BigDecimal currentBalance = balanceOf(wallet.getBalance());
        ensureSufficientBalance(currentBalance, depositAmount, localizedMessageService.get("backend.financial.depositLabel"));

        wallet.setBalance(currentBalance.subtract(depositAmount));
        walletRepository.save(wallet);

        saveWalletTransaction(wallet, null, DEPOSIT_TYPE, depositAmount.negate(), COMPLETED_STATUS);

        reservation.setStatus("Deposit_Paid");
        reservationRepository.save(reservation);

        createMemberNotification(
                reservation.getMember(),
                localizedMessageService.get("systemNotification.deposit.paid.title"),
                localizedMessageService.get("systemNotification.deposit.paid.content", formatMoney(depositAmount),
                        reservation.getBook() == null ? "" : reservation.getBook().getTitle()));
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
    public void issueOverdueFine(Integer borrowDetailId) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.financial.borrowDetailNotFound", borrowDetailId)));
        if (detail.getBorrow() == null || detail.getBorrow().getMember() == null || detail.getDueDate() == null) {
            throw new ValidationException(localizedMessageService.get("backend.financial.overdueFineDataInvalid"));
        }

        LocalDate chargedDate = detail.getReturnDate() == null
                ? LocalDate.now()
                : detail.getReturnDate().toLocalDate();
        LocalDate dueDate = detail.getDueDate().toLocalDate();
        long overdueDays = ChronoUnit.DAYS.between(dueDate, chargedDate);
        if (overdueDays <= 0) {
            return;
        }

        BigDecimal fineAmount = getFinePerDay().multiply(BigDecimal.valueOf(overdueDays));
        if (fineAmount.signum() <= 0) {
            return;
        }

        Borrow borrow = detail.getBorrow();
        Member member = borrow.getMember();
        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseGet(() -> createWalletForMember(member));
        Transaction transaction = transactionRepository
                .findFirstByBorrowBorrowIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDesc(
                        borrow.getBorrowId(), FINE_TYPE, PENDING_STATUS)
                .map(existing -> {
                    existing.setAmount(amountOrZero(existing.getAmount()).subtract(fineAmount));
                    return transactionRepository.save(existing);
                })
                .orElseGet(() -> saveWalletTransaction(
                        wallet, borrow, FINE_TYPE, fineAmount.negate(), PENDING_STATUS));

        String bookTitle = detail.getBook() == null || detail.getBook().getTitle() == null
                ? localizedMessageService.get("backend.book.unknownTitle")
                : detail.getBook().getTitle();
        createMemberNotification(member,
                localizedMessageService.get("systemNotification.overdueFine.title"),
                localizedMessageService.get("systemNotification.overdueFine.pendingContent",
                        formatMoney(fineAmount), bookTitle, overdueDays,
                        formatMoney(transaction.getAmount().abs())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueDamageCompensation(Integer borrowDetailId) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.financial.borrowDetailNotFound", borrowDetailId)));
        if (detail.getBorrow() == null || detail.getBorrow().getMember() == null) {
            throw new ValidationException(localizedMessageService.get("backend.financial.damageCompensationDataInvalid"));
        }

        BigDecimal compensationAmount = getDamageCompensationAmount();
        if (compensationAmount.signum() <= 0) {
            return;
        }

        Borrow borrow = detail.getBorrow();
        Member member = borrow.getMember();
        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseGet(() -> createWalletForMember(member));
        Transaction transaction = transactionRepository
                .findFirstByBorrowBorrowIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDesc(
                        borrow.getBorrowId(), DAMAGE_FEE_TYPE, PENDING_STATUS)
                .map(existing -> {
                    existing.setAmount(amountOrZero(existing.getAmount()).subtract(compensationAmount));
                    return transactionRepository.save(existing);
                })
                .orElseGet(() -> saveWalletTransaction(
                        wallet, borrow, DAMAGE_FEE_TYPE, compensationAmount.negate(), PENDING_STATUS));

        String bookTitle = detail.getBook() == null || detail.getBook().getTitle() == null
                ? localizedMessageService.get("backend.book.unknownTitle")
                : detail.getBook().getTitle();
        String reason = localizedMessageService.get("backend.financial.damageCompensationReason", bookTitle);
        createMemberNotification(member,
                localizedMessageService.get("systemNotification.fine.created.title"),
                localizedMessageService.get("systemNotification.fine.created.content",
                        formatMoney(compensationAmount), reason));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getPendingFines() {
        return transactionRepository.findAllPendingFineTransactions(List.of(FINE_TYPE, DAMAGE_FEE_TYPE));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payFineByCash(Integer fineId) {
        Transaction fine = transactionRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.financial.fineNotFound", fineId)));
        String type = normalize(fine.getTransactionType());
        if (!FINE_TYPE.equals(type) && !DAMAGE_FEE_TYPE.equals(type)) {
            throw new ValidationException(localizedMessageService.get("backend.financial.notFineTransaction"));
        }
        if (isCompletedStatus(fine.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.financial.fineAlreadyPaid"));
        }
        BigDecimal fineAmount = amountOrZero(fine.getAmount()).abs();
        if (fineAmount.signum() <= 0) {
            throw new ValidationException(localizedMessageService.get("backend.financial.invalidFineAmount"));
        }

        fine.setAmount(fineAmount.negate());
        fine.setStatus(COMPLETED_STATUS);
        fine.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(fine);

        Member member = fine.getWallet() == null ? null : fine.getWallet().getMember();
        createMemberNotification(member,
                localizedMessageService.get("systemNotification.fine.cashPaid.title"),
                localizedMessageService.get("systemNotification.fine.cashPaid.content",
                        formatMoney(fineAmount), fine.getTransactionId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payFineByWalletAtDesk(Integer fineId) {
        Transaction fine = transactionRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.financial.fineNotFound", fineId)));
        Member member = fine.getWallet() == null ? null : fine.getWallet().getMember();
        if (member == null || member.getMemberId() == null) {
            throw new ResourceNotFoundException(localizedMessageService.get("backend.financial.memberNotFound"));
        }

        payOverdueFine(member.getMemberId(), fineId);
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
            throw new ValidationException(localizedMessageService.get("backend.financial.memberLookupRequired"));
        }
        if (amount == null || amount <= 0) {
            throw new ValidationException(localizedMessageService.get("backend.financial.topupPositive"));
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
                localizedMessageService.get("systemNotification.topup.success.title"),
                localizedMessageService.get("systemNotification.topup.success.content", formatMoney(topUpAmount), formatMoney(newBalance)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestReservationDepositRefund(Integer memberId, Integer reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.reservationNotFound", reservationId)));

        if (reservation.getMember() == null
                || reservation.getMember().getMemberId() == null
                || !reservation.getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException(localizedMessageService.get("backend.financial.reservationOwnerMismatch"));
        }

        String reservationStatus = normalize(reservation.getStatus());
        if ("REFUND_PENDING".equals(reservationStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.refundAlreadyPending"));
        }
        if ("REFUNDED".equals(reservationStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.depositAlreadyRefunded"));
        }
        if (!"DEPOSIT_PAID".equals(reservationStatus)
                && !"ACTIVE".equals(reservationStatus)
                && !"READY".equals(reservationStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.refundRequiresPaidDeposit"));
        }

        reservation.setStatus("Refund_Pending");
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundReservationDeposit(Integer memberId, Integer reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.reservationNotFound", reservationId)));

        if (reservation.getMember() == null
                || reservation.getMember().getMemberId() == null
                || !reservation.getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException(localizedMessageService.get("backend.financial.reservationMemberMismatch"));
        }

        String reservationStatus = normalize(reservation.getStatus());
        if ("REFUNDED".equals(reservationStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.depositAlreadyRefunded"));
        }
        if (!"REFUND_PENDING".equals(reservationStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.refundPendingOnly"));
        }

        Wallet wallet = walletRepository.findByMemberMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.walletNotFound")));
        BigDecimal refundAmount = getReservationDepositAmount();
        if (refundAmount.signum() <= 0) {
            throw new ValidationException(localizedMessageService.get("backend.financial.invalidRefundAmount"));
        }

        BigDecimal newBalance = balanceOf(wallet.getBalance()).add(refundAmount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
        saveWalletTransaction(wallet, null, REFUND_TYPE, refundAmount, COMPLETED_STATUS);

        reservation.setStatus("Refunded");
        reservationRepository.save(reservation);

        createMemberNotification(
                reservation.getMember(),
                localizedMessageService.get("systemNotification.deposit.refunded.title"),
                localizedMessageService.get("systemNotification.deposit.refunded.content", formatMoney(refundAmount), reservationId, formatMoney(newBalance)));
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
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.notFoundById", borrowId)));

        if (borrow.getMember() == null
                || borrow.getMember().getMemberId() == null
                || !borrow.getMember().getMemberId().equals(memberId)) {
            throw new ForbiddenException(localizedMessageService.get("backend.financial.loanOwnerMismatch"));
        }

        return borrow;
    }

    private Member findMemberByLookup(String lookup) {
        if (lookup.matches("\\d+")) {
            try {
                Integer memberId = Integer.valueOf(lookup);
                return memberRepository.findById(memberId)
                        .or(() -> memberRepository.findByUserPhone(lookup))
                        .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.memberLookupNotFound", lookup)));
            } catch (NumberFormatException ignored) {
                return memberRepository.findByUserPhone(lookup)
                        .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.memberLookupNotFound", lookup)));
            }
        }

        return memberRepository.findByUserPhone(lookup)
                .or(() -> memberRepository.findByUserEmail(lookup))
                .or(() -> memberRepository.findByAccountUsername(lookup))
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.memberLookupNotFound", lookup)));
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
        return localizedMessageService.get("currency.vndAmount", String.format("%,.0f", safeAmount));
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

    private BigDecimal getFinePerDay() {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(FINE_PER_DAY_SETTING_KEY)
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .filter(value -> value.signum() >= 0)
                    .orElse(BigDecimal.valueOf(5000));
        } catch (NumberFormatException ignored) {
            return BigDecimal.valueOf(5000);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getDamageCompensationAmount() {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(DAMAGE_COMPENSATION_SETTING_KEY)
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .filter(value -> value.signum() > 0)
                    .orElse(DEFAULT_DAMAGE_COMPENSATION_AMOUNT);
        } catch (NumberFormatException ignored) {
            return DEFAULT_DAMAGE_COMPENSATION_AMOUNT;
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
            throw new ForbiddenException(localizedMessageService.get("backend.financial.transactionOwnerMismatch"));
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
            throw new ConflictException(localizedMessageService.get("backend.financial.insufficientBalance",
                    paymentName, formatMoney(safeBalance), formatMoney(safeRequiredAmount)));
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
