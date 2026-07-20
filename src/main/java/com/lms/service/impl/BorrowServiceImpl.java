package com.lms.service.impl;

import com.lms.dto.request.BorrowRequest;
import com.lms.dto.response.MemberBorrowDTO;
import com.lms.dto.response.ReservationRequestDTO;
import com.lms.dto.response.ReturnRequestDTO;
import com.lms.entity.Author;
import com.lms.entity.Book;
import com.lms.entity.BookItem;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import com.lms.entity.Notification;
import com.lms.entity.Reservation;
import com.lms.entity.SystemSetting;
import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.enums.ActionType;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import com.lms.enums.UserStatus;
import com.lms.exception.ConflictException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.NotificationRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.AuditLogService;
import com.lms.service.BorrowService;
import com.lms.service.LocalizedMessageService;
import com.lms.util.BorrowCodeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import com.lms.service.FinancialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BorrowServiceImpl implements BorrowService {

    @Autowired
    private LocalizedMessageService localizedMessageService = LocalizedMessageService.fallback();

    private static final String PAYMENT_PENDING = "Payment_Pending";
    private static final String PAYMENT_CANCELLED = "Payment_Cancelled";
    private static final String PAYMENT_EXPIRED = "Payment_Expired";
    private static final String PAYMENT_FAILED = "Payment_Failed";

    private final MemberRepository memberRepository;
    private final BookItemRepository bookItemRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final BookRepository bookRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ReservationRepository reservationRepository;
    private final AuditLogService auditLogService;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialService financialService;
    private final com.lms.service.LoanService loanService;

    public BorrowServiceImpl(MemberRepository memberRepository,
            BookItemRepository bookItemRepository,
            BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            BookRepository bookRepository,
            MemberAccountRepository memberAccountRepository,
            SystemSettingRepository systemSettingRepository,
            ReservationRepository reservationRepository,
            AuditLogService auditLogService,
            NotificationRepository notificationRepository,
            MemberNotificationRepository memberNotificationRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            FinancialService financialService,
            com.lms.service.LoanService loanService) {
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookRepository = bookRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.reservationRepository = reservationRepository;
        this.auditLogService = auditLogService;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.financialService = financialService;
        this.loanService = loanService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) {
        if (request == null) {
            throw new ValidationException(localizedMessageService.get("backend.borrow.requestRequired"));
        }
        boolean awaitingBankPayment = "BANK".equalsIgnoreCase(request.getPaymentMethod());
        String identifier = request.getMemberIdentifier() != null && !request.getMemberIdentifier().isBlank()
                ? request.getMemberIdentifier().trim()
                : request.getMemberEmail();
        if (identifier == null || identifier.isBlank()) {
            throw new ValidationException(localizedMessageService.get("backend.borrow.memberIdentifierRequired"));
        }
        Member member = memberRepository.findByUserEmail(identifier)
                .or(() -> memberRepository.findByUserPhone(identifier))
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.member.notFoundByIdentifier")));

        if (member.getUser() == null || member.getUser().getStatus() != UserStatus.Active) {
            throw new ForbiddenException(localizedMessageService.get("backend.member.inactive"));
        }
        validateMemberBorrowEligibility(member);

        if (request.getBarcodes() == null || request.getBarcodes().isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.barcode.required"));
        }
        Set<String> uniqueBarcodes = new LinkedHashSet<>();
        for (String barcode : request.getBarcodes()) {
            String normalized = barcode == null ? "" : barcode.trim();
            if (normalized.isEmpty())
                continue;
            if (!uniqueBarcodes.add(normalized)) {
                throw new ValidationException(localizedMessageService.get("backend.barcode.duplicate", normalized));
            }
        }
        if (uniqueBarcodes.isEmpty()) {
            throw new ValidationException(localizedMessageService.get("backend.barcode.required"));
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : uniqueBarcodes) {
            BookItem item = bookItemRepository.findByBarcodeForUpdate(barcode)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            localizedMessageService.get("backend.loan.barcodeNotFound", barcode)));
            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException(localizedMessageService.get("backend.loan.barcodeUnavailable", barcode));
            }
            bookItemsToBorrow.add(item);
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowCount + bookItemsToBorrow.size() > maxLimit) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.tierLimitExceeded"));
        }

        int borrowDays = normalizeBorrowDays(request.getNumberOfDays());

        // Tính toán số tiền

        double feePerBookPerDay = 5000.0;
        SystemSetting feeSetting = systemSettingRepository.findBySettingKey("BORROW_FEE_PER_BOOK").orElse(null);
        if (feeSetting != null) {
            try {
                feePerBookPerDay = Double.parseDouble(feeSetting.getSettingValue());
            } catch (NumberFormatException ignored) {
                // Keep the documented default when the optional setting is malformed.
            }
        }
        double baseFee = bookItemsToBorrow.size() * borrowDays * feePerBookPerDay;
        double discount = member.getTier() != null && member.getTier().getDiscountPercent() != null
                ? member.getTier().getDiscountPercent().doubleValue()
                : 0.0;
        double finalFeeDouble = baseFee - (baseFee * discount / 100);
        BigDecimal finalFee = BigDecimal.valueOf(finalFeeDouble);

        // Xá»­ lÃ½ thanh toÃ¡n vÃ­ náº¿u user chá»n WALLET
        if ("WALLET".equalsIgnoreCase(request.getPaymentMethod())) {
            Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            localizedMessageService.get("backend.financial.walletNotFound")));
            if (wallet.getBalance().compareTo(finalFee) < 0) {
                throw new ConflictException(localizedMessageService.get("backend.financial.insufficientBalanceSimple"));
            }
            wallet.setBalance(wallet.getBalance().subtract(finalFee));
            walletRepository.save(wallet);
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus(awaitingBankPayment ? PAYMENT_PENDING : "Active");
        borrow = borrowRepository.save(borrow);

        // Ghi láº¡i giao dá»‹ch náº¿u lÃ  thanh toÃ¡n vÃ­
        if ("WALLET".equalsIgnoreCase(request.getPaymentMethod())) {
            Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId()).get();
            Transaction transaction = new Transaction();
            transaction.setWallet(wallet);
            transaction.setBorrow(borrow);
            transaction.setTransactionType("PAYMENT");
            transaction.setAmount(finalFee.negate());
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transactionRepository.save(transaction);
        }

        for (BookItem item : bookItemsToBorrow) {
            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(item.getBook());
            detail.setBookItem(item);
            detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
            detail.setStatus(awaitingBankPayment ? PAYMENT_PENDING : "Borrowed");
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            item.setStatus(awaitingBankPayment ? PAYMENT_PENDING : "Borrowed");
            bookItemRepository.save(item);
        }

        // Tạo danh sách tên sách
        String bookNames = bookItemsToBorrow.stream()
                .map(item -> item.getBook().getTitle())
                .collect(java.util.stream.Collectors.joining(", "));

        if (awaitingBankPayment) {
            return borrow;
        }

        // Gửi thông báo trực tiếp cho độc giả khi tạo phiếu mượn thành công tại quầy
        sendInternalNotification(member,
                NotificationType.LOAN, NotificationEventType.LOAN_COLLECTED, NotificationSource.LIBRARIAN,
                "systemNotification.borrow.success.title",
                "systemNotification.borrow.desk.content", bookNames, BorrowCodeFormatter.format(borrow.getBorrowId()),
                LocalDateTime.now().plusDays(borrowDays)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow activatePendingBankBorrow(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.notFoundById", borrowId)));
        if ("Active".equalsIgnoreCase(borrow.getStatus())
                || "Borrowing".equalsIgnoreCase(borrow.getStatus())
                || "Overdue".equalsIgnoreCase(borrow.getStatus())) {
            return borrow;
        }
        if (!PAYMENT_PENDING.equalsIgnoreCase(borrow.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.notAwaitingPayment"));
        }

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        if (details.isEmpty()) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.noDetails"));
        }

        LocalDateTime paidAt = LocalDateTime.now();
        LocalDateTime pendingAt = borrow.getBorrowDate();
        for (BorrowDetail detail : details) {
            if (!PAYMENT_PENDING.equalsIgnoreCase(detail.getStatus())) {
                throw new ConflictException(localizedMessageService.get("backend.borrow.detailNotAwaitingPayment"));
            }
            BookItem item = detail.getBookItem();
            if (item == null || !PAYMENT_PENDING.equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException(localizedMessageService.get("backend.borrow.copyNoLongerReserved"));
            }

            long borrowDays = pendingAt == null || detail.getDueDate() == null
                    ? normalizeBorrowDays(null)
                    : Math.max(1, ChronoUnit.DAYS.between(pendingAt, detail.getDueDate()));
            detail.setDueDate(paidAt.plusDays(borrowDays));
            detail.setStatus("Borrowed");
            borrowDetailRepository.save(detail);

            item.setStatus("Borrowed");
            bookItemRepository.save(item);
        }

        borrow.setBorrowDate(paidAt);
        borrow.setStatus("Active");
        Borrow activatedBorrow = borrowRepository.save(borrow);
        sendSuccessfulBorrowNotification(borrow, details);
        return activatedBorrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingBankBorrow(Integer borrowId, String paymentStatus) {
        Borrow borrow = borrowRepository.findById(borrowId).orElse(null);
        if (borrow == null || !PAYMENT_PENDING.equalsIgnoreCase(borrow.getStatus())) {
            return;
        }

        for (BorrowDetail detail : borrowDetailRepository.findByBorrowId(borrowId)) {
            BookItem item = detail.getBookItem();
            if (item != null && PAYMENT_PENDING.equalsIgnoreCase(item.getStatus())) {
                item.setStatus("Available");

                bookItemRepository.save(item);
            }
            if (PAYMENT_PENDING.equalsIgnoreCase(detail.getStatus())) {
                detail.setStatus("Cancelled");
                borrowDetailRepository.save(detail);
            }
        }

        String normalizedStatus = paymentStatus == null ? "" : paymentStatus.trim().toUpperCase();
        borrow.setStatus(switch (normalizedStatus) {
            case "EXPIRED" -> PAYMENT_EXPIRED;
            case "FAILED" -> PAYMENT_FAILED;
            default -> PAYMENT_CANCELLED;
        });
        borrowRepository.save(borrow);
        if (borrow.getMember() != null) {
            sendInternalNotification(borrow.getMember(),
                    NotificationType.FINANCE, NotificationEventType.LOAN_REJECTED, NotificationSource.SYSTEM,
                    "systemNotification.borrow.paymentCancelled.title",
                    "systemNotification.borrow.paymentCancelled.content",
                    BorrowCodeFormatter.format(borrowId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.member.currentNotFound")));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.borrow.requestedBookNotFound")));

        if ("Inactive".equalsIgnoreCase(book.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.bookUnavailable"));
        }

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed >= maxLimit) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.memberLimitReached", maxLimit));
        }

        int borrowDays = normalizeBorrowDays(numberOfDays);
        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Pending");
        borrow = borrowRepository.save(borrow);

        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBook(book);
        detail.setBookItem(null);
        detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
        detail.setStatus("Pending");
        detail.setRenewCount(0);
        borrowDetailRepository.save(detail);

        auditLogService.log(
                ActionType.REQUEST_BORROW,
                localizedMessageService.get("backend.borrow.audit.requested", username, book.getBookId(),
                        book.getTitle(), borrowDays));

        sendInternalNotification(member,
                NotificationType.LOAN, NotificationEventType.LOAN_REQUESTED, NotificationSource.SYSTEM,
                "systemNotification.borrow.requested.title",
                "systemNotification.borrow.requested.content", book.getTitle());

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectPendingRequest(Integer borrowId, String reasonCode, String reason) {
        var rejection = com.lms.util.RejectionReasonValidator.validate("BORROW", reasonCode, reason);
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.requestNotFound")));
        if (!"Pending".equalsIgnoreCase(borrow.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.notPendingApproval"));
        }

        Member member = borrow.getMember();

        // Cáº­p nháº­t tráº¡ng thÃ¡i Borrow thÃ nh Rejected
        borrow.setStatus("Rejected");
        borrow.setRejectionCode(rejection.code());
        borrow.setRejectionReason(rejection.detail());
        borrowRepository.save(borrow);

        // Cáº­p nháº­t tráº¡ng thÃ¡i Táº¤T Cáº¢ BorrowDetail liÃªn quan thÃ nh Rejected
        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        String bookNames = details.stream()
                .map(d -> d.getBook().getTitle())
                .collect(Collectors.joining(", "));
        for (BorrowDetail detail : details) {
            detail.setStatus("Rejected");
            borrowDetailRepository.save(detail);
        }

        // Gửi thông báo đến member
        String content = localizedMessageService.get("systemNotification.borrow.rejected.content", bookNames, borrowId);
        if (reason != null && !reason.trim().isEmpty()) {
            content += localizedMessageService.get("common.rejectReasonPrefix", reason.trim());
        }

        Object translatedReason = localizedMessageService.messageArgument("rejection.code." + rejection.code());
        if (rejection.detail() == null) {
            sendInternalNotification(member,
                    NotificationType.LOAN, NotificationEventType.LOAN_REJECTED, NotificationSource.LIBRARIAN,
                    "systemNotification.borrow.rejected.title",
                    "systemNotification.borrow.rejected.contentWithoutDetail",
                    bookNames, BorrowCodeFormatter.format(borrowId), translatedReason);
        } else {
            sendInternalNotification(member,
                    NotificationType.LOAN, NotificationEventType.LOAN_REJECTED, NotificationSource.LIBRARIAN,
                    "systemNotification.borrow.rejected.title", "systemNotification.borrow.rejected.contentWithReason",
                    bookNames, BorrowCodeFormatter.format(borrowId), translatedReason, rejection.detail());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePendingRequest(Integer borrowId, List<String> barcodes, String staffUsername) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.requestNotFound")));
        if (!"Pending".equalsIgnoreCase(borrow.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.notPendingApproval"));
        }

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        if (details.isEmpty()) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.noDetails"));
        }

        if (barcodes == null || barcodes.size() != details.size()) {
            throw new ValidationException(
                    "Số lượng mã vạch sách cung cấp không khớp với số lượng sách trong yêu cầu mượn.");
        }

        Member member = borrow.getMember();

        // Đếm số lượng yêu cầu cho mỗi bookId trong đơn hiện tại
        java.util.Map<Integer, Long> requestCounts = details.stream()
                .collect(java.util.stream.Collectors.groupingBy(d -> d.getBook().getBookId(),
                        java.util.stream.Collectors.counting()));

        // ── 1. Giai đoạn 1: Xác thực mã vạch và trạng thái ban đầu của toàn bộ bản
        // sách ──
        java.util.List<BookItem> matchedItems = new java.util.ArrayList<>();
        for (int i = 0; i < details.size(); i++) {
            BorrowDetail detail = details.get(i);
            String barcode = barcodes.get(i).trim();

            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã vạch sách: " + barcode));

            // Đảm bảo sách này khớp với sách được yêu cầu trong phiếu mượn
            if (!item.getBook().getBookId().equals(detail.getBook().getBookId())) {
                throw new ConflictException("Mã vạch " + barcode + " thuộc cuốn '" + item.getBook().getTitle()
                        + "', không khớp với sách yêu cầu '" + detail.getBook().getTitle() + "'.");

            }

            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException(
                        "Bản sách " + barcode + " không khả dụng (Trạng thái hiện tại: " + item.getStatus() + ").");
            }
            matchedItems.add(item);
        }

        // Kiểm tra số lượng tồn kho khả dụng cho mỗi đầu sách trong đơn yêu cầu
        for (java.util.Map.Entry<Integer, Long> entry : requestCounts.entrySet()) {
            Integer bookId = entry.getKey();
            Long reqCount = entry.getValue();
            long availableCount = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, "Available");
            if (availableCount < reqCount) {
                Book tempBook = bookRepository.findById(bookId).orElse(null);
                String title = tempBook != null ? tempBook.getTitle() : "Sách";
                throw new ConflictException("Sách '" + title + "' chỉ còn " + availableCount
                        + " cuốn khả dụng trong kho, không đủ để duyệt đơn yêu cầu mượn " + reqCount + " cuốn.");
            }
        }

        // ── 2. Giai đoạn 2: Cập nhật trạng thái vào Database (sau khi tất cả kiểm tra
        // đều thành công) ──
        for (int i = 0; i < details.size(); i++) {
            BorrowDetail detail = details.get(i);
            BookItem item = matchedItems.get(i);

            detail.setBookItem(item);
            item.setStatus("Waiting_Pickup");
            bookItemRepository.save(item);
            detail.setStatus("Waiting_Pickup");
            borrowDetailRepository.save(detail);
        }

        // â”€â”€ 2. TÃ­nh phÃ­ mÆ°á»£n
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double feePerBookPerDay = 5000.0;
        SystemSetting feeSetting = systemSettingRepository.findBySettingKey("BORROW_FEE_PER_BOOK").orElse(null);
        if (feeSetting != null) {
            try {
                feePerBookPerDay = Double.parseDouble(feeSetting.getSettingValue());
            } catch (NumberFormatException ignored) {
            }
        }

        int borrowDays = normalizeBorrowDays(null);
        if (details.get(0).getDueDate() != null && borrow.getBorrowDate() != null) {
            long computed = ChronoUnit.DAYS.between(borrow.getBorrowDate(), details.get(0).getDueDate());
            if (computed > 0)
                borrowDays = (int) computed;
        }

        double discount = member.getTier() != null && member.getTier().getDiscountPercent() != null
                ? member.getTier().getDiscountPercent().doubleValue()
                : 0.0;
        double baseFee = details.size() * borrowDays * feePerBookPerDay;
        BigDecimal finalFee = BigDecimal.valueOf(baseFee - (baseFee * discount / 100));

        // â”€â”€ 3. Trá»« tiá»n vÃ­
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseThrow(() -> new ConflictException(
                        localizedMessageService.get("backend.financial.walletNotFound")));
        if (wallet.getBalance().compareTo(finalFee) < 0) {
            throw new ConflictException(localizedMessageService.get(
                    "backend.borrow.insufficientBalanceForFee", String.format("%,.0f", finalFee)));
        }
        wallet.setBalance(wallet.getBalance().subtract(finalFee));
        walletRepository.save(wallet);

        // â”€â”€ 4. Ghi lá»‹ch sá»­ giao dá»‹ch
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(borrow);
        transaction.setTransactionType("PAYMENT");
        transaction.setAmount(finalFee.negate());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transactionRepository.save(transaction);

        // â”€â”€ 5. Cáº­p nháº­t tráº¡ng thÃ¡i sang Chá» nháº­n báº£n váº­t lÃ½
        // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Waiting_Pickup");
        borrowRepository.save(borrow);

        for (BorrowDetail detail : details) {
            detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
            borrowDetailRepository.save(detail);
        }

        String bookNames = details.stream()
                .map(d -> d.getBook().getTitle())
                .collect(Collectors.joining(", "));
        sendInternalNotification(member,
                NotificationType.LOAN, NotificationEventType.LOAN_APPROVED, NotificationSource.LIBRARIAN,
                "systemNotification.borrow.approved.title",
                "systemNotification.borrow.approvedWithFee.content",
                bookNames, finalFee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPhysicalPickup(Integer borrowId, String staffUsername) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.notFoundById", borrowId)));
        if (!"Waiting_Pickup".equalsIgnoreCase(borrow.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.notWaitingPickup"));
        }

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        if (details.isEmpty()) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.noDetails"));
        }
        if (details.stream().anyMatch(d -> d.getBookItem() == null || d.getBookItem().getBarcode() == null
                || d.getBookItem().getBarcode().isBlank())) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.barcodeRequiredBeforePickup"));
        }

        // TÃ­nh sá»‘ ngÃ y mÆ°á»£n tá»« dueDate ban Ä‘áº§u so vá»›i borrowDate cÅ©
        int borrowDays = normalizeBorrowDays(null);
        if (details.get(0).getDueDate() != null && borrow.getBorrowDate() != null) {
            long computed = ChronoUnit.DAYS.between(borrow.getBorrowDate(), details.get(0).getDueDate());
            if (computed > 0)
                borrowDays = (int) computed;
        }

        // Báº¯t Ä‘áº§u tÃ­nh thá»i gian tá»« thá»i Ä‘iá»ƒm nháº­n sÃ¡ch thá»±c táº¿
        LocalDateTime pickupTime = LocalDateTime.now();
        for (BorrowDetail detail : details) {
            detail.setStatus("Borrowed");
            detail.setDueDate(pickupTime.plusDays(borrowDays));
            borrowDetailRepository.save(detail);

            // Äáº£m báº£o BookItem Ä‘ang á»Ÿ tráº¡ng thÃ¡i Borrowed
            if (detail.getBookItem() != null) {
                detail.getBookItem().setStatus("Borrowed");
                bookItemRepository.save(detail.getBookItem());
            }
        }

        // Cáº­p nháº­t ngÃ y mÆ°á»£n thá»±c táº¿ vÃ  tráº¡ng thÃ¡i Active
        borrow.setBorrowDate(pickupTime);
        borrow.setStatus("Active");
        borrowRepository.save(borrow);

        String bookNames = details.stream()
                .map(d -> d.getBook().getTitle())
                .collect(Collectors.joining(", "));
        sendInternalNotification(borrow.getMember(),
                NotificationType.LOAN, NotificationEventType.LOAN_COLLECTED, NotificationSource.LIBRARIAN,
                "systemNotification.borrow.pickup.title",
                "systemNotification.borrow.pickup.content", bookNames, borrowDays,
                pickupTime.plusDays(borrowDays)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitRenewRequest(String username, Integer borrowDetailId, Integer renewalDays) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.member.currentNotFound")));
        MemberAccount account = memberAccountRepository.findByMemberMemberId(member.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.account.memberNotFound")));
        if (!"Active".equalsIgnoreCase(account.getStatus()))
            throw new ForbiddenException(localizedMessageService.get("backend.member.inactive"));
        BorrowDetail detail = borrowDetailRepository.findByIdForUpdate(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.detailNotFound")));
        if (detail.getBorrow() == null || detail.getBorrow().getMember() == null
                || !member.getMemberId().equals(detail.getBorrow().getMember().getMemberId()))
            throw new ConflictException(localizedMessageService.get("backend.renewal.forbidden"));
        if (!"Borrowed".equalsIgnoreCase(detail.getStatus()))
            throw new ConflictException(localizedMessageService.get("backend.loan.renewBorrowedOnly"));
        if (detail.getDueDate() == null || !detail.getDueDate().isAfter(LocalDateTime.now()))
            throw new ConflictException(localizedMessageService.get("backend.renewal.overdue"));
        int minimumHoursBeforeDue = 24; // Fixed system policy.
        if (!detail.getDueDate().isAfter(LocalDateTime.now().plusHours(minimumHoursBeforeDue)))
            throw new ConflictException(
                    localizedMessageService.get("backend.renewal.tooCloseToDue", minimumHoursBeforeDue));
        int maxDays = getPositiveIntSetting("Max_Renewal_Days", 7);
        if (renewalDays == null || renewalDays < 1 || renewalDays > maxDays)
            throw new ValidationException(localizedMessageService.get("backend.renewal.invalidDays", maxDays));
        int maxRenewals = getPositiveIntSetting("MAX_RENEWALS", 2);
        if (detail.getRenewCount() != null && detail.getRenewCount() >= maxRenewals)
            throw new ConflictException(localizedMessageService.get("backend.loan.maxRenewals", maxRenewals));
        int maxRenewalRequests = getPositiveIntSetting("MAX_RENEWAL_REQUESTS_PER_LOAN", 3);
        long renewalRequestCount = transactionRepository.countByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCase(
                borrowDetailId, "RENEWAL_FEE");
        if (renewalRequestCount >= maxRenewalRequests)
            throw new ConflictException(
                    localizedMessageService.get("backend.renewal.requestLimitReached", maxRenewalRequests));
        int cooldownHours = getPositiveIntSetting("RENEWAL_REJECTION_COOLDOWN_HOURS", 24);
        transactionRepository
                .findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDescTransactionIdDesc(
                        borrowDetailId, "REFUND", "Completed")
                .map(Transaction::getTransactionDate)
                .filter(rejectedAt -> rejectedAt.plusHours(cooldownHours).isAfter(LocalDateTime.now()))
                .ifPresent(rejectedAt -> {
                    throw new ConflictException(localizedMessageService.get("backend.renewal.rejectionCooldown",
                            rejectedAt.plusHours(cooldownHours)
                                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                });
        if (detail.getBook() != null) {
            Integer bookId = detail.getBook().getBookId();
            bookRepository.findByIdForUpdate(bookId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException(localizedMessageService.get("backend.book.notFound")));
            long waitingReservations = reservationRepository.countActiveReservationsByOtherMemberForBook(
                    bookId, member.getMemberId(), List.of("PENDING", "DEPOSIT_PAID", "READY", "ACTIVE"));
            long availableCopies = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, "Available");
            if (waitingReservations > availableCopies)
                throw new ConflictException(localizedMessageService.get("backend.renewal.reservedByAnotherMember"));
        }
        if (transactionRepository
                .findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
                        borrowDetailId, "RENEWAL_FEE", "Pending")
                .isPresent())
            throw new ConflictException(localizedMessageService.get("backend.renewal.alreadyPending"));

        BigDecimal feePerDay = BigDecimal.valueOf(getPositiveIntSetting("FEE_PER_BOOK_PER_DAY", 5000));
        BigDecimal fee = feePerDay.multiply(BigDecimal.valueOf(renewalDays)).setScale(2,
                java.math.RoundingMode.HALF_UP);
        Wallet wallet = walletRepository.findByMemberIdForUpdate(member.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.financial.walletNotFound")));
        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        if (balance.compareTo(fee) < 0)
            throw new ConflictException(
                    localizedMessageService.get("backend.renewal.insufficientBalance", fee, balance));
        wallet.setBalance(balance.subtract(fee));
        walletRepository.save(wallet);

        Transaction hold = new Transaction();
        hold.setWallet(wallet);
        hold.setBorrow(detail.getBorrow());
        hold.setBorrowDetail(detail);
        hold.setRenewalDays(renewalDays);
        hold.setTransactionType("RENEWAL_FEE");
        hold.setAmount(fee.negate());
        hold.setTransactionDate(LocalDateTime.now());
        hold.setStatus("Pending");
        transactionRepository.save(hold);
        detail.setRejectionCode(null);
        detail.setRejectionReason(null);
        detail.setStatus("Renew_Pending");
        borrowDetailRepository.save(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.barcodeNotFound", barcode)));
        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus())
                                || "Overdue".equalsIgnoreCase(d.getStatus())
                                || "Return_Pending".equalsIgnoreCase(d.getStatus())
                                || "Renew_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.activeHistoryNotFound")));

        if ("Renew_Pending".equalsIgnoreCase(activeDetail.getStatus())) {
            loanService.rejectRenewal(activeDetail.getBorrowDetailId(), "SYSTEM", "RETURNED_BEFORE_APPROVAL", null);
        }

        item.setStatus("Available");

        bookItemRepository.save(item);
        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus("Returned");
        borrowDetailRepository.save(activeDetail);

        Borrow parent = activeDetail.getBorrow();
        List<BorrowDetail> sideDetails = getBorrowDetailsByBorrowId(parent.getBorrowId());
        if (sideDetails.stream().allMatch(d -> "Returned".equalsIgnoreCase(d.getStatus()))) {
            parent.setStatus("Returned");
            borrowRepository.save(parent);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation memberSubmitReservationRequest(String username, Integer bookId) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.account.memberNotFound")));
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.borrow.reservedBookNotFound")));

        boolean alreadyReserved = reservationRepository
                .findByMember_MemberIdOrderByReservationDateDesc(member.getMemberId())
                .stream()
                .anyMatch(r -> r.getBook() != null
                        && r.getBook().getBookId().equals(bookId)
                        && ("Pending".equalsIgnoreCase(r.getStatus())
                                || "Deposit_Paid".equalsIgnoreCase(r.getStatus())
                                || "Refund_Pending".equalsIgnoreCase(r.getStatus())
                                || "Ready".equalsIgnoreCase(r.getStatus())
                                || "Active".equalsIgnoreCase(r.getStatus())));
        if (alreadyReserved) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.reservationAlreadyPending"));
        }

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setStatus("Pending");
        Reservation savedReservation = reservationRepository.save(reservation);
        financialService.payReservationDeposit(member.getMemberId(), savedReservation.getReservationId());
        auditLogService.log(
                ActionType.RESERVE_BOOK,
                localizedMessageService.get("backend.borrow.audit.reservationRequested",
                        username, book.getBookId(), book.getTitle()));
        return savedReservation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReservationRequest(Integer reservationId, String staffUsername) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.borrow.reservationNotFound")));
        if (!"Deposit_Paid".equalsIgnoreCase(reservation.getStatus())) {
            throw new ConflictException(localizedMessageService.get("backend.borrow.reservationDepositRequired"));
        }

        reservation.setStatus("Active");
        reservationRepository.save(reservation);
        sendInternalNotification(reservation.getMember(),
                NotificationType.RESERVATION, NotificationEventType.RESERVATION_APPROVED, NotificationSource.LIBRARIAN,
                "systemNotification.reservation.approved.title",
                "systemNotification.reservation.approved.content", reservation.getBook().getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReservationRequest(Integer reservationId, String staffUsername, String reasonCode,
            String reason) {
        var rejection = com.lms.util.RejectionReasonValidator.validate("RESERVATION", reasonCode, reason);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.borrow.reservationNotFound")));

        String currentStatus = reservation.getStatus();
        boolean isPending = "Pending".equalsIgnoreCase(currentStatus);
        boolean isDepositPaid = "Deposit_Paid".equalsIgnoreCase(currentStatus);

        if (!isPending && !isDepositPaid) {
            throw new ConflictException(
                    localizedMessageService.get("backend.borrow.reservationAlreadyProcessedWithStatus", currentStatus));
        }

        // Náº¿u member Ä‘Ã£ ná»™p cá»c â†’ hoÃ n tiá»n cá»c vá» vÃ­
        if (isDepositPaid) {
            Member member = reservation.getMember();
            Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            localizedMessageService.get("backend.financial.walletNotFound")));
            BigDecimal refundAmount = financialService.getReservationDepositAmount();
            if (refundAmount != null && refundAmount.signum() > 0) {
                wallet.setBalance(wallet.getBalance().add(refundAmount));
                walletRepository.save(wallet);

                Transaction tx = new Transaction();
                tx.setWallet(wallet);
                tx.setTransactionType("REFUND");
                tx.setAmount(refundAmount);
                tx.setStatus("Completed");
                tx.setTransactionDate(LocalDateTime.now());
                transactionRepository.save(tx);

                sendInternalNotification(member,
                        NotificationType.RESERVATION, NotificationEventType.RESERVATION_REFUNDED,
                        NotificationSource.LIBRARIAN,
                        "systemNotification.reservation.refund.title",
                        "systemNotification.reservation.refund.content",
                        refundAmount, reservation.getBook().getTitle());
            }
        }

        reservation.setStatus(isDepositPaid ? "Refunded" : "Rejected");
        reservation.setRejectionCode(rejection.code());
        reservation.setRejectionReason(rejection.detail());
        reservationRepository.save(reservation);
        String content = localizedMessageService.get("systemNotification.reservation.rejectedWithRefund.content",
                reservation.getBook().getTitle(), reservationId,
                isDepositPaid ? localizedMessageService.get("systemNotification.reservation.refundSuffix") : "");
        if (reason != null && !reason.trim().isEmpty()) {
            content += localizedMessageService.get("common.rejectReasonPrefix", reason.trim());
        }
        Object translatedReason = localizedMessageService.messageArgument("rejection.code." + rejection.code());
        boolean hasDetail = rejection.detail() != null;
        String rejectionContentKey = isDepositPaid
                ? (hasDetail ? "systemNotification.reservation.rejectedWithDepositRefund.contentWithReason"
                        : "systemNotification.reservation.rejectedWithDepositRefund.contentWithoutDetail")
                : (hasDetail ? "systemNotification.reservation.rejectedWithoutDeposit.contentWithReason"
                        : "systemNotification.reservation.rejectedWithoutDeposit.contentWithoutDetail");
        if (hasDetail) {
            sendInternalNotification(reservation.getMember(), NotificationType.RESERVATION,
                    NotificationEventType.RESERVATION_REJECTED,
                    NotificationSource.LIBRARIAN, "systemNotification.reservation.rejected.title", rejectionContentKey,
                    reservation.getBook().getTitle(), reservationId, translatedReason, rejection.detail());
        } else {
            sendInternalNotification(reservation.getMember(), NotificationType.RESERVATION,
                    NotificationEventType.RESERVATION_REJECTED,
                    NotificationSource.LIBRARIAN, "systemNotification.reservation.rejected.title", rejectionContentKey,
                    reservation.getBook().getTitle(), reservationId, translatedReason);
        }
    }

    @Override
    public Reservation getReservationById(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.borrow.reservationNotFound")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberCancelReservation(String username, Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.borrow.reservationNotFound")));
        Integer currentMemberId = getMemberIdByUsername(username);
        if (currentMemberId == null || !reservation.getMember().getMemberId().equals(currentMemberId)) {
            throw new ForbiddenException(
                    localizedMessageService.get("backend.borrow.cancelOthersReservationForbidden"));
        }
        reservation.setStatus("Canceled");
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getPendingReturnRequestDTOs() {
        return borrowDetailRepository.findByStatus("Return_Pending").stream()
                .map(bd -> {
                    Member member = bd.getBorrow().getMember();
                    String name = member != null && member.getUser() != null ? member.getUser().getFullName() : "N/A";
                    String email = member != null && member.getUser() != null ? member.getUser().getEmail() : "N/A";
                    return new ReturnRequestDTO(
                            bd.getBorrowDetailId(),
                            name,
                            email,
                            bd.getBook().getTitle(),
                            bd.getBookItem() != null ? bd.getBookItem().getBarcode() : "N/A",
                            bd.getBorrow().getBorrowDate());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getAllPendingReservations() {
        return reservationRepository.findAll().stream()
                .filter(r -> "Pending".equalsIgnoreCase(r.getStatus())
                        || "Deposit_Paid".equalsIgnoreCase(r.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationRequestDTO> getPendingReservationDTOs() {
        java.util.Map<Integer, String> usernameMap = memberAccountRepository.findAll().stream()
                .filter(ma -> ma.getMember() != null)
                .collect(Collectors.toMap(
                        ma -> ma.getMember().getMemberId(),
                        ma -> ma.getUsername(),
                        (ex, rep) -> ex));
        return getAllPendingReservations().stream()
                .map(r -> {
                    String email = r.getMember() != null && r.getMember().getUser() != null
                            ? r.getMember().getUser().getEmail()
                            : "";
                    String phone = r.getMember() != null && r.getMember().getUser() != null
                            ? r.getMember().getUser().getPhone()
                            : "";
                    String username = r.getMember() != null ? usernameMap.getOrDefault(r.getMember().getMemberId(), "")
                            : "";
                    return new ReservationRequestDTO(
                            r.getReservationId(),
                            r.getMember() != null && r.getMember().getUser() != null
                                    ? r.getMember().getUser().getFullName()
                                    : "N/A",
                            r.getBook().getTitle(),
                            r.getReservationDate(),
                            1,
                            email,
                            phone,
                            username);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getBorrowsByMemberAndStatus(String username, String status) {
        Integer id = getMemberIdByUsername(username);
        return id == null ? new ArrayList<>()
                : borrowRepository.findAll().stream()
                        .filter(b -> b.getMember() != null && id.equals(b.getMember().getMemberId())
                                && status.equalsIgnoreCase(b.getStatus()))
                        .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllPendingRequests() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Pending".equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllReturnRequests() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Return_Pending".equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllActiveLoans() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Active".equalsIgnoreCase(b.getStatus()) || "Borrowing".equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer loanId, String status) {
        Borrow borrow = borrowRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.notFoundById", loanId)));
        borrow.setStatus(status);
        borrowRepository.save(borrow);
    }

    @Override
    @Transactional(readOnly = true)
    public Borrow getBorrowById(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.notFoundById", borrowId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId) {
        return borrowDetailRepository.findByBorrowId(borrowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllBorrowHistoryByMember(String username) {
        Integer id = getMemberIdByUsername(username);
        return id == null ? new ArrayList<>()
                : borrowRepository.findAll().stream()
                        .filter(b -> b.getMember() != null && id.equals(b.getMember().getMemberId()))
                        .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberCurrentBorrows(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null)
            return new ArrayList<>();
        return borrowDetailRepository.findCurrentBorrowsByMemberId(memberId).stream()
                .filter(d -> "Pending".equalsIgnoreCase(d.getStatus())
                        || "Waiting_Pickup".equalsIgnoreCase(d.getStatus())
                        || "Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus())
                        || "Renew_Pending".equalsIgnoreCase(d.getStatus()))
                .map(this::toMemberBorrowDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberReservations(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null)
            return new ArrayList<>();

        List<MemberBorrowDTO> dtoList = new ArrayList<>();
        for (Reservation res : reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(memberId)) {
            if (!"Pending".equalsIgnoreCase(res.getStatus())
                    && !"Deposit_Paid".equalsIgnoreCase(res.getStatus())
                    && !"Refund_Pending".equalsIgnoreCase(res.getStatus())
                    && !"Ready".equalsIgnoreCase(res.getStatus())
                    && !"Active".equalsIgnoreCase(res.getStatus())) {
                continue;
            }
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(res.getReservationId());
            dto.setBookId(res.getBook().getBookId());
            dto.setBookTitle(res.getBook().getTitle());
            dto.setAuthorName(getAuthorNames(res.getBook()));
            dto.setBookImage(res.getBook().getCoverImageUrl());
            dto.setBookIdStr("RES-" + res.getBook().getBookId());
            dto.setActionDate(res.getReservationDate());
            if (res.getReservationDate() != null) {
                dto.setDueDate(res.getReservationDate().plusDays(3));
            }
            dto.setStatus(res.getStatus());
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberOneMonthHistory(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null)
            return new ArrayList<>();
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        return borrowDetailRepository.findBorrowHistoryInOneMonth(memberId, oneMonthAgo).stream()
                .filter(d -> "Returned".equalsIgnoreCase(d.getStatus())
                        || "Canceled".equalsIgnoreCase(d.getStatus())
                        || "Lost".equalsIgnoreCase(d.getStatus()))
                .map(this::toMemberBorrowDTO)
                .toList();
    }

    private MemberBorrowDTO toMemberBorrowDTO(BorrowDetail detail) {
        MemberBorrowDTO dto = new MemberBorrowDTO();
        dto.setId(detail.getBorrowDetailId());
        dto.setBookId(detail.getBook().getBookId());
        dto.setBookTitle(detail.getBook().getTitle());
        dto.setAuthorName(getAuthorNames(detail.getBook()));
        dto.setBookImage(detail.getBook().getCoverImageUrl());
        dto.setBarcodeAssigned(detail.getBookItem() != null && detail.getBookItem().getBarcode() != null
                && !detail.getBookItem().getBarcode().isBlank());
        dto.setBookIdStr(dto.isBarcodeAssigned() ? detail.getBookItem().getBarcode()
                : localizedMessageService.get("backend.book.barcodeNotAssigned"));
        if (detail.getBorrow() != null) {
            dto.setBorrowIdStr(BorrowCodeFormatter.format(detail.getBorrow().getBorrowId()));
        }
        dto.setActionDate(detail.getBorrow().getBorrowDate());
        dto.setDueDate(detail.getDueDate());
        dto.setReturnDate(detail.getReturnDate());
        dto.setStatus(detail.getStatus());
        if (detail.getBorrow().getBorrowDate() != null && detail.getDueDate() != null) {
            long totalDays = ChronoUnit.DAYS.between(detail.getBorrow().getBorrowDate(), detail.getDueDate());
            long elapsedDays = ChronoUnit.DAYS.between(detail.getBorrow().getBorrowDate(), LocalDateTime.now());
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), detail.getDueDate());
            dto.setDaysLeft(Math.max(0, daysLeft));
            int progress = totalDays > 0 ? (int) ((elapsedDays * 100) / totalDays) : 0;
            dto.setProgressPercentage(Math.min(100, Math.max(0, progress)));
        }
        dto.setRenewCount(detail.getRenewCount() != null ? detail.getRenewCount() : 0);
        int maxRenewalRequests = getPositiveIntSetting("MAX_RENEWAL_REQUESTS_PER_LOAN", 3);
        long requestCount = transactionRepository.countByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCase(
                detail.getBorrowDetailId(), "RENEWAL_FEE");
        dto.setRenewalRequestCount((int) Math.min(Integer.MAX_VALUE, requestCount));
        if (requestCount >= maxRenewalRequests) {
            dto.setRenewalRequestBlocked(true);
            dto.setRenewalBlockedReason(
                    localizedMessageService.get("backend.renewal.requestLimitReached", maxRenewalRequests));
        } else {
            int cooldownHours = getPositiveIntSetting("RENEWAL_REJECTION_COOLDOWN_HOURS", 24);
            transactionRepository
                    .findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDescTransactionIdDesc(
                            detail.getBorrowDetailId(), "REFUND", "Completed")
                    .map(Transaction::getTransactionDate)
                    .filter(rejectedAt -> rejectedAt.plusHours(cooldownHours).isAfter(LocalDateTime.now()))
                    .ifPresent(rejectedAt -> {
                        dto.setRenewalRequestBlocked(true);
                        dto.setRenewalBlockedReason(localizedMessageService.get("backend.renewal.rejectionCooldown",
                                rejectedAt.plusHours(cooldownHours)
                                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
                    });
        }
        if (!dto.isRenewalRequestBlocked()
                && detail.getBook() != null
                && detail.getBorrow() != null
                && detail.getBorrow().getMember() != null) {
            Integer bookId = detail.getBook().getBookId();
            Integer memberId = detail.getBorrow().getMember().getMemberId();
            long waitingReservations = reservationRepository.countActiveReservationsByOtherMemberForBook(
                    bookId, memberId, List.of("PENDING", "DEPOSIT_PAID", "READY", "ACTIVE"));
            long availableCopies = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, "Available");
            if (waitingReservations > availableCopies) {
                dto.setRenewalRequestBlocked(true);
                dto.setRenewalBlockedReason(localizedMessageService.get("backend.renewal.reservedByAnotherMember"));
            }
        }
        return dto;
    }

    private Integer getMemberIdByUsername(String username) {
        return memberAccountRepository.findByUsername(username)
                .map(MemberAccount::getMember)
                .map(Member::getMemberId)
                .orElse(null);
    }

    private void sendInternalNotification(Member member,
            NotificationType type,
            NotificationEventType eventType,
            NotificationSource source,
            String titleKey,
            String contentKey,
            Object... arguments) {
        if (member == null || member.getMemberId() == null) {
            return;
        }
        Notification notif = new Notification();
        localizedMessageService.prepareNotification(notif, titleKey, contentKey, arguments);
        notif.setNotificationType(type);
        notif.setEventType(eventType);
        notif.setNotificationSource(source);
        notif.setCreatedDate(LocalDateTime.now());
        notif.setStatus("Active");
        Notification saved = notificationRepository.save(notif);

        MemberNotification mn = new MemberNotification();
        mn.setId(new MemberNotificationId(member.getMemberId(), saved.getNotificationId()));
        mn.setMember(member);
        mn.setNotification(saved);
        mn.setIsRead(false);
        memberNotificationRepository.save(mn);
    }

    private void sendSuccessfulBorrowNotification(Borrow borrow, List<BorrowDetail> details) {
        String bookNames = details.stream()
                .map(BorrowDetail::getBook)
                .filter(java.util.Objects::nonNull)
                .map(Book::getTitle)
                .collect(Collectors.joining(", "));
        LocalDateTime dueDate = details.stream()
                .map(BorrowDetail::getDueDate)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        sendInternalNotification(borrow.getMember(),
                NotificationType.LOAN, NotificationEventType.LOAN_COLLECTED, NotificationSource.SYSTEM,
                "systemNotification.borrow.success.title",
                "systemNotification.borrow.bankConfirmed.content", bookNames,
                BorrowCodeFormatter.format(borrow.getBorrowId()),
                dueDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private String getAuthorNames(Book book) {
        if (book.getAuthors() == null || book.getAuthors().isEmpty()) {
            return localizedMessageService.get("book.unknownAuthor");
        }
        return book.getAuthors().stream()
                .map(Author::getAuthorName)
                .collect(Collectors.joining(", "));
    }

    private void validateMemberBorrowEligibility(Member member) {
        if (borrowDetailRepository.countByBorrow_Member_MemberIdAndStatusIgnoreCase(member.getMemberId(),
                "Overdue") > 0) {
            throw new ForbiddenException(localizedMessageService.get("backend.borrow.blockedByOverdue"));
        }
        if (!transactionRepository.findUnpaidFineTransactions(member.getMemberId(), List.of("FINE", "DAMAGE_FEE"))
                .isEmpty()) {
            throw new ForbiddenException(localizedMessageService.get("backend.borrow.blockedByUnpaidFine"));
        }
    }

    private int getEffectiveBorrowLimit(Member member) {
        int configuredLimit = getPositiveIntSetting("MAX_BOOKS_PER_MEMBER", 10);
        int tierLimit = member != null && member.getTier() != null && member.getTier().getBorrowLimit() != null
                ? member.getTier().getBorrowLimit()
                : configuredLimit;
        return Math.max(1, tierLimit);
    }

    private int normalizeBorrowDays(Integer requestedDays) {
        int maxBorrowDays = getPositiveIntSetting("MAX_BORROW_DAYS", 14);
        int safeRequestedDays = requestedDays == null || requestedDays <= 0 ? maxBorrowDays : requestedDays;
        return Math.min(safeRequestedDays, maxBorrowDays);
    }

    @Override
    public List<BorrowDetail> getPendingRenewalRequests() {
        return borrowDetailRepository.findByStatus("Renew_Pending");
    }

    @Override
    public BorrowDetail getBorrowDetailById(Integer borrowDetailId) {
        return borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        localizedMessageService.get("backend.loan.detailNotFound")));
    }

    @Override
    public int getMaxBorrowDays() {
        return getPositiveIntSetting("MAX_BORROW_DAYS", 14);
    }

    private int getPositiveIntSetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                    .map(SystemSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .filter(value -> value > 0)
                    .orElse(defaultValue);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitReturnRequest(String username, Integer borrowDetailId) {
        Integer memberId = getMemberIdByUsername(username);
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(
                        () -> new IllegalArgumentException(localizedMessageService.get("backend.loan.detailNotFound")));
        if (memberId == null || detail.getBorrow() == null || detail.getBorrow().getMember() == null
                || !memberId.equals(detail.getBorrow().getMember().getMemberId())) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.return.requestForbidden"));
        }
        if (!"Borrowed".equalsIgnoreCase(detail.getStatus()) && !"Overdue".equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.return.invalidRequestStatus"));
        }

        detail.setStatus("Return_Pending");
        borrowDetailRepository.save(detail);

        Borrow parent = detail.getBorrow();
        parent.setStatus("Return_Pending");
        borrowRepository.save(parent);

        auditLogService.log(
                com.lms.enums.ActionType.REQUEST_RETURN,
                localizedMessageService.get("backend.return.audit.requested", username, parent.getBorrowId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturnRequest(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException(
                        localizedMessageService.get("backend.loan.returnRequestNotFound")));
        borrow.setStatus("Returned");
        borrowRepository.save(borrow);

        for (BorrowDetail detail : getBorrowDetailsByBorrowId(borrowId)) {
            detail.setStatus("Returned");
            detail.setReturnDate(LocalDateTime.now());
            borrowDetailRepository.save(detail);
            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus("Available");

                bookItemRepository.save(item);
            }
        }

        sendInternalNotification(borrow.getMember(),
                NotificationType.LOAN, NotificationEventType.RETURN_CONFIRMED, NotificationSource.LIBRARIAN,
                "systemNotification.return.approved.title",
                "systemNotification.return.approved.content", BorrowCodeFormatter.format(borrowId));
    }

    @Override
    @Transactional(readOnly = true)
    public java.math.BigDecimal calculateBorrowFeePreview(String username, List<Integer> bookIds,
            Integer numberOfDays) {
        if (bookIds == null || bookIds.isEmpty())
            return java.math.BigDecimal.ZERO;

        List<Integer> normalizedBookIds = bookIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedBookIds.isEmpty())
            return java.math.BigDecimal.ZERO;

        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        localizedMessageService.get("backend.member.currentNotFound")));

        int borrowDays = normalizeBorrowDays(numberOfDays);
        java.math.BigDecimal feePerBookPerDay = java.math.BigDecimal
                .valueOf(getPositiveIntSetting("BORROW_FEE_PER_BOOK", 5000));
        double discountPercent = (member.getTier() != null && member.getTier().getDiscountPercent() != null)
                ? member.getTier().getDiscountPercent().doubleValue()
                : 0.0;
        java.math.BigDecimal discountFactor = java.math.BigDecimal.valueOf(1.0 - (discountPercent / 100.0));

        return feePerBookPerDay
                .multiply(java.math.BigDecimal.valueOf(normalizedBookIds.size()))
                .multiply(java.math.BigDecimal.valueOf(borrowDays))
                .multiply(discountFactor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitMultiBookBorrowRequest(String username, List<Integer> bookIds, Integer numberOfDays) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.emptyCart"));
        }

        List<Integer> normalizedBookIds = bookIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedBookIds.isEmpty()) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.emptyCart"));
        }

        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        localizedMessageService.get("backend.member.currentNotFound")));

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed + normalizedBookIds.size() > maxLimit) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.tierLimitExceeded"));
        }

        int borrowDays = normalizeBorrowDays(numberOfDays);

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Pending");
        borrow = borrowRepository.save(borrow);

        List<String> titles = new ArrayList<>();
        for (Integer bookId : normalizedBookIds) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            localizedMessageService.get("backend.inventory.bookNotFound", bookId)));

            if ("Inactive".equalsIgnoreCase(book.getStatus())) {
                throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.bookUnavailable"));
            }

            List<BookItem> items = bookItemRepository.findByBook_BookId(book.getBookId());
            boolean hasAvailableItem = items.stream()
                    .anyMatch(item -> "Available".equalsIgnoreCase(item.getStatus()));
            if (!hasAvailableItem) {
                throw new IllegalArgumentException(
                        localizedMessageService.get("backend.borrow.noAvailableCopy", book.getTitle()));
            }

            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(book);
            detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
            detail.setStatus("Pending");
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            titles.add(book.getTitle());
        }

        auditLogService.log(com.lms.enums.ActionType.REQUEST_BORROW,
                localizedMessageService.get("backend.borrow.audit.multiRequested", username, normalizedBookIds.size(),
                        String.join(", ", titles), borrowDays));

        return borrow;
    }

    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitBankMultiBookBorrowRequest(String username, List<Integer> bookIds, Integer numberOfDays) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.emptyCart"));
        }

        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        localizedMessageService.get("backend.member.currentNotFound")));

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed + bookIds.size() > maxLimit) {
            throw new IllegalArgumentException(localizedMessageService.get("backend.borrow.tierLimitExceeded"));
        }

        int borrowDays = normalizeBorrowDays(numberOfDays);

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus(BorrowServiceImpl.PAYMENT_PENDING);
        borrow = borrowRepository.save(borrow);

        List<String> titles = new ArrayList<>();
        java.util.Set<Integer> validatedBookIds = new java.util.HashSet<>();
        for (Integer bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            localizedMessageService.get("backend.inventory.bookNotFound", bookId)));

            List<BookItem> items = bookItemRepository.findByBook_BookId(book.getBookId());
            boolean hasAvailableItem = items.stream()
                    .anyMatch(item -> "Available".equalsIgnoreCase(item.getStatus()));
            if (!hasAvailableItem) {
                throw new IllegalArgumentException(
                        localizedMessageService.get("backend.borrow.noAvailableCopy", book.getTitle()));
            }

            if (validatedBookIds.add(bookId)) {
                long requestedCopies = java.util.Collections.frequency(bookIds, bookId);
                long availableCopies = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, "Available");
                if (requestedCopies > availableCopies) {
                    throw new ConflictException(
                            localizedMessageService.get("backend.borrow.stockExceeded", availableCopies));
                }
                long activeOrPending = borrowDetailRepository
                        .countActiveOrPendingRequestsByMemberAndBook(member.getMemberId(), bookId);
                if (activeOrPending > 0) {
                    throw new ConflictException(
                            localizedMessageService.get("backend.borrow.duplicateActiveRequest", book.getTitle()));
                }
            }

            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(book);
            detail.setBookItem(null);
            detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
            detail.setStatus(BorrowServiceImpl.PAYMENT_PENDING);
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            titles.add(book.getTitle());
        }

        auditLogService.log(com.lms.enums.ActionType.REQUEST_BORROW,
                localizedMessageService.get("backend.borrow.audit.multiRequested", username, bookIds.size(),
                        String.join(", ", titles), borrowDays));

        return borrow;
    }

}
