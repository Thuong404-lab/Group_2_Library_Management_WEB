package com.lms.service.impl;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.dto.response.LibrarianListViewData;
import com.lms.entity.StaffAccount;
import com.lms.entity.Staff;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.GenreRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.StaffRepository;
import com.lms.service.LibrarianDashboardService;
import com.lms.service.LibrarianInteractionService;
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

    private static final int DASHBOARD_PAGE_SIZE = 5;

    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final ReservationRepository reservationRepository;
    private final BookItemRepository bookItemRepository;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final MemberRepository memberRepository;
    private final StaffRepository staffRepository;
    private final StorageService storageService;
    private final LibrarianInteractionService interactionService;
    private final StaffAccountRepository staffAccountRepository;

    public LibrarianDashboardServiceImpl(
            BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            ReservationRepository reservationRepository,
            BookItemRepository bookItemRepository,
            BookRepository bookRepository,
            CategoryRepository categoryRepository,
            GenreRepository genreRepository,
            MemberRepository memberRepository,
            StaffRepository staffRepository,
            StorageService storageService,
            LibrarianInteractionService interactionService,
            StaffAccountRepository staffAccountRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.reservationRepository = reservationRepository;
        this.bookItemRepository = bookItemRepository;
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.genreRepository = genreRepository;
        this.memberRepository = memberRepository;
        this.staffRepository = staffRepository;
        this.storageService = storageService;
        this.interactionService = interactionService;
        this.staffAccountRepository = staffAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData() {
        return getDashboardData(0, 0, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(int reviewPage, int requestPage) {
        return getDashboardData(0, reviewPage, requestPage);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(int bookPage, int reviewPage, int requestPage) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("activeBorrows", borrowRepository.countByStatusIgnoreCase("Active"));
        data.put("pendingReservations",
                reservationRepository.countByNormalizedStatuses(List.of("PENDING", "DEPOSIT_PAID", "READY")));
        data.put("overdueDetails", borrowDetailRepository.countByStatusIgnoreCase("Overdue"));
        data.put("availableItems", bookItemRepository.countByStatusIgnoreCase("Available"));
        data.put("totalMembers", memberRepository.count());
        data.put("totalLibrarians", staffRepository.countByStaffTypeIgnoreCase("Librarian"));
        data.put("currentDate", LocalDate.now());
        data.put("recentBorrows", borrowRepository.findTop5ByOrderByBorrowDateDesc());
        data.put("dueSoonDetails",
                borrowDetailRepository.findTop5ByStatusIgnoreCaseAndDueDateBetweenOrderByDueDateAsc(
                        "Borrowed", now, now.plusDays(7)));
        data.put("reviews", interactionService.getReviewsForModeration(
                null, PageRequest.of(Math.max(0, reviewPage), DASHBOARD_PAGE_SIZE, Sort.by("createdDate").descending())));
        data.put("notificationRequest", new LibrarianNotificationSendRequest());
        data.put("members", interactionService.getAllMembers());
        data.put("requests", interactionService.getBookAcquisitionRequests(
                PageRequest.of(Math.max(0, requestPage), DASHBOARD_PAGE_SIZE, Sort.by("requestId").ascending())));
        data.put("shelves", storageService.getAllStorageLocations());
        data.put("books", bookRepository.findAll(PageRequest.of(bookPage, 10, Sort.by("bookId").descending())));
        data.put("categories", categoryRepository.findAll());
        data.put("genres", genreRepository.findAll());
        data.put("totalBookCount", bookRepository.count());
        data.put("totalCategories", categoryRepository.count());
        data.put("totalGenres", genreRepository.count());
        data.put("inventoryStatusCounts", inventoryStatusCounts());
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public LibrarianListViewData getLibrarianList(int page, String keyword) {
        PageRequest pageable = PageRequest.of(page, 10, Sort.by("staffId").ascending());
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Page<Staff> staffPage = normalizedKeyword.isEmpty()
                ? staffRepository.findByStaffTypeIgnoreCase("Librarian", pageable)
                : staffRepository.searchByStaffTypeAndKeyword(
                        "Librarian", normalizedKeyword, pageable);

        Map<Integer, StaffAccount> accountByUserId = new HashMap<>();
        for (Staff staff : staffPage.getContent()) {
            if (staff.getUser() != null && staff.getUser().getId() != null) {
                staffAccountRepository.findByStaff_User_Id(staff.getUser().getId())
                        .ifPresent(account -> accountByUserId.put(staff.getUser().getId(), account));
            }
        }
        return new LibrarianListViewData(staffPage, accountByUserId);
    }

    private Map<String, Long> inventoryStatusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Available", bookItemRepository.countByStatusIgnoreCase("Available"));
        counts.put("Borrowed", bookItemRepository.countByStatusIgnoreCase("Borrowed"));
        counts.put("Lost", bookItemRepository.countByStatusIgnoreCase("Lost"));
        counts.put("Damaged", bookItemRepository.countByStatusIgnoreCase("Damaged"));
        counts.put("Disposed", bookItemRepository.countByStatusIgnoreCase("Disposed"));
        return counts;
    }
}
