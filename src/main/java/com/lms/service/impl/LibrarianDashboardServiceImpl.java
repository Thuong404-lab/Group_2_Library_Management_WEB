package com.lms.service.impl;

import com.lms.dto.response.LibrarianListViewData;
import com.lms.dto.response.LibrarianListItemResponse;
import com.lms.entity.StaffAccount;
import com.lms.entity.Book;
import com.lms.entity.BookItem;
import com.lms.enums.AcquisitionRequestStatus;
import com.lms.enums.UserStatus;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.GenreRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.StaffRepository;
import com.lms.exception.DataProcessingException;
import com.lms.exception.ValidationException;
import com.lms.service.LibrarianDashboardService;
import com.lms.service.LibrarianInteractionService;
import com.lms.service.LocalizedMessageService;
import com.lms.service.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LibrarianDashboardServiceImpl implements LibrarianDashboardService {

    private static final int DASHBOARD_PAGE_SIZE = 10;
    private static final int DIRECTORY_PAGE_SIZE = 10;
    private static final int MAX_DIRECTORY_PAGE_INDEX = 10_000;
    private static final int MAX_DIRECTORY_KEYWORD_LENGTH = 100;
    private static final int OVERVIEW_LIST_SIZE = 5;
    private static final int DUE_SOON_DAYS = 7;
    private static final String LIBRARIAN_STAFF_TYPE = "Librarian";
    private static final List<String> CURRENT_LOAN_DETAIL_STATUSES =
            List.of("BORROWED", "RETURN_PENDING", "RENEW_PENDING");
    private static final List<String> RECENT_CIRCULATION_STATUSES =
            List.of("BORROWED", "OVERDUE", "RETURN_PENDING", "RENEW_PENDING", "RETURNED");
    private static final List<String> BOOK_CONDITIONS =
            List.of("New", "Minor damage", "Severely damaged", "Lost book");

    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final ReservationRepository reservationRepository;
    private final BookItemRepository bookItemRepository;
    private final BookRepository bookRepository;
    private final BookAcquisitionRequestRepository bookAcquisitionRequestRepository;
    private final FeedbackRepository feedbackRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final MemberRepository memberRepository;
    private final StaffRepository staffRepository;
    private final StorageService storageService;
    private final LibrarianInteractionService interactionService;
    private final StaffAccountRepository staffAccountRepository;
    private final LocalizedMessageService messages;

    public LibrarianDashboardServiceImpl(
            BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            ReservationRepository reservationRepository,
            BookItemRepository bookItemRepository,
            BookRepository bookRepository,
            BookAcquisitionRequestRepository bookAcquisitionRequestRepository,
            FeedbackRepository feedbackRepository,
            CategoryRepository categoryRepository,
            GenreRepository genreRepository,
            MemberRepository memberRepository,
            StaffRepository staffRepository,
            StorageService storageService,
            LibrarianInteractionService interactionService,
            StaffAccountRepository staffAccountRepository,
            LocalizedMessageService messages) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.reservationRepository = reservationRepository;
        this.bookItemRepository = bookItemRepository;
        this.bookRepository = bookRepository;
        this.bookAcquisitionRequestRepository = bookAcquisitionRequestRepository;
        this.feedbackRepository = feedbackRepository;
        this.categoryRepository = categoryRepository;
        this.genreRepository = genreRepository;
        this.memberRepository = memberRepository;
        this.staffRepository = staffRepository;
        this.storageService = storageService;
        this.interactionService = interactionService;
        this.staffAccountRepository = staffAccountRepository;
        this.messages = messages;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData() {
        return getDashboardData(0, 0, 0, "");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(int reviewPage, int requestPage) {
        return getDashboardData(0, reviewPage, requestPage, "");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(int bookPage, int reviewPage, int requestPage) {
        return getDashboardData(bookPage, reviewPage, requestPage, "");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(int bookPage, int reviewPage, int requestPage, String keyword) {
        return getDashboardData(bookPage, 0, reviewPage, requestPage, keyword);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(int bookPage, int shelfPage, int reviewPage, int requestPage,
            String keyword) {
        return getDashboardData(bookPage, shelfPage, reviewPage, requestPage, keyword, "");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(int bookPage, int shelfPage, int reviewPage, int requestPage,
            String keyword, String bookCondition) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("activeBorrows", borrowRepository.countByStatusIgnoreCase("Active"));
        data.put("pendingReservationRequests",
                reservationRepository.countByStatusIgnoreCase("PENDING"));
        data.put("depositPaidReservations",
                reservationRepository.countByStatusIgnoreCase("DEPOSIT_PAID"));
        data.put("readyReservations",
                reservationRepository.countByStatusIgnoreCase("READY"));
        data.put("overdueDetails", borrowDetailRepository.countByStatusIgnoreCase("Overdue"));
        data.put("dueTodayDetails",
                borrowDetailRepository.countCurrentLoansDueInRange(
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay(),
                        CURRENT_LOAN_DETAIL_STATUSES));
        data.put("pendingReturns", borrowDetailRepository.countByStatusIgnoreCase("Return_Pending"));
        data.put("pendingRenewals", borrowDetailRepository.countByStatusIgnoreCase("Renew_Pending"));
        data.put("pendingAcquisitionRequests",
                bookAcquisitionRequestRepository.countByStatus(AcquisitionRequestStatus.PENDING));
        data.put("unansweredReviews", feedbackRepository.countAwaitingLibrarianResponse());
        data.put("availableItems", bookItemRepository.countByStatusIgnoreCase("Available"));
        data.put("totalMembers", memberRepository.count());
        data.put("totalLibrarians", staffRepository.countByStaffTypeIgnoreCase(LIBRARIAN_STAFF_TYPE));
        data.put("currentDate", today);
        data.put("dashboardGeneratedAt", now);
        data.put("recentBorrows", borrowDetailRepository.findRecentCirculationActivities(
                RECENT_CIRCULATION_STATUSES, PageRequest.of(0, OVERVIEW_LIST_SIZE)));
        data.put("dueSoonDetails",
                borrowDetailRepository.findCurrentLoansDueSoon(
                        now,
                        now.plusDays(DUE_SOON_DAYS),
                        CURRENT_LOAN_DETAIL_STATUSES,
                        PageRequest.of(0, OVERVIEW_LIST_SIZE)));
        data.put("reviews", interactionService.getReviewsForModeration(
                null,
                PageRequest.of(Math.max(0, reviewPage), DASHBOARD_PAGE_SIZE, Sort.by("createdDate").descending())));
        data.put("requests", interactionService.getBookAcquisitionRequests(null, null,
                PageRequest.of(Math.max(0, requestPage), DASHBOARD_PAGE_SIZE,
                        Sort.by(Sort.Order.desc("createdDate"), Sort.Order.desc("requestId")))));
        data.put("shelves", storageService.getAllStorageLocations());
        data.put("shelfPage", storageService.getStorageLocations(
                PageRequest.of(Math.max(0, shelfPage), DASHBOARD_PAGE_SIZE)));
        Map<Integer, Long> shelfBookCounts = new HashMap<>();
        bookItemRepository.countBookItemsByShelf().forEach(row -> shelfBookCounts.put((Integer) row[0], (Long) row[1]));
        data.put("shelfBookCounts", shelfBookCounts);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedBookCondition = BOOK_CONDITIONS.contains(bookCondition) ? bookCondition : "";
        Page<Book> booksPage = bookRepository.searchBookItems(normalizedKeyword, normalizedBookCondition,
                PageRequest.of(Math.max(0, bookPage), DASHBOARD_PAGE_SIZE, Sort.by("bookId").ascending()));
        booksPage.forEach(book -> {
            if (book.getAuthors() != null) {
                book.getAuthors().size();
            }
        });
        data.put("books", booksPage);
        data.put("bookCondition", normalizedBookCondition);

        Map<Integer, Integer> bookShelves = new HashMap<>();
        Map<Integer, Integer> bookTotalQuantities = new HashMap<>();
        Map<Integer, Integer> bookBorrowedQuantities = new HashMap<>();
        Map<Integer, Integer> bookAvailableQuantities = new HashMap<>();
        Map<Integer, List<BookItem>> bookItemsByBookId = new HashMap<>();
        for (Book book : booksPage.getContent()) {
            List<BookItem> items = bookItemRepository.findByBook_BookIdOrderByBarcodeAsc(book.getBookId());
            bookItemsByBookId.put(book.getBookId(), items != null ? items : List.of());
            if (items != null && !items.isEmpty()) {
                if (items.get(0).getShelf() != null) {
                    bookShelves.put(book.getBookId(), items.get(0).getShelf().getShelfId());
                }
                bookTotalQuantities.put(book.getBookId(), items.size());
                int borrowed = (int) items.stream().filter(i -> "Borrowed".equalsIgnoreCase(i.getStatus())).count();
                bookBorrowedQuantities.put(book.getBookId(), borrowed);
                int available = (int) items.stream().filter(i -> "Available".equalsIgnoreCase(i.getStatus())).count();
                bookAvailableQuantities.put(book.getBookId(), available);
            } else {
                bookTotalQuantities.put(book.getBookId(), 0);
                bookBorrowedQuantities.put(book.getBookId(), 0);
                bookAvailableQuantities.put(book.getBookId(), 0);
            }
        }
        data.put("bookShelves", bookShelves);
        data.put("bookTotalQuantities", bookTotalQuantities);
        data.put("bookBorrowedQuantities", bookBorrowedQuantities);
        data.put("bookAvailableQuantities", bookAvailableQuantities);
        data.put("bookItemsByBookId", bookItemsByBookId);
        data.put("categories", categoryRepository.findAll());
        data.put("genres", genreRepository.findAll());
        Map<Integer, Long> genreTitleCounts = new HashMap<>();
        for (Object[] row : bookRepository.countTitlesByGenre()) {
            genreTitleCounts.put((Integer) row[0], (Long) row[1]);
        }
        data.put("genreTitleCounts", genreTitleCounts);

        Map<Integer, List<Book>> genreBooks = new HashMap<>();
        for (Book book : bookRepository.findAll(Sort.by("title").ascending())) {
            if (book.getGenre() != null) {
                genreBooks.computeIfAbsent(book.getGenre().getGenreId(), ignored -> new java.util.ArrayList<>())
                        .add(book);
            }
        }
        Map<Integer, Long> allBookTotalQuantities = new HashMap<>();
        for (Object[] row : bookItemRepository.countBookItemsByBook()) {
            allBookTotalQuantities.put((Integer) row[0], (Long) row[1]);
        }
        data.put("genreBooks", genreBooks);
        data.put("allBookTotalQuantities", allBookTotalQuantities);
        data.put("totalBookCount", bookRepository.count());
        data.put("totalCategories", categoryRepository.count());
        data.put("totalGenres", genreRepository.count());
        data.put("inventoryStatusCounts", inventoryStatusCounts());
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBookManagementData(int bookPage, int shelfPage, String keyword,
            String bookCondition, String subsection, String tab) {
        Map<String, Object> data = new LinkedHashMap<>();
        String activeSubsection = "storage".equalsIgnoreCase(subsection) ? "storage" : "inventory";
        String activeTab = "categories".equalsIgnoreCase(tab) ? "categories"
                : "audit".equalsIgnoreCase(tab) ? "audit" : "books";

        if ("storage".equals(activeSubsection)) {
            data.put("shelves", storageService.getAllStorageLocations());
            data.put("shelfPage", storageService.getStorageLocations(
                    PageRequest.of(Math.max(0, shelfPage), DASHBOARD_PAGE_SIZE)));
            Map<Integer, Long> shelfBookCounts = new HashMap<>();
            bookItemRepository.countBookItemsByShelf()
                    .forEach(row -> shelfBookCounts.put((Integer) row[0], (Long) row[1]));
            data.put("shelfBookCounts", shelfBookCounts);
            return data;
        }

        if ("categories".equals(activeTab)) {
            data.put("categories", categoryRepository.findAll());
            data.put("genres", genreRepository.findAll());
            Map<Integer, Long> genreTitleCounts = new HashMap<>();
            bookRepository.countTitlesByGenre()
                    .forEach(row -> genreTitleCounts.put((Integer) row[0], (Long) row[1]));
            data.put("genreTitleCounts", genreTitleCounts);

            Map<Integer, List<Book>> genreBooks = new HashMap<>();
            for (Book book : bookRepository.findAll(Sort.by("title").ascending())) {
                if (book.getGenre() != null) {
                    genreBooks.computeIfAbsent(book.getGenre().getGenreId(),
                            ignored -> new java.util.ArrayList<>()).add(book);
                }
            }
            Map<Integer, Long> allBookTotalQuantities = new HashMap<>();
            bookItemRepository.countBookItemsByBook()
                    .forEach(row -> allBookTotalQuantities.put((Integer) row[0], (Long) row[1]));
            data.put("genreBooks", genreBooks);
            data.put("allBookTotalQuantities", allBookTotalQuantities);
            data.put("totalCategories", categoryRepository.count());
            data.put("totalGenres", genreRepository.count());
            return data;
        }

        data.put("shelves", storageService.getAllStorageLocations());
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedBookCondition = BOOK_CONDITIONS.contains(bookCondition) ? bookCondition : "";
        Page<Book> booksPage = bookRepository.searchBookItems(
                normalizedKeyword,
                normalizedBookCondition,
                PageRequest.of(Math.max(0, bookPage), DASHBOARD_PAGE_SIZE,
                        Sort.by("bookId").ascending()));
        booksPage.forEach(book -> {
            if (book.getAuthors() != null) {
                book.getAuthors().size();
            }
        });
        data.put("books", booksPage);
        data.put("bookCondition", normalizedBookCondition);

        Map<Integer, Integer> bookShelves = new HashMap<>();
        Map<Integer, Integer> bookTotalQuantities = new HashMap<>();
        Map<Integer, Integer> bookBorrowedQuantities = new HashMap<>();
        Map<Integer, Integer> bookAvailableQuantities = new HashMap<>();
        Map<Integer, List<BookItem>> bookItemsByBookId = new HashMap<>();
        List<Integer> pageBookIds = booksPage.getContent().stream().map(Book::getBookId).toList();
        if (!pageBookIds.isEmpty()) {
            bookItemRepository.findByBookIdsWithShelf(pageBookIds).forEach(item ->
                    bookItemsByBookId.computeIfAbsent(item.getBook().getBookId(),
                            ignored -> new java.util.ArrayList<>()).add(item));
        }
        for (Book book : booksPage.getContent()) {
            List<BookItem> items = bookItemsByBookId.getOrDefault(book.getBookId(), List.of());
            bookItemsByBookId.putIfAbsent(book.getBookId(), items);
            if (!items.isEmpty() && items.get(0).getShelf() != null) {
                bookShelves.put(book.getBookId(), items.get(0).getShelf().getShelfId());
            }
            bookTotalQuantities.put(book.getBookId(), items.size());
            bookBorrowedQuantities.put(book.getBookId(),
                    (int) items.stream().filter(i -> "Borrowed".equalsIgnoreCase(i.getStatus())).count());
            bookAvailableQuantities.put(book.getBookId(),
                    (int) items.stream().filter(i -> "Available".equalsIgnoreCase(i.getStatus())).count());
        }
        data.put("bookShelves", bookShelves);
        data.put("bookTotalQuantities", bookTotalQuantities);
        data.put("bookBorrowedQuantities", bookBorrowedQuantities);
        data.put("bookAvailableQuantities", bookAvailableQuantities);
        data.put("bookItemsByBookId", bookItemsByBookId);

        data.put("totalBookCount", bookRepository.count());
        data.put("inventoryStatusCounts", inventoryStatusCounts());
        if ("books".equals(activeTab)) {
            data.put("categories", categoryRepository.findAll());
            data.put("genres", genreRepository.findAll());
        }
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatisticsData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activeBorrows", borrowRepository.countByStatusIgnoreCase("Active"));
        data.put("pendingReservations",
                reservationRepository.countByNormalizedStatuses(List.of("PENDING", "DEPOSIT_PAID", "READY")));
        data.put("overdueDetails", borrowDetailRepository.countByStatusIgnoreCase("Overdue"));
        data.put("totalMembers", memberRepository.count());
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public LibrarianListViewData getLibrarianList(int page, String keyword, String status) {
        validateDirectoryPage(page);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > MAX_DIRECTORY_KEYWORD_LENGTH) {
            throw new ValidationException("keyword",
                    messages.get("backend.librarian.staff.keywordTooLong", MAX_DIRECTORY_KEYWORD_LENGTH));
        }
        UserStatus selectedStatus = parseUserStatus(status);
        PageRequest pageable = PageRequest.of(page, DIRECTORY_PAGE_SIZE, Sort.by("id").ascending());
        Page<LibrarianListItemResponse> staffPage = staffAccountRepository.searchDirectory(
                        LIBRARIAN_STAFF_TYPE,
                        normalizedKeyword,
                        selectedStatus == null ? "" : selectedStatus.name(),
                        pageable)
                .map(this::toLibrarianListItem);

        return new LibrarianListViewData(
                staffPage,
                librarianSummaryCounts(),
                normalizedKeyword,
                selectedStatus == null ? "" : selectedStatus.name());
    }

    private UserStatus parseUserStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (UserStatus candidate : UserStatus.values()) {
            if (candidate.name().equalsIgnoreCase(status.trim())) {
                return candidate;
            }
        }
        throw new ValidationException("status", messages.get("backend.librarian.staff.statusInvalid"));
    }

    private Map<String, Long> librarianSummaryCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", 0L);
        counts.put("active", 0L);
        counts.put("inactive", 0L);
        counts.put("blocked", 0L);
        for (Object[] row : staffAccountRepository.countDirectoryByStatus(LIBRARIAN_STAFF_TYPE)) {
            UserStatus accountStatus = accountStatus(String.valueOf(row[0]));
            long amount = ((Number) row[1]).longValue();
            counts.put(accountStatus.name().toLowerCase(java.util.Locale.ROOT), amount);
            counts.put("total", counts.get("total") + amount);
        }
        return counts;
    }

    private void validateDirectoryPage(int page) {
        if (page < 0 || page > MAX_DIRECTORY_PAGE_INDEX) {
            throw new ValidationException("page", messages.get("backend.librarian.staff.pageInvalid"));
        }
    }

    private LibrarianListItemResponse toLibrarianListItem(StaffAccount account) {
        return new LibrarianListItemResponse(
                account.getStaff().getStaffId(),
                account.getId(),
                account.getUsername(),
                account.getStaff().getUser().getFullName(),
                account.getStaff().getStaffType(),
                accountStatus(account.getStatus()));
    }

    private UserStatus accountStatus(String status) {
        if (status != null) {
            for (UserStatus candidate : UserStatus.values()) {
                if (candidate.name().equalsIgnoreCase(status.trim())) {
                    return candidate;
                }
            }
        }
        throw new DataProcessingException(messages.get("backend.librarian.staff.accountStatusInvalid"));
    }

    private Map<String, Long> inventoryStatusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Total", bookItemRepository.count());
        counts.put("Available", bookItemRepository.countByStatusIgnoreCase("Available"));
        counts.put("Borrowed", bookItemRepository.countByStatusIgnoreCase("Borrowed"));
        counts.put("Waiting_Pickup", bookItemRepository.countByStatusIgnoreCase("Waiting_Pickup"));
        counts.put("Unavailable", bookItemRepository.countByStatusIgnoreCase("Unavailable"));
        return counts;
    }
}
