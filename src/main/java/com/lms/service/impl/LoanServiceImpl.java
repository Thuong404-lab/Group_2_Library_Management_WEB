package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import com.lms.exception.ConflictException;
import com.lms.exception.DataProcessingException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.*;
import com.lms.service.LoanService;
import com.lms.service.LocalizedMessageService;
import com.lms.service.BookItemConditionPolicy;
import com.lms.util.BorrowCodeFormatter;
import com.lms.service.FinancialService;
import com.lms.service.MembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * LoanServiceImpl - Xá»­ lÃ½ Logic Quáº£n lÃ½ Phiáº¿u mÆ°á»£n (Thá»§ thÆ°)
 * NgÆ°á»i phá»¥ trÃ¡ch: Huá»³nh Gia HÆ°ng (CE190488)
 */
@Service
public class LoanServiceImpl implements LoanService {

    @Autowired
    private LocalizedMessageService localizedMessageService = LocalizedMessageService.fallback();

    private static final String STATUS_BORROWED = "Borrowed";
    private static final String STATUS_AVAILABLE = "Available";
    private static final String STATUS_UNAVAILABLE = "Unavailable";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_RETURNED = "Returned";
    private static final String STATUS_OVERDUE = "Overdue";
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final MemberRepository memberRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final BookItemRepository bookItemRepository;
    private final BookRepository bookRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ReservationRepository reservationRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final FinancialService financialService;
    private final MembershipService membershipService;

    public LoanServiceImpl(BorrowRepository borrowRepository,
                           BorrowDetailRepository borrowDetailRepository,
                           MemberRepository memberRepository,
                           StaffAccountRepository staffAccountRepository,
                           BookItemRepository bookItemRepository,
                           BookRepository bookRepository,
                           MemberAccountRepository memberAccountRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           ReservationRepository reservationRepository,
                           SystemSettingRepository systemSettingRepository,
                           NotificationRepository notificationRepository,
                           MemberNotificationRepository memberNotificationRepository,
                           FinancialService financialService,
                           MembershipService membershipService) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.memberRepository = memberRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.bookItemRepository = bookItemRepository;
        this.bookRepository = bookRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.reservationRepository = reservationRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.financialService = financialService;
        this.membershipService = membershipService;
    }

    // UC-13.1: Xem chi tiáº¿t phiáº¿u mÆ°á»£n
    @Override
    @Transactional(readOnly = true)
    public Borrow getLoanDetails(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.notFoundById", borrowId)));
    }

    // UC-13.2: XÃ¡c nháº­n tráº£ sÃ¡ch trá»±c tiáº¿p báº±ng quÃ©t mÃ£ váº¡ch
    // (Barcode) táº¡i quáº§y
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.barcodeNotFound", barcode)));

        BorrowDetail activeDetail = borrowDetailRepository.findFirstByBookItem_BookItemIdAndStatusInIgnoreCase(
                        item.getBookItemId(), List.of("Borrowed", "Overdue", "Return_Pending", "Renew_Pending"))
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.activeHistoryNotFound")));

        if ("Renew_Pending".equalsIgnoreCase(activeDetail.getStatus())) {
            rejectRenewal(activeDetail.getBorrowDetailId(), "SYSTEM", "RETURNED_BEFORE_APPROVAL", null);
        }

        // 1. Cáº­p nháº­t tráº¡ng thÃ¡i sÃ¡ch váº­t lÃ½
        item.setStatus(BookItemConditionPolicy.circulationStatus(item.getBookCondition()));
        bookItemRepository.save(item);

        // 2. TÃ­nh toÃ¡n pháº¡t quÃ¡ háº¡n náº¿u cÃ³
        processOverdueFine(activeDetail);

        // 3. Cáº­p nháº­t tráº¡ng thÃ¡i chi tiáº¿t mÆ°á»£n
        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus(STATUS_RETURNED);
        borrowDetailRepository.save(activeDetail);

        // 4. Kiá»ƒm tra vÃ  cáº­p nháº­t tráº¡ng thÃ¡i phiáº¿u mÆ°á»£n cha
        updateParentBorrowStatus(activeDetail.getBorrow());
    }

    // PhÃª duyá»‡t yÃªu cáº§u tráº£ sÃ¡ch trá»±c tuyáº¿n tá»« Ä‘á»™c giáº£
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOnlineReturn(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.returnRequestNotFound")));

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);

        for (BorrowDetail detail : details) {
            if ("Return_Pending".equalsIgnoreCase(detail.getStatus())) {
                // 1. Cáº­p nháº­t tráº¡ng thÃ¡i sÃ¡ch váº­t lÃ½ vá» Available
                if (detail.getBookItem() != null) {
                    BookItem item = detail.getBookItem();
                    item.setStatus(BookItemConditionPolicy.circulationStatus(item.getBookCondition()));
                    bookItemRepository.save(item);
                }

                // 2. TÃ­nh toÃ¡n pháº¡t quÃ¡ háº¡n náº¿u cÃ³
                processOverdueFine(detail);

                // 3. Cáº­p nháº­t tráº¡ng thÃ¡i chi tiáº¿t mÆ°á»£n
                detail.setReturnDate(LocalDateTime.now());
                detail.setStatus(STATUS_RETURNED);
                borrowDetailRepository.save(detail);
            }
        }

        // 4. Cáº­p nháº­t tráº¡ng thÃ¡i phiáº¿u mÆ°á»£n cha
        updateParentBorrowStatus(borrow);

        // 5. Gá»­i thÃ´ng bÃ¡o Ä‘áº¿n Ä‘á»™c giáº£
        sendInternalNotification(borrow.getMember(),
                NotificationType.LOAN, NotificationEventType.RETURN_CONFIRMED, NotificationSource.LIBRARIAN,
                "systemNotification.return.approved.title",
                "systemNotification.return.approved.content", BorrowCodeFormatter.format(borrowId));
    }

    // UC-13.3: Quáº§y mÆ°á»£n sÃ¡ch
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowDesk(String memberIdentifier, List<String> barcodes, String staffUsername) {
        Member member;
        try {
            if (memberIdentifier != null && memberIdentifier.contains("@")) {
                member = memberRepository.findByUserEmail(memberIdentifier)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                localizedMessageService.get("backend.loan.memberEmailNotFound")));
            } else {
                member = memberRepository.findByUserPhone(memberIdentifier)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                localizedMessageService.get("backend.loan.memberPhoneNotFound")));
            }
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            throw new DataProcessingException(localizedMessageService.get("backend.loan.duplicateMemberData"), e);
        }

        Staff staff = staffAccountRepository.findByUsername(staffUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.librarianNotFound")));

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : barcodes) {
            String trimmedBarcode = barcode.trim();
            if (trimmedBarcode.isEmpty())
                continue;

            BookItem item;
            try {
                item = bookItemRepository.findByBarcode(trimmedBarcode)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                localizedMessageService.get("backend.loan.barcodeNotFound", trimmedBarcode)));
            } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                throw new DataProcessingException(
                        localizedMessageService.get("backend.loan.duplicateBarcode", trimmedBarcode), e);
            }

            if (!STATUS_AVAILABLE.equalsIgnoreCase(item.getStatus())
                    || !BookItemConditionPolicy.isLendable(item.getBookCondition())) {
                throw new ConflictException(
                        localizedMessageService.get("backend.loan.barcodeUnavailable", trimmedBarcode));
            }
            bookItemsToBorrow.add(item);
        }

        if (bookItemsToBorrow.isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.barcode.noneValid"));
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        if (member.getTier() == null || member.getTier().getBorrowLimit() == null) {
            throw new DataProcessingException(localizedMessageService.get("backend.tier.memberTierMissing"));
        }
        int maxLimit = member.getTier().getBorrowLimit();
        int totalRequestedBooks = (int) currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new ConflictException(
                    localizedMessageService.get("backend.loan.limitExceeded", maxLimit, currentBorrowCount));
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setStaff(staff);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus(STATUS_ACTIVE);
        borrow = borrowRepository.save(borrow);

        for (BookItem item : bookItemsToBorrow) {
            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(item.getBook());
            detail.setBookItem(item);
            detail.setDueDate(LocalDateTime.now().plusDays(14));
            detail.setStatus(STATUS_BORROWED);
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            item.setStatus(STATUS_BORROWED);
            bookItemRepository.save(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowRequest(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.requestNotFound")));

        borrow.setStatus(STATUS_ACTIVE);
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus(STATUS_BORROWED);
            borrowDetailRepository.save(detail);

            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                if (!BookItemConditionPolicy.isLendable(item.getBookCondition())) {
                    throw new ConflictException(
                            localizedMessageService.get("backend.loan.barcodeUnavailable", item.getBarcode()));
                }
                item.setStatus(STATUS_BORROWED);
                bookItemRepository.save(item);
            }
        }
    }

    // UC-13.4: Gia háº¡n mÆ°á»£n
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRenewal(Integer borrowDetailId) {
        // ... kept for compatibility if needed, but not used by member anymore ...
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.detailNotFound")));

        if (!STATUS_BORROWED.equalsIgnoreCase(detail.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.loan.renewBorrowedOnly"));
        }

        // Äá»c cáº¥u hÃ¬nh sá»‘ láº§n gia háº¡n tá»‘i Ä‘a (máº·c Ä‘á»‹nh lÃ  2)
        int maxRenewals = 2;
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey("MAX_RENEWALS");
        if (setting.isPresent()) {
            try {
                maxRenewals = Integer.parseInt(setting.get().getSettingValue());
            } catch (NumberFormatException ignored) {
                /* Use the documented default. */ }
        }

        if (detail.getRenewCount() >= maxRenewals) {
            throw new ConflictException(localizedMessageService.get("backend.loan.maxRenewals", maxRenewals));
        }

        // Äá»c cáº¥u hÃ¬nh sá»‘ ngÃ y gia háº¡n thÃªm má»—i láº§n (máº·c Ä‘á»‹nh lÃ 
        // 7 ngÃ y)
        int renewDays = 7;
        Optional<SystemSetting> renewDaysSetting = systemSettingRepository.findBySettingKey("RENEW_DAYS");
        if (renewDaysSetting.isPresent()) {
            try {
                renewDays = Integer.parseInt(renewDaysSetting.get().getSettingValue());
            } catch (NumberFormatException ignored) {
                /* Use the documented default. */ }
        }

        detail.setDueDate(detail.getDueDate().plusDays(renewDays));
        detail.setRenewCount(detail.getRenewCount() + 1);
        borrowDetailRepository.save(detail);

        Member member = detail.getBorrow().getMember();
        if (member != null && member.getMemberId() != null) {
            sendInternalNotification(member,
                    NotificationType.LOAN, NotificationEventType.RENEWAL_APPROVED, NotificationSource.LIBRARIAN,
                    "systemNotification.renewal.success.title",
                    "systemNotification.renewal.success.content",
                    detail.getBook().getTitle(), renewDays,
                    detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
    }

    // ThÃªm 3 phÆ°Æ¡ng thá»©c nÃ y vÃ o thÃ¢n lá»›p LoanServiceImpl.java

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> findActiveLoansByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return borrowDetailRepository.findActiveLoansByBarcode(barcode.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturnWithDetails(String barcode, LocalDateTime returnDate, String bookCondition,
            String damageNote, String staffUsername) {
        confirmReturnWithDetails(barcode, returnDate, bookCondition, damageNote, BigDecimal.ZERO, "cash",
                staffUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturnWithDetails(String barcode, LocalDateTime returnDate, String bookCondition,
            String damageNote, BigDecimal damageFine, String staffUsername) {
        confirmReturnWithDetails(barcode, returnDate, bookCondition, damageNote, damageFine, "cash", staffUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Transaction confirmReturnWithDetails(String barcode, LocalDateTime returnDate, String bookCondition,
            String damageNote, BigDecimal damageFine, String paymentMethod, String staffUsername) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.return.invalidBarcode"));
        }
        if (returnDate == null || !returnDate.toLocalDate().equals(java.time.LocalDate.now())) {
            throw new ValidationException(localizedMessageService.get("backend.return.invalidReturnDate"));
        }
        String resolvedPaymentMethod = paymentMethod == null ? "cash"
                : paymentMethod.trim().toLowerCase(java.util.Locale.ROOT);
        if (!List.of("cash", "wallet", "bank").contains(resolvedPaymentMethod)) {
            throw new ValidationException(localizedMessageService.get("backend.return.invalidPaymentMethod"));
        }
        if (!isGoodCondition(bookCondition)
                && (damageNote == null || damageNote.trim().isEmpty())) {
            throw new ValidationException(localizedMessageService.get("backend.return.damageDescriptionRequired"));
        }
        List<BorrowDetail> activeLoans = borrowDetailRepository.findActiveLoansByBarcodeForUpdate(barcode.trim());
        if (activeLoans.isEmpty()) {
            throw new ResourceNotFoundException(
                    localizedMessageService.get("backend.loan.unreturnedBarcodeNotFound", barcode));
        }

        BorrowDetail detail = activeLoans.get(0);
        if (detail.getBorrow() != null && detail.getBorrow().getBorrowDate() != null
                && returnDate.isBefore(detail.getBorrow().getBorrowDate())) {
            throw new ValidationException(localizedMessageService.get("backend.return.beforeBorrowDate"));
        }
        BookItem item = detail.getBookItem();
        if (item != null && getConditionLevel(bookCondition) < getConditionLevel(item.getBookCondition())) {
            throw new ValidationException(localizedMessageService.get(
                    "backend.return.conditionCannotImprove",
                    item.getBookCondition(),
                    bookCondition));
        }
        if (damageFine != null && damageFine.compareTo(BigDecimal.ZERO) > 0
                && "wallet".equals(resolvedPaymentMethod)) {
            Member member = detail.getBorrow() == null ? null : detail.getBorrow().getMember();
            BigDecimal balance = member == null || member.getMemberId() == null
                    ? BigDecimal.ZERO
                    : walletRepository.findByMemberMemberId(member.getMemberId())
                            .map(Wallet::getBalance)
                            .orElse(BigDecimal.ZERO);
            if (balance == null || balance.compareTo(damageFine) < 0) {
                throw new ConflictException(localizedMessageService.get("backend.financial.insufficientBalanceSimple"));
            }
        }
        if ("Renew_Pending".equalsIgnoreCase(detail.getStatus())) {
            rejectRenewal(detail.getBorrowDetailId(), "SYSTEM", "RETURNED_BEFORE_APPROVAL", null);
        }
        boolean requiresCompensation = requiresDamageCompensation(bookCondition);

        if (item != null) {
            item.setStatus(resolveReturnedItemStatus(bookCondition));
            item.setBookCondition(
                    bookCondition != null && !bookCondition.trim().isEmpty() ? bookCondition.trim() : "Tốt");
            item.setDamageNote(damageNote != null && !damageNote.trim().isEmpty() ? damageNote.trim() : null);
            bookItemRepository.save(item);
        }

        String fullConditionNote = bookCondition != null ? bookCondition.trim() : "Tốt";
        if (damageNote != null && !damageNote.trim().isEmpty()) {
            fullConditionNote += " - " + damageNote.trim();
        }
        detail.setReturnDate(returnDate);
        detail.setConditionNote(fullConditionNote);
        detail.setConditionCode(resolveConditionCode(bookCondition));
        detail.setStatus(STATUS_RETURNED);
        borrowDetailRepository.save(detail);

        if (detail.getDueDate() != null && returnDate.isAfter(detail.getDueDate())) {
            processOverdueFine(detail);
        }
        if (requiresCompensation) {
            financialService.issueDamageCompensation(detail.getBorrowDetailId());
        }

        Transaction damageFineTx = null;
        if (damageFine != null && damageFine.compareTo(BigDecimal.ZERO) > 0) {
            damageFineTx = processDamageFine(detail, damageFine, resolvedPaymentMethod, staffUsername);
        }

        updateParentBorrowStatus(detail.getBorrow());
        sendInternalNotification(detail.getBorrow().getMember(),
                NotificationType.LOAN, NotificationEventType.RETURN_CONFIRMED, NotificationSource.LIBRARIAN,
                "systemNotification.return.desk.title",
                "systemNotification.return.desk.content", detail.getBook().getTitle());

        // Tự động phân gán cho độc giả đứng đầu hàng đợi Đặt trước (FIFO) nếu bản sao ở trạng thái Available
        if (item != null && "Available".equalsIgnoreCase(item.getStatus()) && detail.getBook() != null && detail.getBook().getBookId() != null) {
            Integer bookId = detail.getBook().getBookId();
            List<Reservation> waitingReservations = reservationRepository
                    .findByBook_BookIdAndStatusInOrderByReservationDateAsc(bookId, List.of("Deposit_Paid", "Pending"));
            if (!waitingReservations.isEmpty()) {
                Reservation nextReservation = waitingReservations.get(0);
                nextReservation.setStatus("Ready");
                reservationRepository.save(nextReservation);

                item.setStatus("Waiting_Pickup");
                bookItemRepository.save(item);

                sendInternalNotification(nextReservation.getMember(),
                        NotificationType.RESERVATION, NotificationEventType.RESERVATION_APPROVED, NotificationSource.SYSTEM,
                        "systemNotification.reservation.ready.title",
                        "systemNotification.reservation.ready.content",
                        nextReservation.getBook() != null ? nextReservation.getBook().getTitle() : "");
            }
        }

        return damageFineTx;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowDetail recoverMissingBookItem(Integer borrowDetailId, String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.barcode.required"));
        }
        BorrowDetail detail = borrowDetailRepository.findByIdForUpdate(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.detailNotFound")));
        if (!List.of(STATUS_BORROWED, STATUS_OVERDUE, "Return_Pending", "Renew_Pending").stream()
                .anyMatch(status -> status.equalsIgnoreCase(detail.getStatus()))) {
            throw new ConflictException(localizedMessageService.get("backend.return.recoveryInactiveLoan"));
        }
        if (detail.getBookItem() != null) {
            throw new ConflictException(localizedMessageService.get("backend.return.recoveryAlreadyAssigned",
                    detail.getBookItem().getBarcode()));
        }

        BookItem item = bookItemRepository.findByBarcodeForUpdate(barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.barcodeNotFound", barcode.trim())));
        if (detail.getBook() == null || item.getBook() == null
                || !detail.getBook().getBookId().equals(item.getBook().getBookId())) {
            throw new ConflictException(localizedMessageService.get("backend.return.recoveryWrongBook", barcode.trim(),
                    detail.getBook() == null ? "-" : detail.getBook().getTitle()));
        }
        if (borrowDetailRepository.findActiveLoansByBarcode(item.getBarcode()).stream()
                .anyMatch(active -> !active.getBorrowDetailId().equals(detail.getBorrowDetailId()))) {
            throw new ConflictException(
                    localizedMessageService.get("backend.return.recoveryBarcodeInUse", item.getBarcode()));
        }
        if (!STATUS_AVAILABLE.equalsIgnoreCase(item.getStatus())
                && !STATUS_BORROWED.equalsIgnoreCase(item.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.return.recoveryInvalidItemStatus",
                    item.getBarcode(), item.getStatus()));
        }

        detail.setBookItem(item);
        item.setStatus(STATUS_BORROWED);
        bookItemRepository.save(item);
        return borrowDetailRepository.save(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Transaction confirmBatchReturnWithDetails(List<String> barcodes, LocalDateTime returnDate,
            String bookCondition, String damageNote, BigDecimal damageFine, String paymentMethod,
            String staffUsername) {
        if (barcodes == null || barcodes.isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.return.invalidBarcodes"));
        }
        if (barcodes.size() > 1 && !isGoodCondition(bookCondition)) {
            throw new ValidationException(localizedMessageService.get("backend.return.minorDamageSingleOnly"));
        }
        LinkedHashMap<String, String> uniqueBarcodes = new LinkedHashMap<>();
        for (String barcode : barcodes) {
            String normalized = barcode == null ? "" : barcode.trim();
            if (normalized.isEmpty()) {
                throw new ValidationException(localizedMessageService.get("backend.return.invalidBarcode"));
            }
            String key = normalized.toUpperCase(java.util.Locale.ROOT);
            if (uniqueBarcodes.putIfAbsent(key, normalized) != null) {
                throw new ValidationException(localizedMessageService.get("backend.return.duplicateBarcodes"));
            }
        }
        Transaction firstTx = null;
        boolean isFirst = true;
        for (String barcode : uniqueBarcodes.values()) {
            BigDecimal fineForThis = isFirst ? damageFine : BigDecimal.ZERO;
            Transaction tx = confirmReturnWithDetails(barcode, returnDate, bookCondition, damageNote, fineForThis,
                    paymentMethod, staffUsername);
            if (isFirst) {
                firstTx = tx;
            }
            isFirst = false;
        }
        return firstTx;
    }

    private Transaction processDamageFine(BorrowDetail detail, BigDecimal damageFine, String paymentMethod,
            String staffUsername) {
        Member member = detail.getBorrow().getMember();
        if (member == null) {
            return null;
        }
        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setMember(member);
                    newWallet.setBalance(BigDecimal.ZERO);
                    return walletRepository.save(newWallet);
                });

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(detail.getBorrow());
        transaction.setBorrowDetail(detail);
        transaction.setTransactionType("DAMAGE_FEE");
        transaction.setTransactionDate(LocalDateTime.now());

        String resolvedMethod = (paymentMethod != null) ? paymentMethod.trim().toLowerCase() : "cash";
        transaction.setChannel("wallet".equals(resolvedMethod) ? "WALLET"
                : "cash".equals(resolvedMethod) ? "CASH" : "PAYOS");
        staffAccountRepository.findByUsername(staffUsername)
                .map(StaffAccount::getStaff)
                .ifPresent(transaction::setPerformedByStaff);

        if ("wallet".equals(resolvedMethod)) {
            // Trừ tiền trực tiếp vào ví
            BigDecimal currentBalance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
            if (currentBalance.compareTo(damageFine) < 0) {
                throw new ConflictException(localizedMessageService.get("backend.financial.insufficientBalanceSimple"));
            }
            wallet.setBalance(currentBalance.subtract(damageFine));
            walletRepository.save(wallet);

            transaction.setAmount(damageFine.abs().negate());
            transaction.setStatus("Completed");
        } else if ("cash".equals(resolvedMethod)) {
            // Thanh toán tiền mặt tại quầy (không trừ ví, giao dịch hoàn thành ngay)
            transaction.setAmount(damageFine.abs().negate());
            transaction.setStatus("Completed");
        } else {
            // Thanh toán qua ngân hàng (chờ quét mã QR)
            transaction.setAmount(damageFine.abs().negate());
            transaction.setStatus("Pending");
        }

        Transaction savedTx = transactionRepository.save(transaction);

        // Gửi thông báo hệ thống đến độc giả
        String formattedMoney = formatMoney(damageFine);
        if ("wallet".equals(resolvedMethod)) {
            sendInternalNotification(member,
                    NotificationType.FINANCE, NotificationEventType.FINE_PAID, NotificationSource.LIBRARIAN,
                    "systemNotification.damageFine.paidWallet.title",
                    "systemNotification.damageFine.paidWallet.content",
                    formattedMoney, detail.getBook().getTitle());
        } else if ("cash".equals(resolvedMethod)) {
            sendInternalNotification(member,
                    NotificationType.FINANCE, NotificationEventType.FINE_PAID, NotificationSource.LIBRARIAN,
                    "systemNotification.damageFine.paidCash.title",
                    "systemNotification.damageFine.paidCash.content",
                    formattedMoney, detail.getBook().getTitle());
        } else {
            sendInternalNotification(member,
                    NotificationType.FINANCE, NotificationEventType.FINE_CREATED, NotificationSource.LIBRARIAN,
                    "systemNotification.damageFine.created.title",
                    "systemNotification.damageFine.created.content",
                    formattedMoney, detail.getBook().getTitle());
        }

        return savedTx;
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return localizedMessageService.get("currency.vndAmount", String.format("%,.0f", safeAmount));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> searchActiveLoansByQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cleanQuery = query.trim();

        List<BorrowDetail> results = new ArrayList<>();

        // 1. Try to search by Email or Phone number of a member
        Optional<Member> memberOpt = memberRepository.findByUserEmail(cleanQuery);
        if (!memberOpt.isPresent()) {
            memberOpt = memberRepository.findByUserPhone(cleanQuery);
        }

        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            // Get all current borrow details of this member
            List<BorrowDetail> allMemberDetails = borrowDetailRepository
                    .findCurrentBorrowsByMemberId(member.getMemberId());
            for (BorrowDetail d : allMemberDetails) {
                if ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus())
                        || "Renew_Pending".equalsIgnoreCase(d.getStatus())) {
                    results.add(d);
                }
            }
            return results;
        }

        // 2. Otherwise, treat as Barcode or BorrowId
        List<Borrow> matchingBorrows = new ArrayList<>();

        // Loan codes use one canonical format only: BOR-{borrowId}.
        if (cleanQuery.matches("^BOR-[1-9]\\d*$")) {
            String borrowIdStr = cleanQuery.substring(4);
            try {
                Integer id = Integer.parseInt(borrowIdStr);
                Optional<Borrow> borrowOpt = borrowRepository.findById(id);
                borrowOpt.ifPresent(matchingBorrows::add);
            } catch (NumberFormatException ignored) {
                // An identifier larger than Integer.MAX_VALUE cannot be a valid borrow ID.
            }
        }

        // Check if query is barcode
        List<BorrowDetail> barcodeDetails = borrowDetailRepository.findActiveLoansByBarcode(cleanQuery);
        for (BorrowDetail bd : barcodeDetails) {
            if (bd.getBorrow() != null && !matchingBorrows.contains(bd.getBorrow())) {
                matchingBorrows.add(bd.getBorrow());
            }
        }

        // Collect all active details for matching borrows
        for (Borrow b : matchingBorrows) {
            List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(b.getBorrowId());
            for (BorrowDetail d : details) {
                if ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus())
                        || "Renew_Pending".equalsIgnoreCase(d.getStatus())) {
                    if (!results.contains(d)) {
                        results.add(d);
                    }
                }
            }
        }

        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmCollection(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException(
                        localizedMessageService.get("backend.loan.notFoundById", borrowId)));

        if (!"Approved".equalsIgnoreCase(borrow.getStatus())) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.notWaitingPickup"));
        }

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        if (details.isEmpty() || details.stream().anyMatch(d -> d.getBookItem() == null
                || d.getBookItem().getBarcode() == null || d.getBookItem().getBarcode().isBlank())) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.barcodeRequiredBeforePickup"));
        }

        borrow.setStatus(STATUS_ACTIVE);
        borrow.setBorrowDate(LocalDateTime.now());
        borrowRepository.save(borrow);

        for (BorrowDetail detail : details) {
            detail.setStatus(STATUS_BORROWED);
            detail.setDueDate(LocalDateTime.now().plusDays(14));
            borrowDetailRepository.save(detail);

            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                if (!BookItemConditionPolicy.isLendable(item.getBookCondition())) {
                    throw new ConflictException(
                            localizedMessageService.get("backend.loan.barcodeUnavailable", item.getBarcode()));
                }
                item.setStatus(STATUS_BORROWED);
                bookItemRepository.save(item);
            }
        }

        sendInternalNotification(borrow.getMember(),
                NotificationType.LOAN, NotificationEventType.LOAN_COLLECTED, NotificationSource.LIBRARIAN,
                "systemNotification.borrow.pickup.title",
                "systemNotification.borrow.collection.content", BorrowCodeFormatter.format(borrowId),
                LocalDateTime.now().plusDays(14)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getTodayReturnedBooks() {
        LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = java.time.LocalDate.now().atTime(java.time.LocalTime.MAX);
        return borrowDetailRepository.findReturnedBooksToday(startOfDay, endOfDay);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = ConflictException.class)
    public void approveRenewal(Integer borrowDetailId, String staffUsername) {
        BorrowDetail detail = borrowDetailRepository.findByIdForUpdate(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.detailNotFound")));
        if (!"Renew_Pending".equalsIgnoreCase(detail.getStatus()))
            throw new ConflictException(localizedMessageService.get("backend.loan.renewalNotPending"));
        Transaction hold = findPendingRenewalHold(borrowDetailId);
        // Renewal limits were validated when the immutable wallet hold was created.
        // Pending requests keep that policy snapshot even if an administrator changes
        // settings later.
        if (detail.getDueDate() == null || !detail.getDueDate().isAfter(LocalDateTime.now())) {
            rejectRenewal(borrowDetailId, "SYSTEM", "APPROVAL_EXPIRED", null);
            throw new ConflictException(localizedMessageService.get("backend.renewal.autoRejectedExpired"));
        }
        MemberAccount account = memberAccountRepository
                .findByMemberMemberId(detail.getBorrow().getMember().getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.account.memberNotFound")));
        if (!"Active".equalsIgnoreCase(account.getStatus())) {
            rejectRenewal(borrowDetailId, "SYSTEM", "ACCOUNT_RESTRICTED", null);
            throw new ConflictException(localizedMessageService.get("backend.renewal.autoRejectedAccountInactive"));
        }
        if (detail.getBook() != null && detail.getBorrow() != null && detail.getBorrow().getMember() != null) {
            Integer bookId = detail.getBook().getBookId();
            bookRepository.findByIdForUpdate(bookId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException(localizedMessageService.get("backend.book.notFound")));
            long waitingReservations = reservationRepository.countActiveReservationsByOtherMemberForBook(
                    bookId, detail.getBorrow().getMember().getMemberId(),
                    List.of("PENDING", "DEPOSIT_PAID", "READY", "ACTIVE"));
            long availableCopies = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, STATUS_AVAILABLE);
            if (waitingReservations > availableCopies) {
                rejectRenewal(borrowDetailId, "SYSTEM", "RESERVED_BY_OTHER", null);
                throw new ConflictException(localizedMessageService.get("backend.renewal.autoRejectedReserved"));
            }
        }
        if (hold.getRenewalDays() == null || hold.getRenewalDays() < 1) {
            rejectRenewal(borrowDetailId, "SYSTEM", "OTHER", "Invalid stored renewal request.");
            throw new ConflictException(localizedMessageService.get("backend.renewal.autoRejectedInvalidSnapshot"));
        }

        detail.setDueDate(detail.getDueDate().plusDays(hold.getRenewalDays()));
        detail.setRenewCount((detail.getRenewCount() == null ? 0 : detail.getRenewCount()) + 1);
        detail.setStatus(STATUS_BORROWED);
        hold.setStatus("Completed");
        staffAccountRepository.findByUsername(staffUsername)
                .map(StaffAccount::getStaff)
                .ifPresent(hold::setPerformedByStaff);
        borrowDetailRepository.save(detail);
        transactionRepository.save(hold);
        if (detail.getBorrow().getMember() != null) {
            membershipService.synchronizeMemberTier(detail.getBorrow().getMember().getMemberId());
            sendInternalNotification(detail.getBorrow().getMember(),
                    NotificationType.LOAN, NotificationEventType.RENEWAL_APPROVED, NotificationSource.LIBRARIAN,
                    "systemNotification.renewal.approved.title", "systemNotification.renewal.approved.content",
                    detail.getBook().getTitle(),
                    detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRenewal(Integer borrowDetailId, String staffUsername, String reasonCode, String reason) {
        var rejection = com.lms.util.RejectionReasonValidator.validate("RENEWAL", reasonCode, reason);
        BorrowDetail detail = borrowDetailRepository.findByIdForUpdate(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.detailNotFound")));
        if (!"Renew_Pending".equalsIgnoreCase(detail.getStatus()))
            throw new ConflictException(localizedMessageService.get("backend.loan.renewalNotPending"));
        Transaction hold = findPendingRenewalHold(borrowDetailId);
        Wallet wallet = walletRepository.findByMemberIdForUpdate(detail.getBorrow().getMember().getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.financial.walletNotFound")));
        BigDecimal refund = hold.getAmount() == null ? BigDecimal.ZERO : hold.getAmount().abs();
        BigDecimal balanceBeforeRefund = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        wallet.setBalance(balanceBeforeRefund.add(refund));
        hold.setStatus("Refunded");
        Transaction refundTransaction = new Transaction();
        refundTransaction.setWallet(wallet);
        refundTransaction.setBorrow(detail.getBorrow());
        refundTransaction.setBorrowDetail(detail);
        refundTransaction.setRenewalDays(hold.getRenewalDays());
        refundTransaction.setTransactionType("REFUND");
        refundTransaction.setAmount(refund);
        refundTransaction.setTransactionDate(LocalDateTime.now());
        refundTransaction.setStatus("Completed");
        refundTransaction.setChannel("WALLET");
        refundTransaction.setBalanceBefore(balanceBeforeRefund);
        refundTransaction.setBalanceAfter(wallet.getBalance());
        if (!"SYSTEM".equalsIgnoreCase(staffUsername)) {
            staffAccountRepository.findByUsername(staffUsername)
                    .map(StaffAccount::getStaff)
                    .ifPresent(refundTransaction::setPerformedByStaff);
        }
        detail.setStatus(
                detail.getDueDate() != null && detail.getDueDate().isBefore(LocalDateTime.now()) ? STATUS_OVERDUE
                        : STATUS_BORROWED);
        detail.setRejectionCode(rejection.code());
        detail.setRejectionReason(rejection.detail());
        walletRepository.save(wallet);
        transactionRepository.save(hold);
        transactionRepository.save(refundTransaction);
        borrowDetailRepository.save(detail);

        if (detail.getBorrow().getMember() != null) {
            boolean systemDecision = "SYSTEM".equalsIgnoreCase(staffUsername);
            boolean autoExpired = "APPROVAL_EXPIRED".equalsIgnoreCase(rejection.code());
            boolean hasDetail = rejection.detail() != null;
            String contentKey = autoExpired
                    ? (hasDetail ? "systemNotification.renewal.expired.contentWithReason"
                            : "systemNotification.renewal.expired.contentWithoutDetail")
                    : (hasDetail ? "systemNotification.renewal.refunded.contentWithReason"
                            : "systemNotification.renewal.refunded.contentWithoutDetail");
            Object translatedReason = localizedMessageService.messageArgument("rejection.code." + rejection.code());
            if (hasDetail) {
                sendInternalNotification(detail.getBorrow().getMember(),
                        NotificationType.FINANCE, NotificationEventType.RENEWAL_REJECTED,
                        systemDecision ? NotificationSource.SYSTEM : NotificationSource.LIBRARIAN,
                        autoExpired ? "systemNotification.renewal.expired.title"
                                : "systemNotification.renewal.refunded.title",
                        contentKey, detail.getBook().getTitle(), refund, wallet.getBalance(), translatedReason,
                        rejection.detail());
            } else {
                sendInternalNotification(detail.getBorrow().getMember(),
                        NotificationType.FINANCE, NotificationEventType.RENEWAL_REJECTED,
                        systemDecision ? NotificationSource.SYSTEM : NotificationSource.LIBRARIAN,
                        autoExpired ? "systemNotification.renewal.expired.title"
                                : "systemNotification.renewal.refunded.title",
                        contentKey, detail.getBook().getTitle(), refund, wallet.getBalance(), translatedReason);
            }
        }
    }

    private Transaction findPendingRenewalHold(Integer borrowDetailId) {
        return transactionRepository
                .findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
                        borrowDetailId, "RENEWAL_FEE", "Pending")
                .orElseThrow(() -> new ConflictException(localizedMessageService.get("backend.renewal.holdNotFound")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getAllPendingRenewals() {
        return borrowDetailRepository.findByStatusOrderByDueDateAsc("Renew_Pending");
    }

    @Override
    public java.util.List<com.lms.entity.BorrowDetail> getAllBorrowDetails() {
        return borrowDetailRepository.findAllBorrowDetailsWithRelationships();
    }
    // --- Private Helper Methods ---

    /**
     * Chuyá»ƒn viá»‡c phÃ¡t sinh khoáº£n pháº¡t quÃ¡ háº¡n sang module tÃ i chÃ­nh.
     * Khoáº£n pháº¡t chá»‰ Ä‘Æ°á»£c ghi nháº­n lÃ  cÃ´ng ná»£; vÃ­ chá»‰ thay
     * Ä‘á»•i khi thÃ nh viÃªn thanh toÃ¡n.
     */
    private void processOverdueFine(BorrowDetail detail) {
        if (detail != null && detail.getBorrowDetailId() != null) {
            financialService.issueOverdueFine(detail.getBorrowDetailId());
        }
    }

    private boolean requiresDamageCompensation(String bookCondition) {
        int threshold = 3;
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey("Damage_Compensation_Threshold");
        if (setting.isPresent()) {
            try {
                threshold = Integer.parseInt(setting.get().getSettingValue());
            } catch (NumberFormatException ignored) {
            }
        }
        int conditionLevel = getConditionLevel(bookCondition);
        return conditionLevel >= threshold;
    }

    private int getConditionLevel(String bookCondition) {
        String normalized = bookCondition == null ? "" : bookCondition.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("mất sách") || normalized.contains("lost")) {
            return 4;
        }
        if (normalized.contains("hư hỏng nặng") || normalized.contains("severe damage")) {
            return 3;
        }
        if (normalized.contains("hư hỏng nhẹ") || normalized.contains("minor damage") || normalized.contains("hư hỏng")) {
            return 2;
        }
        return 1;
    }

    private boolean isGoodCondition(String bookCondition) {
        String normalized = bookCondition == null ? "" : bookCondition.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("tốt") || normalized.startsWith("good") || normalized.startsWith("new")
                || normalized.startsWith("mới");
    }

    private String resolveReturnedItemStatus(String bookCondition) {
        String normalized = bookCondition == null ? "" : bookCondition.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("mất sách") || normalized.contains("lost")) {
            return STATUS_UNAVAILABLE;
        }
        return BookItemConditionPolicy.circulationStatus(bookCondition);
    }

    private String resolveConditionCode(String bookCondition) {
        return switch (getConditionLevel(bookCondition)) {
            case 4 -> "LOST";
            case 3 -> "DAMAGED";
            case 2 -> "MINOR_DAMAGE";
            default -> "GOOD";
        };
    }

    /**
     * Cáº­p nháº­t tráº¡ng thÃ¡i cá»§a phiáº¿u mÆ°á»£n cha dá»±a trÃªn cÃ¡c chi
     * tiáº¿t sÃ¡ch Ä‘Ã£ tráº£
     */
    private void updateParentBorrowStatus(Borrow borrow) {
        List<BorrowDetail> allDetails = borrowDetailRepository.findByBorrowId(borrow.getBorrowId());
        boolean allReturned = true;
        boolean hasOverdue = false;

        for (BorrowDetail d : allDetails) {
            if (!STATUS_RETURNED.equalsIgnoreCase(d.getStatus())) {
                allReturned = false;
            }
            if (STATUS_OVERDUE.equalsIgnoreCase(d.getStatus())) {
                hasOverdue = true;
            }
        }

        if (allReturned) {
            borrow.setStatus(STATUS_RETURNED);
        } else if (hasOverdue) {
            borrow.setStatus(STATUS_OVERDUE);
        } else {
            borrow.setStatus(STATUS_ACTIVE);
        }

        borrowRepository.save(borrow);
    }

    @Autowired(required = false)
    private com.lms.service.EmailService emailService;

    /**
     * Tạo thông báo hệ thống và gửi cho độc giả (kèm gửi Email nếu khả dụng)
     */
    private void sendInternalNotification(Member member,
            NotificationType type,
            NotificationEventType eventType,
            NotificationSource source,
            String titleKey,
            String contentKey,
            Object... arguments) {
        if (member == null || member.getMemberId() == null) {
            throw new DataProcessingException(localizedMessageService.get("backend.notification.memberMissing"));
        }
        Notification notif = new Notification();
        localizedMessageService.prepareNotification(notif, titleKey, contentKey, arguments);
        notif.setNotificationType(type);
        notif.setEventType(eventType);
        notif.setNotificationSource(source);
        notif.setCreatedDate(LocalDateTime.now());
        notif.setStatus(STATUS_ACTIVE);
        Notification saved = notificationRepository.save(notif);

        MemberNotification mn = new MemberNotification();
        mn.setId(new MemberNotificationId(member.getMemberId(), saved.getNotificationId()));
        mn.setMember(member);
        mn.setNotification(saved);
        mn.setIsRead(false);
        memberNotificationRepository.save(mn);

        if (emailService != null) {
            String recipientEmail = (member.getUser() != null) ? member.getUser().getEmail() : null;
            String recipientName = (member.getUser() != null) ? member.getUser().getFullName() : "Độc giả";
            if (recipientEmail != null && !recipientEmail.trim().isEmpty()) {
                final String to = recipientEmail.trim();
                final String name = recipientName;
                final String title = notif.getTitle();
                final String content = notif.getContent();
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendNotificationEmail(to, name, title, content);
                    } catch (Exception ignored) {
                        // Safe fallback if SMTP email server is unconfigured or unavailable
                    }
                });
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getMemberBorrowDetailsLimit365Days(Integer memberId) {
        java.time.LocalDateTime limitDate = java.time.LocalDateTime.now().minusDays(365);
        return borrowDetailRepository.findBorrowHistoryLimit365Days(memberId, limitDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getMemberTransactionsLimit365Days(Integer memberId) {
        java.time.LocalDateTime limitDate = java.time.LocalDateTime.now().minusDays(365);
        return transactionRepository.findTransactionsByMemberIdLimit365Days(memberId, limitDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getMemberBorrowDetailsByDateRange(Integer memberId, java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate) {
        return borrowDetailRepository.findBorrowHistoryByDateRange(memberId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getMemberTransactionsByDateRange(Integer memberId, java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate) {
        return transactionRepository.findTransactionsByMemberIdAndDateRange(memberId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowSchedule(String borrowDate, String returnDate, String keyword) {
        List<BorrowDetail> all = borrowDetailRepository.findAll();
        return all.stream().filter(d -> {
            boolean match = true;
            if (keyword != null && !keyword.isEmpty()) {
                match = (d.getBook() != null && d.getBook().getTitle().toLowerCase().contains(keyword.toLowerCase()))
                        || (d.getBookItem() != null && d.getBookItem().getBarcode().contains(keyword));
            }
            return match;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBookReturn(Integer borrowDetailId, String conditionNote) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(
                        () -> new IllegalArgumentException(localizedMessageService.get("backend.loan.detailNotFound")));

        BookItem item = detail.getBookItem();
        if (item == null) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.loan.physicalCopyMissing"));
        }
        String barcode = item.getBarcode();
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.loan.physicalBarcodeMissing"));
        }
        confirmReturnWithDetails(barcode, LocalDateTime.now(), conditionNote, null, "admin");
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId) {
        return borrowDetailRepository.findByBorrowId(borrowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByBorrowId(Integer borrowId) {
        return transactionRepository.findByBorrow_BorrowId(borrowId);
    }
}
