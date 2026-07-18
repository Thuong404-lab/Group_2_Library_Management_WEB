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
import com.lms.enums.UserStatus;
import com.lms.exception.ConflictException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
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
import com.lms.service.FinancialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowServiceImpl implements BorrowService {

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
                             FinancialService financialService) {
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) {
        boolean awaitingBankPayment = "BANK".equalsIgnoreCase(request.getPaymentMethod());
        String identifier = request.getMemberIdentifier() != null && !request.getMemberIdentifier().isBlank()
                ? request.getMemberIdentifier().trim()
                : request.getMemberEmail();
        Member member = memberRepository.findByUserEmail(identifier)
                .or(() -> memberRepository.findByUserPhone(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy độc giả với email hoặc số điện thoại này!"));

        if (member.getUser() != null && member.getUser().getStatus() != UserStatus.Active) {
            throw new ForbiddenException("Tài khoản thành viên này đang bị khóa hoặc chưa kích hoạt!");
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new ResourceNotFoundException("Mã vạch " + barcode + " không tồn tại!"));
            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException("Sách có mã vạch " + barcode + " hiện tại không sẵn sàng!");
            }
            bookItemsToBorrow.add(item);
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowCount + bookItemsToBorrow.size() > maxLimit) {
            throw new ConflictException("Số lượng sách vượt quá giới hạn mượn của hạng thành viên!");
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
        double discount = member.getTier() != null && member.getTier().getDiscountPercent() != null ? member.getTier().getDiscountPercent().doubleValue() : 0.0;
        double finalFeeDouble = baseFee - (baseFee * discount / 100);
        BigDecimal finalFee = BigDecimal.valueOf(finalFeeDouble);

        // Xử lý thanh toán ví nếu user chọn WALLET
        if ("WALLET".equalsIgnoreCase(request.getPaymentMethod())) {
            Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của độc giả!"));
            if (wallet.getBalance().compareTo(finalFee) < 0) {
                throw new ConflictException("Số dư ví không đủ để thanh toán!");
            }
            wallet.setBalance(wallet.getBalance().subtract(finalFee));
            walletRepository.save(wallet);
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus(awaitingBankPayment ? PAYMENT_PENDING : "Active");
        borrow = borrowRepository.save(borrow);

        // Ghi lại giao dịch nếu là thanh toán ví
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
        sendInternalNotification(member, "Mượn sách thành công", 
                "Bạn đã mượn thành công các cuốn sách [" + bookNames + "] tại quầy thông qua mã phiếu mượn BOR-" + borrow.getBorrowId() + ". Vui lòng trả sách đúng hạn vào ngày " + LocalDateTime.now().plusDays(borrowDays).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow activatePendingBankBorrow(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mượn."));
        if ("Active".equalsIgnoreCase(borrow.getStatus())
                || "Borrowing".equalsIgnoreCase(borrow.getStatus())
                || "Overdue".equalsIgnoreCase(borrow.getStatus())) {
            return borrow;
        }
        if (!PAYMENT_PENDING.equalsIgnoreCase(borrow.getStatus())) {
            throw new ConflictException("Phiếu mượn không còn ở trạng thái chờ thanh toán.");
        }

        List<BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrowId);
        if (details.isEmpty()) {
            throw new ConflictException("Phiếu mượn không có chi tiết sách.");
        }

        LocalDateTime paidAt = LocalDateTime.now();
        LocalDateTime pendingAt = borrow.getBorrowDate();
        for (BorrowDetail detail : details) {
            if (!PAYMENT_PENDING.equalsIgnoreCase(detail.getStatus())) {
                throw new ConflictException("Chi tiết phiếu mượn không còn chờ thanh toán.");
            }
            BookItem item = detail.getBookItem();
            if (item == null || !PAYMENT_PENDING.equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException("Sách trong phiếu không còn được giữ cho giao dịch này.");
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin độc giả!"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Sách yêu cầu mượn không tồn tại!"));

        if ("Inactive".equalsIgnoreCase(book.getStatus())) {
            throw new ConflictException("Sách này hiện không có sẵn để mượn!");
        }

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed >= maxLimit) {
            throw new ConflictException("Yêu cầu bị từ chối! Bạn đã mượn chạm giới hạn tối đa cho phép (" + maxLimit + " cuốn).");
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
                "Thành viên " + username + " gửi yêu cầu mượn sách #" + book.getBookId()
                        + " - " + book.getTitle() + " trong " + borrowDays + " ngay.");
                        
        sendInternalNotification(member, "Yêu cầu mượn sách thành công",
                "Yêu cầu mượn cuốn sách '" + book.getTitle() + "' đã được gửi thành công. Vui lòng chờ thủ thư phê duyệt.");

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectPendingRequest(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn yêu cầu mượn!"));
        if (!"Pending".equalsIgnoreCase(borrow.getStatus())) {
            throw new ConflictException("Đơn mượn không còn ở trạng thái chờ phê duyệt.");
        }

        Member member = borrow.getMember();

        // Cập nhật trạng thái Borrow thành Rejected
        borrow.setStatus("Rejected");
        borrowRepository.save(borrow);

        // Cập nhật trạng thái TẤT CẢ BorrowDetail liên quan thành Rejected
        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        String bookNames = details.stream()
                .map(d -> d.getBook().getTitle())
                .collect(Collectors.joining(", "));
        for (BorrowDetail detail : details) {
            detail.setStatus("Rejected");
            borrowDetailRepository.save(detail);
        }

        // Gửi thông báo đến member
        sendInternalNotification(member, "Yêu cầu mượn sách bị từ chối",
                "Yêu cầu mượn sách [" + bookNames + "] (Mã phiếu BOR-" + borrowId + ") của bạn đã bị thủ thư từ chối. "
                + "Nếu có thắc mắc, vui lòng liên hệ thủ thư để biết thêm chi tiết.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePendingRequest(Integer borrowId, String staffUsername) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn yêu cầu mượn!"));
        if (!"Pending".equalsIgnoreCase(borrow.getStatus())) {
            throw new ConflictException("Đơn mượn không còn ở trạng thái chờ phê duyệt.");
        }

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        if (details.isEmpty()) {
            throw new ConflictException("Đơn mượn không có sách nào!");
        }

        Member member = borrow.getMember();

        // ── 1. Gán BookItem và đặt trạng thái Borrowed ──────────────────────
        for (BorrowDetail detail : details) {
            BookItem item = detail.getBookItem();
            if (item == null) {
                item = bookItemRepository
                        .findFirstByBook_BookIdAndStatusIgnoreCaseOrderByBookItemIdAsc(
                                detail.getBook().getBookId(), "Available")
                        .orElseThrow(() -> new ConflictException(
                                "Không còn bản sách khả dụng cho: " + detail.getBook().getTitle()));
                detail.setBookItem(item);
            } else if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new ConflictException(
                        "Bản sách " + item.getBarcode() + " không còn khả dụng.");
            }
            item.setStatus("Waiting_Pickup");
            bookItemRepository.save(item);
            detail.setStatus("Waiting_Pickup");
            borrowDetailRepository.save(detail);
        }

        // ── 2. Tính phí mượn ────────────────────────────────────────────────
        double feePerBookPerDay = 5000.0;
        SystemSetting feeSetting = systemSettingRepository.findBySettingKey("BORROW_FEE_PER_BOOK").orElse(null);
        if (feeSetting != null) {
            try {
                feePerBookPerDay = Double.parseDouble(feeSetting.getSettingValue());
            } catch (NumberFormatException ignored) {}
        }

        int borrowDays = normalizeBorrowDays(null);
        if (details.get(0).getDueDate() != null && borrow.getBorrowDate() != null) {
            long computed = ChronoUnit.DAYS.between(borrow.getBorrowDate(), details.get(0).getDueDate());
            if (computed > 0) borrowDays = (int) computed;
        }

        double discount = member.getTier() != null && member.getTier().getDiscountPercent() != null
                ? member.getTier().getDiscountPercent().doubleValue() : 0.0;
        double baseFee = details.size() * borrowDays * feePerBookPerDay;
        BigDecimal finalFee = BigDecimal.valueOf(baseFee - (baseFee * discount / 100));

        // ── 3. Trừ tiền ví ──────────────────────────────────────────────────
        Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                .orElseThrow(() -> new ConflictException(
                        "Không tìm thấy ví của độc giả! Vui lòng liên hệ thủ thư."));
        if (wallet.getBalance().compareTo(finalFee) < 0) {
            throw new ConflictException(
                    "Số dư ví của độc giả không đủ để thanh toán phí mượn sách ("
                    + String.format("%,.0f", finalFee)
                    + " VND). Vui lòng yêu cầu độc giả nạp thêm tiền vào ví.");
        }
        wallet.setBalance(wallet.getBalance().subtract(finalFee));
        walletRepository.save(wallet);

        // ── 4. Ghi lịch sử giao dịch ────────────────────────────────────────
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setBorrow(borrow);
        transaction.setTransactionType("PAYMENT");
        transaction.setAmount(finalFee.negate());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transactionRepository.save(transaction);

        // ── 5. Cập nhật trạng thái sang Chờ nhận bản vật lý ─────────────────
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
        sendInternalNotification(member, "Yêu cầu mượn sách đã được phê duyệt",
                "Yêu cầu mượn sách [" + bookNames + "] của bạn đã được thủ thư phê duyệt. "
                + "Phí mượn " + String.format("%,.0f", finalFee) + " VND đã được trừ từ ví. "
                + "Vui lòng đến thư viện để nhận sách vật lý.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPhysicalPickup(Integer borrowId, String staffUsername) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mượn!"));
        if (!"Waiting_Pickup".equalsIgnoreCase(borrow.getStatus())) {
            throw new ConflictException("Phiếu mượn không ở trạng thái chờ nhận sách vật lý.");
        }

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        if (details.isEmpty()) {
            throw new ConflictException("Phiếu mượn không có sách nào!");
        }

        // Tính số ngày mượn từ dueDate ban đầu so với borrowDate cũ
        int borrowDays = normalizeBorrowDays(null);
        if (details.get(0).getDueDate() != null && borrow.getBorrowDate() != null) {
            long computed = ChronoUnit.DAYS.between(borrow.getBorrowDate(), details.get(0).getDueDate());
            if (computed > 0) borrowDays = (int) computed;
        }

        // Bắt đầu tính thời gian từ thời điểm nhận sách thực tế
        LocalDateTime pickupTime = LocalDateTime.now();
        for (BorrowDetail detail : details) {
            detail.setStatus("Borrowed");
            detail.setDueDate(pickupTime.plusDays(borrowDays));
            borrowDetailRepository.save(detail);

            // Đảm bảo BookItem đang ở trạng thái Borrowed
            if (detail.getBookItem() != null) {
                detail.getBookItem().setStatus("Borrowed");
                bookItemRepository.save(detail.getBookItem());
            }
        }

        // Cập nhật ngày mượn thực tế và trạng thái Active
        borrow.setBorrowDate(pickupTime);
        borrow.setStatus("Active");
        borrowRepository.save(borrow);

        String bookNames = details.stream()
                .map(d -> d.getBook().getTitle())
                .collect(Collectors.joining(", "));
        sendInternalNotification(borrow.getMember(), "Đã nhận sách thành công",
                "Bạn đã nhận sách [" + bookNames + "] thành công. "
                + "Thời hạn trả sách là " + borrowDays + " ngày kể từ hôm nay. "
                + "Vui lòng trả sách đúng hạn vào ngày "
                + pickupTime.plusDays(borrowDays).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitRenewRequest(Integer borrowDetailId) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết phiếu mượn!"));

        if (!"Borrowed".equalsIgnoreCase(detail.getStatus())) {
            throw new ConflictException("Chỉ sách đang ở trạng thái 'Đang mượn' mới được phép yêu cầu gia hạn!");
        }

        int maxRenewals = getPositiveIntSetting("MAX_RENEWALS", 2);
        if (detail.getRenewCount() != null && detail.getRenewCount() >= maxRenewals) {
            throw new ConflictException("Sách này đã được gia hạn tối đa " + maxRenewals + " lần!");
        }

        detail.setStatus("Renew_Pending");
        borrowDetailRepository.save(detail);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Mã vạch sách vật lý không tồn tại!"));
        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử mượn hợp lệ ứng với mã vạch sách này!"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản độc giả không tồn tại!"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Sách yêu cầu đặt trước không tồn tại!"));



        boolean alreadyReserved = reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(member.getMemberId())
                .stream()
                .anyMatch(r -> r.getBook() != null
                        && r.getBook().getBookId().equals(bookId)
                        && ("Pending".equalsIgnoreCase(r.getStatus())
                        || "Deposit_Paid".equalsIgnoreCase(r.getStatus())
                        || "Refund_Pending".equalsIgnoreCase(r.getStatus())
                        || "Ready".equalsIgnoreCase(r.getStatus())
                        || "Active".equalsIgnoreCase(r.getStatus())));
        if (alreadyReserved) {
            throw new ConflictException("Bạn đã có một yêu cầu đặt trước cuốn sách này và đang chờ xử lý!");
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
                "Member " + username + " gui yeu cau dat truoc sach #" + book.getBookId()
                        + " - " + book.getTitle() + ".");
        return savedReservation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReservationRequest(Integer reservationId, String staffUsername) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn yêu cầu đặt trước!"));
        if (!"Pending".equalsIgnoreCase(reservation.getStatus())
                && !"Deposit_Paid".equalsIgnoreCase(reservation.getStatus())) {
            throw new ConflictException("Đơn đặt trước này đã được xử lý từ trước!");
        }

        reservation.setStatus("Active");
        reservationRepository.save(reservation);
        sendInternalNotification(reservation.getMember(), "Yêu cầu đặt trước được phê duyệt",
                "Cuốn sách '" + reservation.getBook().getTitle() + "' đã được đặt giữ thành công.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReservationRequest(Integer reservationId, String staffUsername) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn yêu cầu đặt trước!"));

        String currentStatus = reservation.getStatus();
        boolean isPending = "Pending".equalsIgnoreCase(currentStatus);
        boolean isDepositPaid = "Deposit_Paid".equalsIgnoreCase(currentStatus);

        if (!isPending && !isDepositPaid) {
            throw new ConflictException("Đơn đặt trước này đã được xử lý từ trước (trạng thái: " + currentStatus + ")!");
        }

        // Nếu member đã nộp cọc → hoàn tiền cọc về ví
        if (isDepositPaid) {
            Member member = reservation.getMember();
            Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của thành viên."));
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

                sendInternalNotification(member, "Hoàn tiền cọc đặt trước",
                        "Thư viện đã hoàn " + refundAmount.toPlainString() + " VNĐ tiền cọc đặt trước sách '"
                        + reservation.getBook().getTitle() + "' về ví của bạn do đơn đặt trước bị từ chối.");
            }
        }

        reservation.setStatus("Rejected");
        reservationRepository.save(reservation);
        sendInternalNotification(reservation.getMember(), "Yêu cầu đặt trước bị từ chối",
                "Yêu cầu đặt trước cuốn sách '" + reservation.getBook().getTitle() + "' (Mã RSV-" + reservationId + ") của bạn đã bị thủ thư từ chối."
                + (isDepositPaid ? " Tiền cọc đã được hoàn vào ví của bạn." : ""));
    }

    @Override
    public Reservation getReservationById(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt trước."));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberCancelReservation(String username, Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt trước không tồn tại!"));
        Integer currentMemberId = getMemberIdByUsername(username);
        if (currentMemberId == null || !reservation.getMember().getMemberId().equals(currentMemberId)) {
            throw new ForbiddenException("Bạn không có quyền hủy đơn đặt trước của người khác!");
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
                        (ex, rep) -> ex
                ));
        return getAllPendingReservations().stream()
                .map(r -> {
                    String email = r.getMember() != null && r.getMember().getUser() != null ? r.getMember().getUser().getEmail() : "";
                    String phone = r.getMember() != null && r.getMember().getUser() != null ? r.getMember().getUser().getPhone() : "";
                    String username = r.getMember() != null ? usernameMap.getOrDefault(r.getMember().getMemberId(), "") : "";
                    return new ReservationRequestDTO(
                            r.getReservationId(),
                            r.getMember() != null && r.getMember().getUser() != null ? r.getMember().getUser().getFullName() : "N/A",
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
        return id == null ? new ArrayList<>() : borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null && id.equals(b.getMember().getMemberId()) && status.equalsIgnoreCase(b.getStatus()))
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn mượn/trả tương ứng!"));
        borrow.setStatus(status);
        borrowRepository.save(borrow);
    }

    @Override
    @Transactional(readOnly = true)
    public Borrow getBorrowById(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn mượn!"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId) {
        return borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllBorrowHistoryByMember(String username) {
        Integer id = getMemberIdByUsername(username);
        return id == null ? new ArrayList<>() : borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null && id.equals(b.getMember().getMemberId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberCurrentBorrows(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) return new ArrayList<>();
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
        if (memberId == null) return new ArrayList<>();

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
        if (memberId == null) return new ArrayList<>();
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
        dto.setBookTitle(detail.getBook().getTitle());
        dto.setAuthorName(getAuthorNames(detail.getBook()));
        dto.setBookImage(detail.getBook().getCoverImageUrl());
        dto.setBookIdStr(detail.getBookItem() != null ? detail.getBookItem().getBarcode() : "Chưa cấp mã");
        if (detail.getBorrow() != null) {
            dto.setBorrowIdStr("BOR-" + detail.getBorrow().getBorrowId());
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
        return dto;
    }

    private Integer getMemberIdByUsername(String username) {
        return memberAccountRepository.findByUsername(username)
                .map(MemberAccount::getMember)
                .map(Member::getMemberId)
                .orElse(null);
    }

    private void sendInternalNotification(Member member, String title, String content) {
        if (member == null || member.getMemberId() == null) {
            return;
        }
        Notification notif = new Notification();
        notif.setTitle(title);
        notif.setContent(content);
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
        sendInternalNotification(borrow.getMember(), "Mượn sách thành công",
                "Ngân hàng đã xác nhận thanh toán. Bạn đã mượn thành công các cuốn sách [" + bookNames
                        + "] tại quầy thông qua mã phiếu mượn BOR-" + borrow.getBorrowId()
                        + ". Vui lòng trả sách đúng hạn vào ngày "
                        + dueDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");
    }

    private String getAuthorNames(Book book) {
        if (book.getAuthors() == null || book.getAuthors().isEmpty()) {
            return "Chua ro tac gia";
        }
        return book.getAuthors().stream()
                .map(Author::getAuthorName)
                .collect(Collectors.joining(", "));
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết phiếu mượn."));
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
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay chi tiet phieu muon tuong ung!"));
        if (memberId == null || detail.getBorrow() == null || detail.getBorrow().getMember() == null
                || !memberId.equals(detail.getBorrow().getMember().getMemberId())) {
            throw new IllegalArgumentException("Ban khong co quyen gui yeu cau tra sach nay!");
        }
        if (!"Borrowed".equalsIgnoreCase(detail.getStatus()) && !"Overdue".equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException("Trang thai sach hien tai khong hop le de gui yeu cau tra!");
        }

        detail.setStatus("Return_Pending");
        borrowDetailRepository.save(detail);

        Borrow parent = detail.getBorrow();
        parent.setStatus("Return_Pending");
        borrowRepository.save(parent);

        auditLogService.log(
                com.lms.enums.ActionType.REQUEST_RETURN,
                "Member " + username + " gui yeu cau tra sach #" + parent.getBorrowId() + ".");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturnRequest(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don yeu cau tra!"));
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

        sendInternalNotification(borrow.getMember(), "Xac nhan tra sach thanh cong",
                "Yeu cau tra sach cua phieu muon #" + borrowId + " da duoc thu thu phe duyet.");
    }

    @Override
    @Transactional(readOnly = true)
    public java.math.BigDecimal calculateBorrowFeePreview(String username, List<Integer> bookIds, Integer numberOfDays) {
        if (bookIds == null || bookIds.isEmpty()) return java.math.BigDecimal.ZERO;

        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin độc giả!"));

        int borrowDays = normalizeBorrowDays(numberOfDays);
        java.math.BigDecimal feePerBookPerDay = java.math.BigDecimal.valueOf(getPositiveIntSetting("FEE_PER_BOOK_PER_DAY", 5000));
        double discountPercent = (member.getTier() != null && member.getTier().getDiscountPercent() != null)
                ? member.getTier().getDiscountPercent().doubleValue() : 0.0;
        java.math.BigDecimal discountFactor = java.math.BigDecimal.valueOf(1.0 - (discountPercent / 100.0));

        return feePerBookPerDay
                .multiply(java.math.BigDecimal.valueOf(bookIds.size()))
                .multiply(java.math.BigDecimal.valueOf(borrowDays))
                .multiply(discountFactor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitMultiBookBorrowRequest(String username, List<Integer> bookIds, Integer numberOfDays) {
        if (bookIds == null || bookIds.isEmpty()) {
            throw new IllegalArgumentException("Giỏ sách trống! Vui lòng chọn ít nhất một cuốn sách.");
        }

        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin độc giả!"));

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed + bookIds.size() > maxLimit) {
            throw new IllegalArgumentException("Yêu cầu bị từ chối! Số lượng sách vượt quá giới hạn.");
        }

        int borrowDays = normalizeBorrowDays(numberOfDays);

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Pending");
        borrow = borrowRepository.save(borrow);

        List<String> titles = new ArrayList<>();
        for (Integer bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Sách mang mã số #" + bookId + " không tồn tại!"));

            List<BookItem> items = bookItemRepository.findByBook_BookId(book.getBookId());
            boolean hasAvailableItem = items.stream()
                    .anyMatch(item -> "Available".equalsIgnoreCase(item.getStatus()));
            if (!hasAvailableItem) {
                throw new IllegalArgumentException("Đầu sách '" + book.getTitle() + "' hiện tại đã hết bản vật lý sẵn sàng!");
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
                "Độc giả " + username + " đã đăng ký mượn " + bookIds.size() + " cuốn sách: "
                        + String.join(", ", titles) + " trong vòng " + borrowDays + " ngày. Đang chờ thủ thư phê duyệt.");

        return borrow;
    }
}
