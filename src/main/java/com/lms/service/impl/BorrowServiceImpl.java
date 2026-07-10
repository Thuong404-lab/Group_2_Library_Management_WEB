package com.lms.service.impl;

import com.lms.dto.request.BorrowRequest;
import com.lms.dto.response.MemberBorrowDTO;
import com.lms.dto.response.ReservationRequestDTO;
import com.lms.dto.response.ReturnRequestDTO;
import com.lms.entity.*;
import com.lms.enums.UserStatus;
import com.lms.repository.*;
import com.lms.service.BorrowService;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BorrowServiceImpl implements BorrowService {

    private static final String STATUS_AVAILABLE = "Available";
    private static final String STATUS_BORROWED = "Borrowed";
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_RETURN_PENDING = "Return_Pending";
    private static final String STATUS_RETURNED = "Returned";
    private static final String STATUS_OVERDUE = "Overdue";
    private static final String UNKNOWN_AUTHOR = "Chưa rõ tác giả";

    private final MemberRepository memberRepository;
    private final BookItemRepository bookItemRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final BookRepository bookRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public BorrowServiceImpl(MemberRepository memberRepository,
            BookItemRepository bookItemRepository,
            BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            BookRepository bookRepository,
            MemberAccountRepository memberAccountRepository,
            ReservationRepository reservationRepository,
            NotificationRepository notificationRepository,
            MemberNotificationRepository memberNotificationRepository) {
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookRepository = bookRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.reservationRepository = reservationRepository;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    // ==========================================
    // NGHIỆP VỤ MƯỢN SÁCH (BORROW LOGIC)
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBorrowing(BorrowRequest request, String librarianUsername) {
        String identifier = request.getMemberIdentifier();
        Member member = memberRepository.findByUserEmail(identifier)
                .orElseGet(() -> memberRepository.findByUserPhone(identifier).orElse(null));

        if (member == null) {
            throw new ResourceNotFoundException("Không tìm thấy độc giả với email hoặc số điện thoại này!");
        }

        if (member.getUser() != null && member.getUser().getStatus() != UserStatus.Active) {
            throw new ValidationException("Tài khoản thành viên này đang bị khóa hoặc chưa kích hoạt!");
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new ResourceNotFoundException("Mã vạch " + barcode + " không tồn tại!"));

            if (!STATUS_AVAILABLE.equalsIgnoreCase(item.getStatus())) {
                throw new ValidationException("Sách có mã vạch " + barcode + " hiện tại không sẵn sàng!");
            }
            bookItemsToBorrow.add(item);
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        if (currentBorrowCount + bookItemsToBorrow.size() > maxLimit) {
            throw new ValidationException("Số lượng sách vượt quá giới hạn mượn của hạng thành viên!");
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
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
    public void memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin độc giả!"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Sách yêu cầu mượn không tồn tại!"));

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        if (currentBorrowed >= maxLimit) {
            throw new ValidationException("Yêu cầu bị từ chối! Bạn đã mượn chạm giới hạn tối đa cho phép.");
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus(STATUS_PENDING);
        borrow = borrowRepository.save(borrow);

        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBook(book);
        detail.setBookItem(null);
        detail.setDueDate(LocalDateTime.now().plusDays(numberOfDays != null ? numberOfDays : 14));
        detail.setStatus(STATUS_PENDING);
        detail.setRenewCount(0);
        borrowDetailRepository.save(detail);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePendingRequest(Integer borrowId, String staffUsername) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn yêu cầu mượn!"));

        borrow.setStatus(STATUS_ACTIVE);
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .toList();

        for (BorrowDetail detail : details) {
            detail.setStatus(STATUS_BORROWED);
            borrowDetailRepository.save(detail);
        }
    }

    // ==========================================
    // LUỒNG NGHIỆP VỤ TRẢ SÁCH (RETURN LOGIC)
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitReturnRequest(String username, Integer borrowDetailId) {
        memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Độc giả không tồn tại!"));

        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết phiếu mượn tương ứng!"));

        if (!STATUS_BORROWED.equalsIgnoreCase(detail.getStatus())
                && !STATUS_OVERDUE.equalsIgnoreCase(detail.getStatus())) {
            throw new ValidationException("Trạng thái sách hiện tại không hợp lệ để gửi yêu cầu trả!");
        }

        detail.setStatus(STATUS_RETURN_PENDING);
        borrowDetailRepository.save(detail);

        Borrow parent = detail.getBorrow();
        parent.setStatus(STATUS_RETURN_PENDING);
        borrowRepository.save(parent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturnRequest(Integer borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn yêu cầu trả!"));

        borrow.setStatus(STATUS_RETURNED);
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .toList();

        for (BorrowDetail detail : details) {
            detail.setStatus(STATUS_RETURNED);
            detail.setReturnDate(LocalDateTime.now());
            borrowDetailRepository.save(detail);

            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus(STATUS_AVAILABLE);
                bookItemRepository.save(item);
            }
        }

        sendInternalNotification(borrow.getMember(), "Xác nhận trả sách thành công",
                "Yêu cầu trả sách của phiếu mượn #" + borrowId + " đã được thủ thư phê duyệt.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Mã vạch sách vật lý không tồn tại!"));

        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && (STATUS_BORROWED.equalsIgnoreCase(d.getStatus())
                                || STATUS_OVERDUE.equalsIgnoreCase(d.getStatus())
                                || STATUS_RETURN_PENDING.equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lịch sử mượn hợp lệ ứng với mã vạch sách này!"));

        item.setStatus(STATUS_AVAILABLE);
        bookItemRepository.save(item);

        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus(STATUS_RETURNED);
        borrowDetailRepository.save(activeDetail);

        Borrow parent = activeDetail.getBorrow();
        parent.setStatus(STATUS_RETURNED);
        borrowRepository.save(parent);
    }

    // FIX LỖI DÒNG DÒNG 243-244 TRONG HÌNH: Truy cập bắc cầu qua đối tượng User
    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getPendingReturnRequestDTOs() {
        List<BorrowDetail> details = borrowDetailRepository.findByStatus(STATUS_RETURN_PENDING);
        return details.stream().map(bd -> {
            Member member = bd.getBorrow().getMember();
            String name = (member != null && member.getUser() != null) ? member.getUser().getFullName() : "N/A";
            String email = (member != null && member.getUser() != null) ? member.getUser().getEmail() : "N/A";

            return new ReturnRequestDTO(
                    bd.getBorrowDetailId(),
                    name,
                    email,
                    bd.getBook().getTitle(),
                    bd.getBookItem() != null ? bd.getBookItem().getBarcode() : "N/A",
                    bd.getBorrow().getBorrowDate());
        }).toList();
    }

    // ==========================================
    // LUỒNG NGHIỆP VỤ ĐẶT TRƯỚC (RESERVATION LOGIC)
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitReservationRequest(String username, Integer bookId) {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản độc giả không tồn tại!"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Sách yêu cầu đặt giữ không tồn tại!"));

        boolean alreadyReserved = reservationRepository
                .findByMember_MemberIdOrderByReservationDateDesc(member.getMemberId())
                .stream()
                .anyMatch(
                        r -> r.getBook().getBookId().equals(bookId) && STATUS_PENDING.equalsIgnoreCase(r.getStatus()));
        if (alreadyReserved) {
            throw new ValidationException("Bạn đã có một yêu cầu đặt trước cuốn sách này và đang chờ duyệt!");
        }

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setStatus(STATUS_PENDING);
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReservationRequest(Integer reservationId, String staffUsername) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn yêu cầu đặt trước!"));

        if (!STATUS_PENDING.equalsIgnoreCase(reservation.getStatus())) {
            throw new ValidationException("Đơn đặt trước này đã được xử lý từ trước!");
        }

        reservation.setStatus(STATUS_ACTIVE);
        reservationRepository.save(reservation);

        sendInternalNotification(reservation.getMember(), "Yêu cầu đặt trước được phê duyệt",
                "Cuốn sách '" + reservation.getBook().getTitle()
                        + "' đã được đặt giữ thành công. Vui lòng đến nhận trong vòng 3 ngày.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberCancelReservation(String username, Integer reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn đặt trước không tồn tại!"));

        Integer currentMemberId = getMemberIdByUsername(username);

        if (!reservation.getMember().getMemberId().equals(currentMemberId)) {
            throw new ValidationException("Bạn không có quyền hủy đơn đặt trước của người khác!");
        }

        reservation.setStatus("Canceled");
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getAllPendingReservations() {
        return reservationRepository.findAll().stream()
                .filter(r -> STATUS_PENDING.equalsIgnoreCase(r.getStatus()))
                .toList();
    }

    // FIX LỖI DÒNG 328-331 TRONG HÌNH: Sửa lỗi lấy FullName qua User và gán cứng
    // hàng đợi mặc định nếu entity thiếu trường
    @Override
    @Transactional(readOnly = true)
    public List<ReservationRequestDTO> getPendingReservationDTOs() {
        List<Reservation> reservations = reservationRepository.findAll().stream()
                .filter(r -> STATUS_PENDING.equalsIgnoreCase(r.getStatus()))
                .toList();
        return reservations.stream().map(r -> {
            Member member = r.getMember();
            String name = (member != null && member.getUser() != null) ? member.getUser().getFullName() : "N/A";

            return new ReservationRequestDTO(
                    r.getReservationId(),
                    name,
                    r.getBook().getTitle(),
                    r.getReservationDate(),
                    1 // Vì Entity Reservation không định nghĩa queuePosition, gán cứng giá trị an
                      // toàn '1' để tránh lỗi compile
            );
        }).toList();
    }

    // ==========================================
    // ĐỒNG BỘ HIỂN THỊ DỮ LIỆU FRONT-END TAB
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberCurrentBorrows(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null)
            return new ArrayList<>();

        List<BorrowDetail> details = borrowDetailRepository.findCurrentBorrowsByMemberId(memberId).stream()
                .filter(d -> STATUS_PENDING.equalsIgnoreCase(d.getStatus())
                        || STATUS_BORROWED.equalsIgnoreCase(d.getStatus())
                        || STATUS_OVERDUE.equalsIgnoreCase(d.getStatus())
                        || STATUS_RETURN_PENDING.equalsIgnoreCase(d.getStatus()))
                .toList();

        List<MemberBorrowDTO> dtoList = new ArrayList<>();
        for (BorrowDetail detail : details) {
            MemberBorrowDTO dto = mapToMemberBorrowDTO(detail);

            if (detail.getBorrow().getBorrowDate() != null && detail.getDueDate() != null) {
                long totalDays = ChronoUnit.DAYS.between(detail.getBorrow().getBorrowDate(), detail.getDueDate());
                long elapsedDays = ChronoUnit.DAYS.between(detail.getBorrow().getBorrowDate(), LocalDateTime.now());
                long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), detail.getDueDate());
                dto.setDaysLeft(Math.max(0, daysLeft));
                int progress = totalDays > 0 ? (int) ((elapsedDays * 100) / totalDays) : 0;
                dto.setProgressPercentage(Math.min(100, Math.max(0, progress)));
            }
            dto.setRenewCount(detail.getRenewCount() != null ? detail.getRenewCount() : 0);
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberReservations(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null)
            return new ArrayList<>();

        List<Reservation> reservations = reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(memberId)
                .stream()
                .filter(r -> STATUS_PENDING.equalsIgnoreCase(r.getStatus())
                        || STATUS_ACTIVE.equalsIgnoreCase(r.getStatus()))
                .toList();

        List<MemberBorrowDTO> dtoList = new ArrayList<>();
        for (Reservation res : reservations) {
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(res.getReservationId());
            dto.setBookTitle(res.getBook().getTitle());
            dto.setAuthorName(getAuthorNames(res.getBook()));
            dto.setBookIdStr("RES-" + res.getBook().getBookId());
            dto.setActionDate(res.getReservationDate());
            dto.setDueDate(res.getReservationDate().plusDays(3));
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
        List<BorrowDetail> details = borrowDetailRepository.findBorrowHistoryInOneMonth(memberId, oneMonthAgo).stream()
                .filter(d -> STATUS_RETURNED.equalsIgnoreCase(d.getStatus()))
                .toList();

        List<MemberBorrowDTO> dtoList = new ArrayList<>();
        for (BorrowDetail detail : details) {
            MemberBorrowDTO dto = mapToMemberBorrowDTO(detail);
            dto.setReturnDate(detail.getReturnDate());
            dtoList.add(dto);
        }
        return dtoList;
    }

    // ==========================================
    private MemberBorrowDTO mapToMemberBorrowDTO(BorrowDetail detail) {
        MemberBorrowDTO dto = new MemberBorrowDTO();
        dto.setId(detail.getBorrowDetailId());
        dto.setBookTitle(detail.getBook().getTitle());
        dto.setAuthorName(getAuthorNames(detail.getBook()));
        dto.setBookIdStr(detail.getBookItem() != null ? detail.getBookItem().getBarcode()
                : "BK-" + detail.getBook().getBookId());
        dto.setActionDate(detail.getBorrow().getBorrowDate());
        dto.setStatus(detail.getStatus());
        return dto;
    }

    private String getAuthorNames(Book book) {
        return book.getAuthors() != null && !book.getAuthors().isEmpty()
                ? book.getAuthors().stream().map(Author::getAuthorName).collect(Collectors.joining(", "))
                : UNKNOWN_AUTHOR;
    }

    // CÁC PHƯƠNG THỨC TRUY VẤN BỔ TRỢ KHÁC
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllPendingRequests() {
        return borrowRepository.findAll().stream().filter(b -> STATUS_PENDING.equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllReturnRequests() {
        return borrowRepository.findAll().stream().filter(b -> STATUS_RETURN_PENDING.equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllActiveLoans() {
        return borrowRepository.findAll().stream()
                .filter(b -> STATUS_ACTIVE.equalsIgnoreCase(b.getStatus())
                        || "Borrowing".equalsIgnoreCase(b.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getBorrowsByMemberAndStatus(String username, String status) {
        Integer id = getMemberIdByUsername(username);
        return id == null ? new ArrayList<>()
                : borrowRepository.findAll().stream().filter(b -> b.getMember() != null
                        && id.equals(b.getMember().getMemberId()) && status.equalsIgnoreCase(b.getStatus()))
                        .toList();
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
    public Borrow getBorrowById(Integer id) {
        return borrowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn mượn!"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer id) {
        return borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(id))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, String status) {
        Borrow b = borrowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn mượn!"));
        b.setStatus(status);
        borrowRepository.save(b);
    }

    private Integer getMemberIdByUsername(String username) {
        return memberAccountRepository.findByUsername(username).map(MemberAccount::getMember).map(Member::getMemberId)
                .orElse(null);
    }

    private void sendInternalNotification(Member member, String title, String content) {
        Notification notif = new Notification();
        notif.setTitle(title);
        notif.setContent(content);
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
}
