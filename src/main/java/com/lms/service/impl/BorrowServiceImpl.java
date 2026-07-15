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
import com.lms.enums.ActionType;
import com.lms.enums.UserStatus;
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
import com.lms.entity.Wallet;
import com.lms.entity.Transaction;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowServiceImpl implements BorrowService {

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
                             TransactionRepository transactionRepository) {
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) {
        String identifier = request.getMemberIdentifier() != null && !request.getMemberIdentifier().isBlank()
                ? request.getMemberIdentifier().trim()
                : request.getMemberEmail();
        Member member = memberRepository.findByUserEmail(identifier)
                .or(() -> memberRepository.findByUserPhone(identifier))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy độc giả với email hoặc số điện thoại này!"));

        if (member.getUser() != null && member.getUser().getStatus() != UserStatus.Active) {
            throw new IllegalArgumentException("Tài khoản thành viên này đang bị khóa hoặc chưa kích hoạt!");
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new IllegalArgumentException("Mã vạch " + barcode + " không tồn tại!"));
            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new IllegalArgumentException("Sách có mã vạch " + barcode + " hiện tại không sẵn sàng!");
            }
            bookItemsToBorrow.add(item);
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowCount + bookItemsToBorrow.size() > maxLimit) {
            throw new IllegalArgumentException("So luong sach vuot qua gioi han muon cua hang thanh vien!");
        }

        int borrowDays = normalizeBorrowDays(request.getNumberOfDays());
        
        // Tính toán số tiền
        double feePerBookPerDay = 5000.0;
        try {
            SystemSetting setting = systemSettingRepository.findBySettingKey("BORROW_FEE_PER_BOOK").orElse(null);
            if (setting != null) feePerBookPerDay = Double.parseDouble(setting.getSettingValue());
        } catch (Exception e) {}
        double baseFee = bookItemsToBorrow.size() * borrowDays * feePerBookPerDay;
        double discount = member.getTier() != null && member.getTier().getDiscountPercent() != null ? member.getTier().getDiscountPercent().doubleValue() : 0.0;
        double finalFeeDouble = baseFee - (baseFee * discount / 100);
        BigDecimal finalFee = BigDecimal.valueOf(finalFeeDouble);

        // Xử lý thanh toán ví nếu user chọn WALLET
        if ("WALLET".equals(request.getPaymentMethod())) {
            Wallet wallet = walletRepository.findByMemberMemberId(member.getMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ví của độc giả!"));
            if (wallet.getBalance().compareTo(finalFee) < 0) {
                throw new IllegalArgumentException("Số dư ví không đủ để thanh toán!");
            }
            wallet.setBalance(wallet.getBalance().subtract(finalFee));
            walletRepository.save(wallet);
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Active");
        borrow = borrowRepository.save(borrow);

        // Ghi lại giao dịch nếu là thanh toán ví
        if ("WALLET".equals(request.getPaymentMethod())) {
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
            detail.setStatus("Borrowed");
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            item.setStatus("Borrowed");
            bookItemRepository.save(item);
        }
        
        // Tạo danh sách tên sách
        String bookNames = bookItemsToBorrow.stream()
                .map(item -> item.getBook().getTitle())
                .collect(java.util.stream.Collectors.joining(", "));
        
        // Gửi thông báo trực tiếp cho độc giả khi tạo phiếu mượn thành công tại quầy
        sendInternalNotification(member, "Mượn sách thành công", 
                "Bạn đã mượn thành công các cuốn sách [" + bookNames + "] tại quầy thông qua mã phiếu mượn BRW-" + borrow.getBorrowId() + ". Vui lòng trả sách đúng hạn vào ngày " + LocalDateTime.now().plusDays(borrowDays).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".");

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin độc giả!"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Sách yêu cầu mượn không tồn tại!"));

        if ("Inactive".equalsIgnoreCase(book.getStatus())) {
            throw new IllegalArgumentException("Sách này hiện không có sẵn để mượn!");
        }

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed >= maxLimit) {
            throw new IllegalArgumentException("Yêu cầu bị từ chối! Bạn đã mượn chạm giới hạn tối đa cho phép (" + maxLimit + " cuốn).");
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
    public void approvePendingRequest(Integer borrowId, String staffUsername) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn yêu cầu mượn!"));
        if (!"Pending".equalsIgnoreCase(borrow.getStatus())) {
            throw new IllegalArgumentException("Đơn mượn không còn ở trạng thái chờ phê duyệt.");
        }

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            BookItem item = detail.getBookItem();
            if (item == null) {
                item = bookItemRepository
                        .findFirstByBook_BookIdAndStatusIgnoreCaseOrderByBookItemIdAsc(
                                detail.getBook().getBookId(), "Available")
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Không còn bản sách khả dụng cho: " + detail.getBook().getTitle()));
                detail.setBookItem(item);
            } else if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new IllegalArgumentException(
                        "Bản sách " + item.getBarcode() + " không còn khả dụng.");
            }

            item.setStatus("Borrowed");
            bookItemRepository.save(item);
            detail.setStatus("Borrowed");
            borrowDetailRepository.save(detail);
        }

        borrow.setStatus("Active");
        borrowRepository.save(borrow);
        
        sendInternalNotification(borrow.getMember(), "Yêu cầu mượn sách đã được phê duyệt",
                "Yêu cầu mượn sách của bạn đã được thủ thư phê duyệt. Vui lòng đến quầy để nhận sách.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitRenewRequest(Integer borrowDetailId) {
        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết phiếu mượn!"));

        if (!"Borrowed".equalsIgnoreCase(detail.getStatus())) {
            throw new IllegalArgumentException("Chỉ sách đang ở trạng thái 'Đang mượn' mới được phép yêu cầu gia hạn!");
        }

        int maxRenewals = getPositiveIntSetting("MAX_RENEWALS", 2);
        if (detail.getRenewCount() != null && detail.getRenewCount() >= maxRenewals) {
            throw new IllegalArgumentException("Sách này đã được gia hạn tối đa " + maxRenewals + " lần!");
        }

        detail.setStatus("Renew_Pending");
        borrowDetailRepository.save(detail);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new IllegalArgumentException("Mã vạch sách vật lý không tồn tại!"));
        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch sử mượn hợp lệ ứng với mã vạch sách này!"));

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
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản độc giả không tồn tại!"));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Sách yêu cầu đặt trước không tồn tại!"));



        boolean alreadyReserved = reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(member.getMemberId())
                .stream()
                .anyMatch(r -> r.getBook() != null
                        && r.getBook().getBookId().equals(bookId)
                        && ("Pending".equalsIgnoreCase(r.getStatus()) || "Active".equalsIgnoreCase(r.getStatus())));
        if (alreadyReserved) {
            throw new IllegalArgumentException("Bạn đã có một yêu cầu đặt trước cuốn sách này và đang chờ xử lý!");
        }

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setStatus("Pending");
        Reservation savedReservation = reservationRepository.save(reservation);
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn yêu cầu đặt trước!"));
        if (!"Pending".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("Đơn đặt trước này đã được xử lý từ trước!");
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn yêu cầu đặt trước!"));
        if (!"Pending".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("Đơn đặt trước này đã được xử lý từ trước!");
        }

        reservation.setStatus("Rejected");
        reservationRepository.save(reservation);
        sendInternalNotification(reservation.getMember(), "Yêu cầu đặt trước bị từ chối",
                "Yêu cầu đặt trước cuốn sách '" + reservation.getBook().getTitle() + "' đã bị từ chối.");
    }

    @Override
    public Reservation getReservationById(Integer reservationId) {
        return reservationRepository.findById(reservationId).orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberCancelReservation(String username, Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn đặt trước không tồn tại!"));
        Integer currentMemberId = getMemberIdByUsername(username);
        if (currentMemberId == null || !reservation.getMember().getMemberId().equals(currentMemberId)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy đơn đặt trước của người khác!");
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
                .filter(r -> "Pending".equalsIgnoreCase(r.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationRequestDTO> getPendingReservationDTOs() {
        return getAllPendingReservations().stream()
                .map(r -> new ReservationRequestDTO(
                        r.getReservationId(),
                        r.getMember() != null && r.getMember().getUser() != null ? r.getMember().getUser().getFullName() : "N/A",
                        r.getBook().getTitle(),
                        r.getReservationDate(),
                        1))
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mượn/trả tương ứng!"));
        borrow.setStatus(status);
        borrowRepository.save(borrow);
    }

    @Override
    @Transactional(readOnly = true)
    public Borrow getBorrowById(Integer borrowId) {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn mượn!"));
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
            if (!"Pending".equalsIgnoreCase(res.getStatus()) && !"Active".equalsIgnoreCase(res.getStatus())) {
                continue;
            }
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(res.getReservationId());
            dto.setBookTitle(res.getBook().getTitle());
            dto.setAuthorName(getAuthorNames(res.getBook()));
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
                .filter(d -> "Returned".equalsIgnoreCase(d.getStatus()))
                .map(this::toMemberBorrowDTO)
                .toList();
    }

    private MemberBorrowDTO toMemberBorrowDTO(BorrowDetail detail) {
        MemberBorrowDTO dto = new MemberBorrowDTO();
        dto.setId(detail.getBorrowDetailId());
        dto.setBookTitle(detail.getBook().getTitle());
        dto.setAuthorName(getAuthorNames(detail.getBook()));
        dto.setBookIdStr(detail.getBookItem() != null ? detail.getBookItem().getBarcode() : "Chưa cấp mã");
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
        return borrowDetailRepository.findById(borrowDetailId).orElse(null);
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
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
