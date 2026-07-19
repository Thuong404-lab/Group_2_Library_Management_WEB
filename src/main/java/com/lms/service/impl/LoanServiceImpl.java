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
import com.lms.util.BorrowCodeFormatter;
import com.lms.service.FinancialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private static final String STATUS_DAMAGED = "Damaged";
    private static final String STATUS_LOST = "Lost";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_RETURNED = "Returned";
    private static final String STATUS_OVERDUE = "Overdue";
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final MemberRepository memberRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final BookItemRepository bookItemRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final FinancialService financialService;


    public LoanServiceImpl(BorrowRepository borrowRepository,
                           BorrowDetailRepository borrowDetailRepository,
                           MemberRepository memberRepository,
                           StaffAccountRepository staffAccountRepository,
                           BookItemRepository bookItemRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           SystemSettingRepository systemSettingRepository,
                           NotificationRepository notificationRepository,
                           MemberNotificationRepository memberNotificationRepository,
                           FinancialService financialService) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.memberRepository = memberRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.bookItemRepository = bookItemRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.financialService = financialService;
    }

    // UC-13.1: Xem chi tiáº¿t phiáº¿u mÆ°á»£n
    @Override
    @Transactional(readOnly = true)
    public Borrow getLoanDetails(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.notFoundById", borrowId)));
    }

    // UC-13.2: XÃ¡c nháº­n tráº£ sÃ¡ch trá»±c tiáº¿p báº±ng quÃ©t mÃ£ váº¡ch (Barcode) táº¡i quáº§y
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReturn(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.barcodeNotFound", barcode)));

        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && (STATUS_BORROWED.equalsIgnoreCase(d.getStatus()) || STATUS_OVERDUE.equalsIgnoreCase(d.getStatus()) || "Return_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.activeHistoryNotFound")));

        // 1. Cáº­p nháº­t tráº¡ng thÃ¡i sÃ¡ch váº­t lÃ½
        item.setStatus(STATUS_AVAILABLE);
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
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.returnRequestNotFound")));

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        
        for (BorrowDetail detail : details) {
            if ("Return_Pending".equalsIgnoreCase(detail.getStatus()) || STATUS_BORROWED.equalsIgnoreCase(detail.getStatus()) || STATUS_OVERDUE.equalsIgnoreCase(detail.getStatus())) {
                // 1. Cáº­p nháº­t tráº¡ng thÃ¡i sÃ¡ch váº­t lÃ½ vá» Available
                if (detail.getBookItem() != null) {
                    BookItem item = detail.getBookItem();
                    item.setStatus(STATUS_AVAILABLE);
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
                        .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.memberEmailNotFound")));
            } else {
                member = memberRepository.findByUserPhone(memberIdentifier)
                        .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.memberPhoneNotFound")));
            }
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            throw new DataProcessingException(localizedMessageService.get("backend.loan.duplicateMemberData"), e);
        }

        Staff staff = staffAccountRepository.findByUsername(staffUsername)
                .map(StaffAccount::getStaff)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.librarianNotFound")));

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : barcodes) {
            String trimmedBarcode = barcode.trim();
            if (trimmedBarcode.isEmpty()) continue;

            BookItem item;
            try {
                item = bookItemRepository.findByBarcode(trimmedBarcode)
                        .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.barcodeNotFound", trimmedBarcode)));
            } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                throw new DataProcessingException(localizedMessageService.get("backend.loan.duplicateBarcode", trimmedBarcode), e);
            }

            if (!STATUS_AVAILABLE.equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException(localizedMessageService.get("backend.loan.barcodeUnavailable", trimmedBarcode));
            }
            bookItemsToBorrow.add(item);
        }

        if (bookItemsToBorrow.isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.barcode.noneValid"));
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        int totalRequestedBooks = (int) currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new ConflictException(localizedMessageService.get("backend.loan.limitExceeded", maxLimit, currentBorrowCount));
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
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.requestNotFound")));
        
        borrow.setStatus(STATUS_ACTIVE);
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus(STATUS_BORROWED);
            borrowDetailRepository.save(detail);
            
            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
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
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.detailNotFound")));

        if (!STATUS_BORROWED.equalsIgnoreCase(detail.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.loan.renewBorrowedOnly"));
        }

        // Äá»c cáº¥u hÃ¬nh sá»‘ láº§n gia háº¡n tá»‘i Ä‘a (máº·c Ä‘á»‹nh lÃ  2)
        int maxRenewals = 2;
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey("MAX_RENEWALS");
        if (setting.isPresent()) {
            try {
                maxRenewals = Integer.parseInt(setting.get().getSettingValue());
            } catch (NumberFormatException ignored) { /* Use the documented default. */ }
        }

        if (detail.getRenewCount() >= maxRenewals) {
            throw new ConflictException(localizedMessageService.get("backend.loan.maxRenewals", maxRenewals));
        }

        // Äá»c cáº¥u hÃ¬nh sá»‘ ngÃ y gia háº¡n thÃªm má»—i láº§n (máº·c Ä‘á»‹nh lÃ  7 ngÃ y)
        int renewDays = 7;
        Optional<SystemSetting> renewDaysSetting = systemSettingRepository.findBySettingKey("RENEW_DAYS");
        if (renewDaysSetting.isPresent()) {
            try {
                renewDays = Integer.parseInt(renewDaysSetting.get().getSettingValue());
            } catch (NumberFormatException ignored) { /* Use the documented default. */ }
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
    public void confirmReturnWithDetails(String barcode, LocalDateTime returnDate, String bookCondition, String damageNote, String staffUsername) {
        List<BorrowDetail> activeLoans = borrowDetailRepository.findActiveLoansByBarcode(barcode.trim());
        if (activeLoans.isEmpty()) {
            throw new ResourceNotFoundException(localizedMessageService.get("backend.loan.unreturnedBarcodeNotFound", barcode));
        }

        BorrowDetail detail = activeLoans.get(0);
        BookItem item = detail.getBookItem();
        boolean requiresCompensation = requiresDamageCompensation(bookCondition);

        if (item != null) {
            item.setStatus(resolveReturnedItemStatus(bookCondition));
            item.setBookCondition(bookCondition != null && !bookCondition.trim().isEmpty() ? bookCondition.trim() : "Tá»‘t");
            item.setDamageNote(damageNote != null && !damageNote.trim().isEmpty() ? damageNote.trim() : null);
            bookItemRepository.save(item);
        }

        String fullConditionNote = bookCondition != null ? bookCondition.trim() : "Tá»‘t";
        if (damageNote != null && !damageNote.trim().isEmpty()) {
            fullConditionNote += " - " + damageNote.trim();
        }
        detail.setReturnDate(returnDate);
        detail.setConditionNote(fullConditionNote);
        detail.setStatus(STATUS_RETURNED);
        borrowDetailRepository.save(detail);

        if (returnDate.isAfter(detail.getDueDate())) {
            processOverdueFine(detail);
        }
        if (requiresCompensation) {
            financialService.issueDamageCompensation(detail.getBorrowDetailId());
        }

        updateParentBorrowStatus(detail.getBorrow());
        sendInternalNotification(detail.getBorrow().getMember(),
                NotificationType.LOAN, NotificationEventType.RETURN_CONFIRMED, NotificationSource.LIBRARIAN,
                "systemNotification.return.desk.title",
                "systemNotification.return.desk.content", detail.getBook().getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmBatchReturnWithDetails(List<String> barcodes, LocalDateTime returnDate, String bookCondition, String damageNote, String staffUsername) {
        if (barcodes == null || barcodes.isEmpty()) {
            return;
        }
        for (String barcode : barcodes) {
            if (barcode != null && !barcode.trim().isEmpty()) {
                confirmReturnWithDetails(barcode.trim(), returnDate, bookCondition, damageNote, staffUsername);
            }
        }
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
            List<BorrowDetail> allMemberDetails = borrowDetailRepository.findCurrentBorrowsByMemberId(member.getMemberId());
            for (BorrowDetail d : allMemberDetails) {
                if (d.getBookItem() != null && ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus()))) {
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
                if (d.getBookItem() != null && ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus()))) {
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
                .orElseThrow(() -> new IllegalArgumentException(localizedMessageService.get("backend.loan.notFoundById", borrowId)));

        if (!"Approved".equalsIgnoreCase(borrow.getStatus())) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.notWaitingPickup"));
        }

        borrow.setStatus(STATUS_ACTIVE);
        borrow.setBorrowDate(LocalDateTime.now());
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus(STATUS_BORROWED);
            detail.setDueDate(LocalDateTime.now().plusDays(14));
            borrowDetailRepository.save(detail);

            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
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
    @Transactional(rollbackFor = Exception.class)
    public void approveRenewal(Integer borrowDetailId, String staffUsername) {
        BorrowDetail detail = borrowDetailRepository.findByIdForUpdate(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.detailNotFound")));
        if (!"Renew_Pending".equalsIgnoreCase(detail.getStatus()))
            throw new ConflictException(localizedMessageService.get("backend.loan.renewalNotPending"));
        Transaction hold = findPendingRenewalHold(borrowDetailId);
        int maxRenewals = getPositiveIntSetting("MAX_RENEWALS", 2);
        if (detail.getRenewCount() != null && detail.getRenewCount() >= maxRenewals)
            throw new ConflictException(localizedMessageService.get("backend.loan.maxRenewals", maxRenewals));
        if (detail.getDueDate() == null || !detail.getDueDate().isAfter(LocalDateTime.now()))
            throw new ConflictException(localizedMessageService.get("backend.renewal.overdueApproval"));
        int maxDays = getPositiveIntSetting("Max_Renewal_Days", 7);
        if (hold.getRenewalDays() == null || hold.getRenewalDays() < 1 || hold.getRenewalDays() > maxDays)
            throw new ConflictException(localizedMessageService.get("backend.renewal.invalidDays", maxDays));

        detail.setDueDate(detail.getDueDate().plusDays(hold.getRenewalDays()));
        detail.setRenewCount((detail.getRenewCount() == null ? 0 : detail.getRenewCount()) + 1);
        detail.setStatus(STATUS_BORROWED);
        hold.setStatus("Completed");
        borrowDetailRepository.save(detail);
        transactionRepository.save(hold);
        if (detail.getBorrow().getMember() != null) sendInternalNotification(detail.getBorrow().getMember(),
                NotificationType.LOAN, NotificationEventType.RENEWAL_APPROVED, NotificationSource.LIBRARIAN,
                "systemNotification.renewal.approved.title", "systemNotification.renewal.approved.content",
                detail.getBook().getTitle(), detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRenewal(Integer borrowDetailId, String staffUsername) {
        BorrowDetail detail = borrowDetailRepository.findByIdForUpdate(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.loan.detailNotFound")));
        if (!"Renew_Pending".equalsIgnoreCase(detail.getStatus()))
            throw new ConflictException(localizedMessageService.get("backend.loan.renewalNotPending"));
        Transaction hold = findPendingRenewalHold(borrowDetailId);
        Wallet wallet = walletRepository.findByMemberIdForUpdate(detail.getBorrow().getMember().getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException(localizedMessageService.get("backend.financial.walletNotFound")));
        BigDecimal refund = hold.getAmount() == null ? BigDecimal.ZERO : hold.getAmount().abs();
        wallet.setBalance((wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance()).add(refund));
        hold.setStatus("Refunded");
        detail.setStatus(detail.getDueDate() != null && detail.getDueDate().isBefore(LocalDateTime.now()) ? STATUS_OVERDUE : STATUS_BORROWED);
        walletRepository.save(wallet); transactionRepository.save(hold); borrowDetailRepository.save(detail);
        if (detail.getBorrow().getMember() != null) sendInternalNotification(detail.getBorrow().getMember(),
                NotificationType.LOAN, NotificationEventType.RENEWAL_REJECTED, NotificationSource.LIBRARIAN,
                "systemNotification.renewal.rejected.title", "systemNotification.renewal.rejected.content",
                detail.getBook().getTitle(), detail.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private Transaction findPendingRenewalHold(Integer borrowDetailId) {
        return transactionRepository.findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
                        borrowDetailId, "RENEWAL_FEE", "Pending")
                .orElseThrow(() -> new ConflictException(localizedMessageService.get("backend.renewal.holdNotFound")));
    }

    private int getPositiveIntSetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key).map(SystemSetting::getSettingValue)
                    .filter(v -> v != null && !v.isBlank()).map(String::trim).map(Integer::parseInt)
                    .filter(v -> v > 0).orElse(defaultValue);
        } catch (NumberFormatException ignored) { return defaultValue; }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getAllPendingRenewals() {
        return borrowDetailRepository.findAll().stream()
                .filter(d -> "Renew_Pending".equalsIgnoreCase(d.getStatus()))
                .toList();
    }

    @Override
    public java.util.List<com.lms.entity.BorrowDetail> getAllBorrowDetails() {
        return borrowDetailRepository.findAllBorrowDetailsWithRelationships();
    }
    // --- Private Helper Methods ---

    /**
     * Chuyá»ƒn viá»‡c phÃ¡t sinh khoáº£n pháº¡t quÃ¡ háº¡n sang module tÃ i chÃ­nh.
     * Khoáº£n pháº¡t chá»‰ Ä‘Æ°á»£c ghi nháº­n lÃ  cÃ´ng ná»£; vÃ­ chá»‰ thay Ä‘á»•i khi thÃ nh viÃªn thanh toÃ¡n.
     */
    private void processOverdueFine(BorrowDetail detail) {
        if (detail != null && detail.getBorrowDetailId() != null) {
            financialService.issueOverdueFine(detail.getBorrowDetailId());
        }
    }

    private boolean requiresDamageCompensation(String bookCondition) {
        String normalized = bookCondition == null ? "" : bookCondition.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("hÆ° há»ng náº·ng")
                || normalized.contains("máº¥t sÃ¡ch")
                || normalized.contains("severe damage")
                || normalized.contains("lost");
    }

    private String resolveReturnedItemStatus(String bookCondition) {
        String normalized = bookCondition == null ? "" : bookCondition.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("máº¥t sÃ¡ch") || normalized.contains("lost")) {
            return STATUS_LOST;
        }
        if (normalized.contains("hÆ° há»ng náº·ng") || normalized.contains("severe damage")) {
            return STATUS_DAMAGED;
        }
        return STATUS_AVAILABLE;
    }

    /**
     * Cáº­p nháº­t tráº¡ng thÃ¡i cá»§a phiáº¿u mÆ°á»£n cha dá»±a trÃªn cÃ¡c chi tiáº¿t sÃ¡ch Ä‘Ã£ tráº£
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

    /**
     * Táº¡o thÃ´ng bÃ¡o há»‡ thá»‘ng vÃ  gá»­i cho Ä‘á»™c giáº£
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
    public List<BorrowDetail> getMemberBorrowDetailsByDateRange(Integer memberId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return borrowDetailRepository.findBorrowHistoryByDateRange(memberId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getMemberTransactionsByDateRange(Integer memberId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
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
                .orElseThrow(() -> new IllegalArgumentException(localizedMessageService.get("backend.loan.detailNotFound")));

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

