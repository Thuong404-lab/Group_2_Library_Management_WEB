package com.lms.service.impl;

import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.Notification;
import com.lms.entity.Reservation;
import com.lms.entity.SystemSetting;
import com.lms.entity.Staff;
import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import com.lms.enums.TransactionChannel;
import com.lms.enums.TransactionStatus;
import com.lms.enums.TransactionType;
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
import com.lms.service.TopUpPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

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
    private static final String PENDING_STATUS = "Pending";
    private static final String BORROW_FEE_SETTING_KEY = "BORROW_FEE_PER_BOOK";
    private static final String NEW_BOOK_OVERDUE_FINE_KEY = "New_Book_Overdue_Fine";
    private static final String MINOR_DAMAGE_OVERDUE_FINE_KEY = "Minor_Damage_Overdue_Fine";
    private static final String DAMAGE_COMPENSATION_SETTING_KEY = "Damage_Compensation_Amount";
    private static final String DEPOSIT_SETTING_KEY = "Deposit_Amount";
    private static final BigDecimal DEFAULT_DEPOSIT_AMOUNT = BigDecimal.valueOf(50000);
    private static final int DEFAULT_BORROW_FEE_DAYS = 10;
    private static final BigDecimal DEFAULT_BORROW_FEE_PER_BOOK = BigDecimal.valueOf(5000);
    private static final int MEMBER_TRANSACTION_PAGE_SIZE = 10;
    @Value("${library.transactions.page-size:12}")
    private int librarianTransactionPageSize = 12;

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
        Transaction fine = transactionRepository.findByIdForUpdate(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.fineNotFound", fineId)));

        validateTransactionOwner(fine, memberId);

        String type = normalize(fine.getTransactionType());
        if (!FINE_TYPE.equals(type) && !DAMAGE_FEE_TYPE.equals(type)) {
            throw new ValidationException(localizedMessageService.get("backend.financial.notFineTransaction"));
        }

        requirePendingFine(fine);

        BigDecimal fineAmount = amountOrZero(fine.getAmount()).abs();
        if (fineAmount.signum() <= 0) {
            throw new ValidationException(localizedMessageService.get("backend.financial.invalidFineAmount"));
        }

        var wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.walletNotFound")));
        BigDecimal currentBalance = balanceOf(wallet.getBalance());
        ensureSufficientBalance(currentBalance, fineAmount, localizedMessageService.get("backend.financial.fineLabel"));
        wallet.setBalance(currentBalance.subtract(fineAmount));
        walletRepository.save(wallet);

        fine.setAmount(fineAmount.negate());
        LocalDateTime paidAt = LocalDateTime.now();
        fine.setTransactionDate(paidAt);
        fine.setPaidAt(paidAt);
        fine.setStatus(COMPLETED_STATUS);
        fine.setChannel(TransactionChannel.WALLET.name());
        fine.setBalanceBefore(currentBalance);
        fine.setBalanceAfter(wallet.getBalance());
        transactionRepository.save(fine);

        Member member = wallet.getMember();
        createMemberNotification(
                member,
                NotificationType.FINANCE, NotificationEventType.FINE_PAID, NotificationSource.SYSTEM,
                "systemNotification.fine.walletPaid.title",
                "systemNotification.fine.walletPaid.content",
                fineAmount, fineId, wallet.getBalance());
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

        var wallet = walletRepository.findByMemberIdForUpdate(memberId)
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

        return details.stream()
                .map(detail -> BigDecimal.valueOf(calculateBorrowDays(detail))
                        .multiply(getBorrowFeeForCondition(detail.getBookItem())))
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
        if (!"PENDING".equals(reservationStatus)) {
            throw new ConflictException(localizedMessageService.get("backend.financial.depositNotPayable"));
        }

        Wallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
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
                NotificationType.RESERVATION, NotificationEventType.RESERVATION_DEPOSIT_PAID, NotificationSource.SYSTEM,
                "systemNotification.deposit.paid.title",
                "systemNotification.deposit.paid.content",
                depositAmount, reservation.getBook() == null ? "" : reservation.getBook().getTitle());
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

        BigDecimal fineAmount = getOverdueFinePerDay(detail).multiply(BigDecimal.valueOf(overdueDays));
        if (fineAmount.signum() <= 0) {
            return;
        }

        Borrow borrow = detail.getBorrow();
        Member member = borrow.getMember();
        Wallet wallet = walletRepository.findByMemberIdForUpdate(member.getMemberId())
                .orElseGet(() -> createWalletForMember(member));
        var existingPending = transactionRepository
                .findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
                        detail.getBorrowDetailId(), FINE_TYPE, PENDING_STATUS);
        Transaction transaction;
        if (existingPending.isPresent()) {
            transaction = existingPending.get();
            transaction.setAmount(fineAmount.negate());
            transaction = transactionRepository.save(transaction);
        } else if (transactionRepository.countByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCase(
                detail.getBorrowDetailId(), FINE_TYPE) > 0) {
            return;
        } else {
            transaction = saveBorrowDetailTransaction(
                    wallet, borrow, detail, FINE_TYPE, fineAmount.negate(), PENDING_STATUS);
        }

        String bookTitle = detail.getBook() == null || detail.getBook().getTitle() == null
                ? localizedMessageService.get("backend.book.unknownTitle")
                : detail.getBook().getTitle();
        createMemberNotification(
                member,
                NotificationType.FINANCE, NotificationEventType.OVERDUE_FINE_CREATED, NotificationSource.SYSTEM,
                "systemNotification.overdueFine.title",
                "systemNotification.overdueFine.pendingContent",
                fineAmount, bookTitle, overdueDays, transaction.getAmount().abs());
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
        Wallet wallet = walletRepository.findByMemberIdForUpdate(member.getMemberId())
                .orElseGet(() -> createWalletForMember(member));
        var existingPending = transactionRepository
                .findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
                        detail.getBorrowDetailId(), DAMAGE_FEE_TYPE, PENDING_STATUS);
        Transaction transaction;
        if (existingPending.isPresent()) {
            transaction = existingPending.get();
            transaction.setAmount(compensationAmount.negate());
            transaction = transactionRepository.save(transaction);
        } else if (transactionRepository.countByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCase(
                detail.getBorrowDetailId(), DAMAGE_FEE_TYPE) > 0) {
            return;
        } else {
            transaction = saveBorrowDetailTransaction(
                    wallet, borrow, detail, DAMAGE_FEE_TYPE, compensationAmount.negate(), PENDING_STATUS);
        }

        String bookTitle = detail.getBook() == null || detail.getBook().getTitle() == null
                ? localizedMessageService.get("backend.book.unknownTitle")
                : detail.getBook().getTitle();
        String reason = localizedMessageService.get("backend.financial.damageCompensationReason", bookTitle);
        createMemberNotification(
                member,
                NotificationType.FINANCE, NotificationEventType.FINE_CREATED, NotificationSource.LIBRARIAN,
                "systemNotification.fine.created.title",
                "systemNotification.fine.created.content",
                compensationAmount, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getPendingFines() {
        return transactionRepository.findAllPendingFineTransactions(List.of(FINE_TYPE, DAMAGE_FEE_TYPE));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payFineByCash(Integer fineId, Staff performedBy) {
        if (performedBy == null || performedBy.getStaffId() == null) {
            throw new ValidationException(localizedMessageService.get("backend.financial.staffRequired"));
        }
        Transaction fine = transactionRepository.findByIdForUpdate(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.financial.fineNotFound", fineId)));
        String type = normalize(fine.getTransactionType());
        if (!FINE_TYPE.equals(type) && !DAMAGE_FEE_TYPE.equals(type)) {
            throw new ValidationException(localizedMessageService.get("backend.financial.notFineTransaction"));
        }
        requirePendingFine(fine);
        BigDecimal fineAmount = amountOrZero(fine.getAmount()).abs();
        if (fineAmount.signum() <= 0) {
            throw new ValidationException(localizedMessageService.get("backend.financial.invalidFineAmount"));
        }

        fine.setAmount(fineAmount.negate());
        LocalDateTime paidAt = LocalDateTime.now();
        fine.setStatus(COMPLETED_STATUS);
        fine.setTransactionDate(paidAt);
        fine.setPaidAt(paidAt);
        fine.setChannel(TransactionChannel.CASH.name());
        fine.setPerformedByStaff(performedBy);
        fine.setReferenceCode("CASH-FINE-" + fine.getTransactionId());
        transactionRepository.save(fine);

        Member member = fine.getWallet() == null ? null : fine.getWallet().getMember();
        createMemberNotification(
                member,
                NotificationType.FINANCE, NotificationEventType.FINE_PAID, NotificationSource.LIBRARIAN,
                "systemNotification.fine.cashPaid.title",
                "systemNotification.fine.cashPaid.content",
                fineAmount, fine.getTransactionId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payFineByWalletAtDesk(Integer fineId, Staff performedBy) {
        if (performedBy == null || performedBy.getStaffId() == null) {
            throw new ValidationException(localizedMessageService.get("backend.financial.staffRequired"));
        }
        Transaction fine = transactionRepository.findByIdForUpdate(fineId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.financial.fineNotFound", fineId)));
        Member member = fine.getWallet() == null ? null : fine.getWallet().getMember();
        if (member == null || member.getMemberId() == null) {
            throw new ResourceNotFoundException(localizedMessageService.get("backend.financial.memberNotFound"));
        }

        payOverdueFine(member.getMemberId(), fineId);
        fine.setPerformedByStaff(performedBy);
        transactionRepository.save(fine);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getAllTransactions(int page, String query, String type,
            String status, String channel, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ValidationException(localizedMessageService.get("backend.transaction.invalidDateRange"));
        }
        String normalizedType = normalizeTransactionType(type);
        String normalizedStatus = normalizeTransactionStatus(status);
        String normalizedChannel = normalizeTransactionChannel(channel);
        String normalizedQuery = query == null ? "" : query.trim();
        TransactionSearchIds searchIds = extractTransactionSearchIds(normalizedQuery);
        int pageSize = Math.max(5, Math.min(librarianTransactionPageSize, 100));
        PageRequest pageable = PageRequest.of(Math.max(page, 0), pageSize);
        return transactionRepository.searchForLibrarian(
                normalizedQuery, searchIds.transactionId(), searchIds.memberId(),
                normalizedType, normalizedStatus, normalizedChannel,
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay(), pageable);
    }

    private String normalizeTransactionType(String type) {
        if (type == null || type.isBlank()) return null;
        return TransactionType.fromCode(type).map(Enum::name)
                .orElseThrow(() -> new ValidationException(localizedMessageService.get("backend.transaction.invalidType")));
    }

    private String normalizeTransactionStatus(String status) {
        if (status == null || status.isBlank()) return null;
        return TransactionStatus.fromValue(status).map(value -> value.getDatabaseValue().toUpperCase())
                .orElseThrow(() -> new ValidationException(localizedMessageService.get("backend.transaction.invalidStatus")));
    }

    private String normalizeTransactionChannel(String channel) {
        if (channel == null || channel.isBlank()) return null;
        return TransactionChannel.fromCode(channel).map(Enum::name)
                .orElseThrow(() -> new ValidationException(localizedMessageService.get("backend.transaction.invalidChannel")));
    }

    private TransactionSearchIds extractTransactionSearchIds(String query) {
        if (query == null || query.isBlank()) return new TransactionSearchIds(null, null);
        String normalized = query.trim().toUpperCase(java.util.Locale.ROOT);
        boolean transactionCode = normalized.matches("^#?TXN-\\d+$");
        boolean memberCode = normalized.matches("^(TV-|MEM-)\\d+$");
        String digits = normalized.replaceFirst("^(#?TXN-|TV-|MEM-)", "");
        if (!digits.matches("\\d+")) return new TransactionSearchIds(null, null);
        try {
            Integer value = Integer.valueOf(digits);
            if (transactionCode) return new TransactionSearchIds(value, null);
            if (memberCode) return new TransactionSearchIds(null, value);
            return new TransactionSearchIds(value, value);
        } catch (NumberFormatException ignored) {
            return new TransactionSearchIds(null, null);
        }
    }

    private record TransactionSearchIds(Integer transactionId, Integer memberId) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topUpMemberAccount(String memberLookup, BigDecimal amount, String requestId, Staff performedBy) {
        if (memberLookup == null || memberLookup.trim().isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.financial.memberLookupRequired"));
        }
        if (!isValidRequestId(requestId)) {
            throw new ValidationException(localizedMessageService.get("backend.financial.invalidRequest"));
        }
        if (performedBy == null || performedBy.getStaffId() == null) {
            throw new ValidationException(localizedMessageService.get("backend.financial.staffRequired"));
        }
        String referenceCode = "CASH-TOPUP-" + requestId.trim();
        if (transactionRepository.existsByReferenceCode(referenceCode)) return;

        BigDecimal topUpAmount = requireTopUpAmount(amount);

        Member member = findMemberByLookup(memberLookup.trim());
        Wallet wallet = walletRepository.findByMemberIdForUpdate(member.getMemberId())
                .orElseGet(() -> createWalletForMember(member));
        if (transactionRepository.existsByReferenceCode(referenceCode)) return;

        BigDecimal oldBalance = balanceOf(wallet.getBalance());
        BigDecimal newBalance = oldBalance.add(topUpAmount);
        if (newBalance.compareTo(TopUpPolicy.MAX_WALLET_BALANCE) > 0) {
            throw new ValidationException(localizedMessageService.get("backend.financial.walletLimit", TopUpPolicy.MAX_WALLET_BALANCE));
        }
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction transaction = saveWalletTransaction(wallet, null, TOP_UP_TYPE, topUpAmount, COMPLETED_STATUS);
        transaction.setReferenceCode(referenceCode);
        transaction.setPerformedByStaff(performedBy);
        transaction.setChannel("CASH");
        transaction.setBalanceBefore(oldBalance);
        transaction.setBalanceAfter(newBalance);
        transactionRepository.save(transaction);

        createMemberNotification(
                member,
                NotificationType.FINANCE, NotificationEventType.TOP_UP_SUCCESS, NotificationSource.LIBRARIAN,
                "systemNotification.topup.success.title",
                "systemNotification.topup.success.content",
                topUpAmount, newBalance);
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

        Wallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
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
                NotificationType.RESERVATION, NotificationEventType.RESERVATION_REFUNDED, NotificationSource.LIBRARIAN,
                "systemNotification.deposit.refunded.title",
                "systemNotification.deposit.refunded.content",
                refundAmount, reservationId, newBalance);
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

    private BigDecimal requireTopUpAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException(localizedMessageService.get("backend.financial.topupPositive"));
        }
        BigDecimal value = amount.stripTrailingZeros();
        if (value.scale() > 0 || value.compareTo(TopUpPolicy.MIN_AMOUNT) < 0
                || value.compareTo(TopUpPolicy.MAX_AMOUNT) > 0) {
            throw new ValidationException(localizedMessageService.get("backend.payment.amountRange"));
        }
        return value;
    }

    private boolean isValidRequestId(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 48) {
            return false;
        }
        try {
            UUID.fromString(requestId.trim());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
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

    private Transaction saveBorrowDetailTransaction(Wallet wallet,
                                                    Borrow borrow,
                                                    BorrowDetail detail,
                                                    String transactionType,
                                                    BigDecimal amount,
                                                    String status) {
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(borrow);
        transaction.setBorrowDetail(detail);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(status);
        transaction.setChannel("SYSTEM");
        return transactionRepository.save(transaction);
    }

    private void createMemberNotification(Member member,
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
        localizedMessageService.prepareNotification(notification, titleKey, contentKey, arguments);
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

    private BigDecimal getBorrowFeeForCondition(com.lms.entity.BookItem item) {
        String condition = item == null || item.getBookCondition() == null
                ? ""
                : item.getBookCondition().trim().toLowerCase(java.util.Locale.ROOT);
        if (condition.contains("severely")) {
            return BigDecimal.ZERO;
        }
        if (condition.contains("minor")) {
            return getMoneySetting("MINOR_DAMAGE_BORROW_FEE", BigDecimal.valueOf(4000));
        }
        return getBorrowFeePerBookPerDay();
    }

    private BigDecimal getMoneySetting(String key, BigDecimal defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .filter(value -> value.signum() >= 0)
                    .orElse(defaultValue);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private BigDecimal getOverdueFinePerDay(BorrowDetail detail) {
        boolean minorDamage = detail.getBookItem() != null
                && detail.getBookItem().getBookCondition() != null
                && detail.getBookItem().getBookCondition().toLowerCase(java.util.Locale.ROOT).contains("minor");
        String key = minorDamage ? MINOR_DAMAGE_OVERDUE_FINE_KEY : NEW_BOOK_OVERDUE_FINE_KEY;
        BigDecimal fallbackBorrowFee = minorDamage
                ? getMoneySetting("MINOR_DAMAGE_BORROW_FEE", BigDecimal.valueOf(4000))
                : getBorrowFeePerBookPerDay();
        return getMoneySetting(key, fallbackBorrowFee.multiply(BigDecimal.valueOf(2)));
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
                    .orElseThrow(() -> new ValidationException(localizedMessageService.get("backend.financial.damageRateNotConfigured")));
        } catch (NumberFormatException ignored) {
            throw new ValidationException(localizedMessageService.get("backend.financial.damageRateNotConfigured"));
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

    private void requirePendingFine(Transaction fine) {
        if (!PENDING_STATUS.equalsIgnoreCase(fine.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.financial.fineNotPending"));
        }
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
