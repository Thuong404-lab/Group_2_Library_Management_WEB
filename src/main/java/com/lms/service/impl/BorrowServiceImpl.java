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
import com.lms.service.AuditLogService;
import com.lms.service.BorrowService;
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
    private final ReservationRepository reservationRepository;
    private final AuditLogService auditLogService;
    private final NotificationRepository notificationRepository;
    private final MemberNotificationRepository memberNotificationRepository;

    public BorrowServiceImpl(MemberRepository memberRepository,
            BookItemRepository bookItemRepository,
            BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            BookRepository bookRepository,
            MemberAccountRepository memberAccountRepository,
            ReservationRepository reservationRepository,
            AuditLogService auditLogService,
            NotificationRepository notificationRepository,
            MemberNotificationRepository memberNotificationRepository) {
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookRepository = bookRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.reservationRepository = reservationRepository;
        this.auditLogService = auditLogService;
        this.notificationRepository = notificationRepository;
        this.memberNotificationRepository = memberNotificationRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception {
        String identifier = request.getMemberIdentifier();
        Member member = memberRepository.findByUserEmail(identifier)
                .orElseGet(() -> memberRepository.findByUserPhone(identifier).orElse(null));

        if (member == null) {
            throw new Exception("Không tìm thấy độc giả với email hoặc số điện thoại này!");
        }

        if (member.getUser() != null && member.getUser().getStatus() != UserStatus.Active) {
            throw new Exception("Tài khoản thành viên này đang bị khóa hoặc chưa kích hoạt!");
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new Exception("Mã vạch " + barcode + " không tồn tại!"));

            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new Exception("Sách có mã vạch " + barcode + " hiện tại không sẵn sàng!");
            }
            bookItemsToBorrow.add(item);
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        if (currentBorrowCount + bookItemsToBorrow.size() > maxLimit) {
            throw new Exception("Số lượng sách vượt quá giới hạn mượn của hạng thành viên!");
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Active");
        borrow = borrowRepository.save(borrow);

        for (BookItem item : bookItemsToBorrow) {
            BorrowDetail detail = new BorrowDetail();
            detail.setBorrow(borrow);
            detail.setBook(item.getBook());
            detail.setBookItem(item);
            detail.setDueDate(LocalDateTime.now().plusDays(14));
            detail.setStatus("Borrowed");
            detail.setRenewCount(0);
            borrowDetailRepository.save(detail);

            item.setStatus("Borrowed");
            bookItemRepository.save(item);
        }
        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) throws Exception {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new Exception("Không tìm thấy thông tin độc giả!"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new Exception("Sách yêu cầu mượn không tồn tại!"));

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = member.getTier() != null ? member.getTier().getBorrowLimit() : 3;
        if (currentBorrowed >= maxLimit) {
            throw new Exception(
                    "Yêu cầu bị từ chối! Bạn đã mượn chạm giới hạn tối đa cho phép (" + maxLimit + " cuốn).");
        }

        Borrow borrow = new Borrow();
        borrow.setMember(member);
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setStatus("Pending");
        borrow = borrowRepository.save(borrow);

        BorrowDetail detail = new BorrowDetail();
        detail.setBorrow(borrow);
        detail.setBook(book);
        detail.setBookItem(null);
        detail.setDueDate(LocalDateTime.now().plusDays(numberOfDays != null ? numberOfDays : 14));
        detail.setStatus("Pending");
        detail.setRenewCount(0);
        borrowDetailRepository.save(detail);

        auditLogService.log(
                ActionType.REQUEST_BORROW,
                "Member " + username + " gửi yêu cầu mượn sách #" + book.getBookId()
                        + " - " + book.getTitle() + " trong " + (numberOfDays != null ? numberOfDays : 14) + " ngày.");

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvePendingRequest(Integer borrowId, String staffUsername) throws Exception {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn yêu cầu mượn!"));

        borrow.setStatus("Active");
        borrowRepository.save(borrow);

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus("Borrowed");
            borrowDetailRepository.save(detail);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberSubmitReturnRequest(String username, Integer borrowDetailId) throws Exception {
        memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new Exception("Độc giả không tồn tại!"));

        BorrowDetail detail = borrowDetailRepository.findById(borrowDetailId)
                .orElseThrow(() -> new Exception("Không tìm thấy chi tiết phiếu mượn tương ứng!"));

        if (!"Borrowed".equalsIgnoreCase(detail.getStatus()) && !"Overdue".equalsIgnoreCase(detail.getStatus())) {
            throw new Exception("Trạng thái sách hiện tại không hợp lệ để gửi yêu cầu trả!");
        }

        detail.setStatus("Return_Pending");
        borrowDetailRepository.save(detail);

        Borrow parent = detail.getBorrow();
        parent.setStatus("Return_Pending");
        borrowRepository.save(parent);

        auditLogService.log(
                ActionType.REQUEST_RETURN,
                "Member " + username + " gửi yêu cầu trả sách #" + parent.getBorrowId() + ".");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturnRequest(Integer borrowId) throws Exception {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn yêu cầu trả!"));

        borrow.setStatus("Returned");
        borrowRepository.save(borrow);

        List<BorrowDetail> details = getBorrowDetailsByBorrowId(borrowId);
        for (BorrowDetail detail : details) {
            detail.setStatus("Returned");
            detail.setReturnDate(LocalDateTime.now());
            borrowDetailRepository.save(detail);

            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus("Available");
                bookItemRepository.save(item);
            }
        }

        sendInternalNotification(borrow.getMember(), "Xác nhận trả sách thành công",
                "Yêu cầu trả sách của phiếu mượn #" + borrowId + " đã được thủ thư phê duyệt.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) throws Exception {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new Exception("Mã vạch sách vật lý không tồn tại!"));

        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus())
                                || "Overdue".equalsIgnoreCase(d.getStatus())
                                || "Return_Pending".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new Exception("Không tìm thấy lịch sử mượn hợp lệ ứng với mã vạch sách này!"));

        item.setStatus("Available");
        bookItemRepository.save(item);

        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus("Returned");
        borrowDetailRepository.save(activeDetail);

        List<BorrowDetail> sameBorrowDetails = getBorrowDetailsByBorrowId(activeDetail.getBorrow().getBorrowId());
        boolean allReturned = sameBorrowDetails.stream().allMatch(d -> "Returned".equalsIgnoreCase(d.getStatus()));
        if (allReturned) {
            Borrow parent = activeDetail.getBorrow();
            parent.setStatus("Returned");
            borrowRepository.save(parent);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer loanId, String status) throws Exception {
        Borrow borrow = borrowRepository.findById(loanId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn mượn/trả tương ứng!"));
        borrow.setStatus(status);
        borrowRepository.save(borrow);

        if ("Return_Pending".equalsIgnoreCase(status)) {
            auditLogService.log(
                    ActionType.REQUEST_RETURN,
                    "Member gửi yêu cầu trả sách #" + borrow.getBorrowId() + ".");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getPendingReturnRequestDTOs() {
        List<BorrowDetail> details = borrowDetailRepository.findByStatus("Return_Pending");
        return details.stream().map(bd -> {
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
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation memberSubmitReservationRequest(String username, Integer bookId) throws Exception {
        Member member = memberRepository.findByAccountUsername(username)
                .orElseThrow(() -> new Exception("Tài khoản độc giả không tồn tại!"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new Exception("Sách yêu cầu đặt giữ không tồn tại!"));

        boolean alreadyReserved = reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(member.getMemberId())
                .stream()
                .anyMatch(r -> r.getBook().getBookId().equals(bookId) && "Pending".equalsIgnoreCase(r.getStatus()));
        if (alreadyReserved) {
            throw new Exception("Bạn đã có một yêu cầu đặt trước cuốn sách này và đang chờ duyệt!");
        }

        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setBook(book);
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setStatus("Pending");
        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReservationRequest(Integer reservationId, String staffUsername) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn yêu cầu đặt trước!"));

        if (!"Pending".equalsIgnoreCase(reservation.getStatus())) {
            throw new Exception("Đơn đặt trước này đã được xử lý từ trước!");
        }

        reservation.setStatus("Active");
        reservationRepository.save(reservation);

        sendInternalNotification(reservation.getMember(), "Yêu cầu đặt trước được phê duyệt",
                "Cuốn sách '" + reservation.getBook().getTitle()
                        + "' đã được đặt giữ thành công. Vui lòng đến nhận trong vòng 3 ngày.");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void memberCancelReservation(String username, Integer reservationId) throws Exception {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new Exception("Đơn đặt trước không tồn tại!"));

        Integer currentMemberId = getMemberIdByUsername(username);

        if (currentMemberId == null || !reservation.getMember().getMemberId().equals(currentMemberId)) {
            throw new Exception("Bạn không có quyền hủy đơn đặt trước của người khác!");
        }

        reservation.setStatus("Canceled");
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getAllPendingReservations() {
        return reservationRepository.findAll().stream()
                .filter(r -> "Pending".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationRequestDTO> getPendingReservationDTOs() {
        List<Reservation> reservations = getAllPendingReservations();
        return reservations.stream().map(r -> {
            Member member = r.getMember();
            String name = member != null && member.getUser() != null ? member.getUser().getFullName() : "N/A";

            return new ReservationRequestDTO(
                    r.getReservationId(),
                    name,
                    r.getBook().getTitle(),
                    r.getReservationDate(),
                    1);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberCurrentBorrows(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) {
            return new ArrayList<>();
        }

        List<BorrowDetail> details = borrowDetailRepository.findCurrentBorrowsByMemberId(memberId).stream()
                .filter(d -> "Pending".equalsIgnoreCase(d.getStatus())
                        || "Borrowed".equalsIgnoreCase(d.getStatus())
                        || "Overdue".equalsIgnoreCase(d.getStatus())
                        || "Return_Pending".equalsIgnoreCase(d.getStatus()))
                .collect(Collectors.toList());

        List<MemberBorrowDTO> dtoList = new ArrayList<>();
        for (BorrowDetail detail : details) {
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(detail.getBorrowDetailId());
            dto.setBookTitle(detail.getBook().getTitle());
            dto.setAuthorName(authorNames(detail.getBook()));
            dto.setBookIdStr(detail.getBookItem() != null ? detail.getBookItem().getBarcode()
                    : "BK-" + detail.getBook().getBookId());
            dto.setActionDate(detail.getBorrow().getBorrowDate());
            dto.setDueDate(detail.getDueDate());
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
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberReservations(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) {
            return new ArrayList<>();
        }

        List<Reservation> reservations = reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(memberId)
                .stream()
                .filter(r -> "Pending".equalsIgnoreCase(r.getStatus()) || "Active".equalsIgnoreCase(r.getStatus()))
                .collect(Collectors.toList());

        List<MemberBorrowDTO> dtoList = new ArrayList<>();
        for (Reservation res : reservations) {
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(res.getReservationId());
            dto.setBookTitle(res.getBook().getTitle());
            dto.setAuthorName(authorNames(res.getBook()));
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
        if (memberId == null) {
            return new ArrayList<>();
        }

        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<BorrowDetail> details = borrowDetailRepository.findBorrowHistoryInOneMonth(memberId, oneMonthAgo).stream()
                .filter(d -> "Returned".equalsIgnoreCase(d.getStatus()))
                .collect(Collectors.toList());

        List<MemberBorrowDTO> dtoList = new ArrayList<>();
        for (BorrowDetail detail : details) {
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(detail.getBorrowDetailId());
            dto.setBookTitle(detail.getBook().getTitle());
            dto.setAuthorName(authorNames(detail.getBook()));
            dto.setBookIdStr(detail.getBookItem() != null ? detail.getBookItem().getBarcode()
                    : "BK-" + detail.getBook().getBookId());
            dto.setActionDate(detail.getBorrow().getBorrowDate());
            dto.setReturnDate(detail.getReturnDate());
            dto.setStatus(detail.getStatus());
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getBorrowsByMemberAndStatus(String username, String status) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) {
            return new ArrayList<>();
        }
        return borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null
                        && memberId.equals(b.getMember().getMemberId())
                        && status.equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllPendingRequests() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Pending".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllReturnRequests() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Return_Pending".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllActiveLoans() {
        return borrowRepository.findAll().stream()
                .filter(b -> "Active".equalsIgnoreCase(b.getStatus()) || "Borrowing".equalsIgnoreCase(b.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllBorrowHistoryByMember(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) {
            return new ArrayList<>();
        }
        return borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null && memberId.equals(b.getMember().getMemberId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Borrow getBorrowById(Integer borrowId) throws Exception {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn mượn!"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId) {
        return borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .collect(Collectors.toList());
    }

    private Integer getMemberIdByUsername(String username) {
        return memberAccountRepository.findByUsername(username)
                .map(MemberAccount::getMember)
                .map(Member::getMemberId)
                .orElse(null);
    }

    private String authorNames(Book book) {
        if (book.getAuthors() == null || book.getAuthors().isEmpty()) {
            return "Chưa rõ tác giả";
        }
        return book.getAuthors().stream()
                .map(Author::getAuthorName)
                .collect(Collectors.joining(", "));
    }

    private void sendInternalNotification(Member member, String title, String content) {
        if (member == null || member.getMemberId() == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setCreatedDate(LocalDateTime.now());
        notification.setStatus("Active");
        Notification saved = notificationRepository.save(notification);

        MemberNotification memberNotification = new MemberNotification();
        memberNotification.setId(new MemberNotificationId(member.getMemberId(), saved.getNotificationId()));
        memberNotification.setMember(member);
        memberNotification.setNotification(saved);
        memberNotification.setIsRead(false);
        memberNotificationRepository.save(memberNotification);
    }
}
