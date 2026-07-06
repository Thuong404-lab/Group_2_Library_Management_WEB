package com.lms.service.impl;

import com.lms.dto.request.BorrowRequest;
import com.lms.dto.response.MemberBorrowDTO;
import com.lms.entity.Author;
import com.lms.entity.Book;
import com.lms.entity.BookItem;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.Reservation;
import com.lms.entity.SystemSetting;
import com.lms.enums.UserStatus;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.SystemSettingRepository;
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
    private final SystemSettingRepository systemSettingRepository;
    private final ReservationRepository reservationRepository;

    public BorrowServiceImpl(MemberRepository memberRepository,
                             BookItemRepository bookItemRepository,
                             BorrowRepository borrowRepository,
                             BorrowDetailRepository borrowDetailRepository,
                             BookRepository bookRepository,
                             MemberAccountRepository memberAccountRepository,
                             SystemSettingRepository systemSettingRepository,
                             ReservationRepository reservationRepository) {
        this.memberRepository = memberRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.bookRepository = bookRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception {
        Member member = memberRepository.findByUserEmail(request.getMemberEmail())
                .orElseThrow(() -> new Exception("Khong tim thay doc gia voi email nay!"));

        if (member.getUser() != null && member.getUser().getStatus() != UserStatus.Active) {
            throw new Exception("Tai khoan thanh vien nay dang bi khoa hoac chua kich hoat!");
        }

        List<BookItem> bookItemsToBorrow = new ArrayList<>();
        for (String barcode : request.getBarcodes()) {
            BookItem item = bookItemRepository.findByBarcode(barcode)
                    .orElseThrow(() -> new Exception("Ma vach " + barcode + " khong ton tai!"));

            if (!"Available".equalsIgnoreCase(item.getStatus())) {
                throw new Exception("Sach co ma vach " + barcode + " hien tai khong san sang!");
            }
            bookItemsToBorrow.add(item);
        }

        long currentBorrowCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        int totalRequestedBooks = (int) currentBorrowCount + bookItemsToBorrow.size();

        if (totalRequestedBooks > maxLimit) {
            throw new Exception("So luong sach vuot qua gioi han muon cua hang thanh vien!");
        }

        int borrowDays = normalizeBorrowDays(request.getNumberOfDays());

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
            detail.setDueDate(LocalDateTime.now().plusDays(borrowDays));
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
                .orElseThrow(() -> new Exception("Khong tim thay thong tin doc gia!"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new Exception("Sach yeu cau muon khong ton tai!"));

        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int maxLimit = getEffectiveBorrowLimit(member);
        if (currentBorrowed >= maxLimit) {
            throw new Exception("Yeu cau bi tu choi! Ban da muon cham gioi han toi da cho phep (" + maxLimit + " cuon).");
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

        return borrow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReturnBook(String barcode) throws Exception {
        BookItem item = bookItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new Exception("Ma vach sach vat ly khong ton tai!"));

        if (!"Borrowed".equalsIgnoreCase(item.getStatus())) {
            throw new Exception("Sach nay hien tai dang nam trong kho, khong co lich su cho muon!");
        }

        BorrowDetail activeDetail = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBookItem() != null && d.getBookItem().getBookItemId().equals(item.getBookItemId())
                        && ("Borrowed".equalsIgnoreCase(d.getStatus()) || "Overdue".equalsIgnoreCase(d.getStatus())))
                .findFirst()
                .orElseThrow(() -> new Exception("Khong tim thay lich su muon hop le ung voi ma vach sach nay!"));

        item.setStatus("Available");
        bookItemRepository.save(item);

        activeDetail.setReturnDate(LocalDateTime.now());
        activeDetail.setStatus("Returned");
        borrowDetailRepository.save(activeDetail);

        List<BorrowDetail> sideDetails = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(activeDetail.getBorrow().getBorrowId()))
                .collect(Collectors.toList());

        boolean allReturned = sideDetails.stream().allMatch(d -> "Returned".equalsIgnoreCase(d.getStatus()));
        if (allReturned) {
            Borrow parent = activeDetail.getBorrow();
            parent.setStatus("Returned");
            borrowRepository.save(parent);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getBorrowsByMemberAndStatus(String username, String status) {
        Integer targetMemberId = memberAccountRepository.findByUsername(username)
                .map(MemberAccount::getMember)
                .map(Member::getMemberId)
                .orElse(null);

        if (targetMemberId == null) return new ArrayList<>();

        return borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null
                        && targetMemberId.equals(b.getMember().getMemberId())
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
    @Transactional(rollbackFor = Exception.class)
    public void approvePendingRequest(Integer borrowId, String staffUsername) throws Exception {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Khong tim thay don yeu cau!"));

        borrow.setStatus("Active");
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .collect(Collectors.toList());

        for (BorrowDetail detail : details) {
            detail.setStatus("Borrowed");
            borrowDetailRepository.save(detail);

            if (detail.getBookItem() != null) {
                BookItem item = detail.getBookItem();
                item.setStatus("Borrowed");
                bookItemRepository.save(item);
            }
        }

        if (borrow.getMember() == null || borrow.getMember().getMemberId() == null) {
            throw new RuntimeException("Phieu muon khong co member hop le de tinh phi muon.");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReturnRequest(Integer borrowId) throws Exception {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Khong tim thay don yeu cau tra!"));
        borrow.setStatus("Returned");
        borrowRepository.save(borrow);

        List<BorrowDetail> details = borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .collect(Collectors.toList());
        for (BorrowDetail detail : details) {
            detail.setStatus("Returned");
            borrowDetailRepository.save(detail);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer loanId, String status) throws Exception {
        Borrow borrow = borrowRepository.findById(loanId)
                .orElseThrow(() -> new Exception("Khong tim thay don muon/tra tuong ung!"));
        borrow.setStatus(status);
        borrowRepository.save(borrow);
    }

    @Override
    @Transactional(readOnly = true)
    public Borrow getBorrowById(Integer borrowId) throws Exception {
        return borrowRepository.findById(borrowId)
                .orElseThrow(() -> new Exception("Khong tim thay don muon!"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId) {
        return borrowDetailRepository.findAll().stream()
                .filter(d -> d.getBorrow() != null && d.getBorrow().getBorrowId().equals(borrowId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Borrow> getAllBorrowHistoryByMember(String username) {
        Integer targetMemberId = memberAccountRepository.findByUsername(username)
                .map(MemberAccount::getMember)
                .map(Member::getMemberId)
                .orElse(null);

        if (targetMemberId == null) return new ArrayList<>();

        return borrowRepository.findAll().stream()
                .filter(b -> b.getMember() != null && targetMemberId.equals(b.getMember().getMemberId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberCurrentBorrows(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) return new ArrayList<>();

        List<BorrowDetail> details = borrowDetailRepository.findCurrentBorrowsByMemberId(memberId);
        List<MemberBorrowDTO> dtoList = new ArrayList<>();

        for (BorrowDetail detail : details) {
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(detail.getBorrowDetailId());
            dto.setBookTitle(detail.getBook().getTitle());
            dto.setAuthorName(getAuthorNames(detail.getBook()));
            dto.setBookIdStr(detail.getBookItem() != null ? detail.getBookItem().getBarcode() : "BK-" + detail.getBook().getBookId());
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
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberBorrowDTO> getMemberReservations(String username) {
        Integer memberId = getMemberIdByUsername(username);
        if (memberId == null) return new ArrayList<>();

        List<Reservation> reservations = reservationRepository.findByMember_MemberIdOrderByReservationDateDesc(memberId);
        List<MemberBorrowDTO> dtoList = new ArrayList<>();

        for (Reservation res : reservations) {
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
        List<BorrowDetail> details = borrowDetailRepository.findBorrowHistoryInOneMonth(memberId, oneMonthAgo);
        List<MemberBorrowDTO> dtoList = new ArrayList<>();

        for (BorrowDetail detail : details) {
            MemberBorrowDTO dto = new MemberBorrowDTO();
            dto.setId(detail.getBorrowDetailId());
            dto.setBookTitle(detail.getBook().getTitle());
            dto.setAuthorName(getAuthorNames(detail.getBook()));
            dto.setBookIdStr(detail.getBookItem() != null ? detail.getBookItem().getBarcode() : "BK-" + detail.getBook().getBookId());
            dto.setActionDate(detail.getBorrow().getBorrowDate());
            dto.setReturnDate(detail.getReturnDate());
            dto.setStatus(detail.getStatus());
            dtoList.add(dto);
        }
        return dtoList;
    }

    private Integer getMemberIdByUsername(String username) {
        return memberAccountRepository.findByUsername(username)
                .map(MemberAccount::getMember)
                .map(Member::getMemberId)
                .orElse(null);
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
        return Math.max(1, Math.min(configuredLimit, tierLimit));
    }

    private int normalizeBorrowDays(Integer requestedDays) {
        int maxBorrowDays = getPositiveIntSetting("MAX_BORROW_DAYS", 14);
        int safeRequestedDays = requestedDays == null || requestedDays <= 0 ? maxBorrowDays : requestedDays;
        return Math.min(safeRequestedDays, maxBorrowDays);
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
