package com.lms.service.impl;

import com.lms.dto.response.AdminStaffListViewData;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.StaffRepository;
import com.lms.service.AdminDashboardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookItemRepository bookItemRepository;
    private final StaffRepository staffRepository;
    private final StaffAccountRepository staffAccountRepository;

    public AdminDashboardServiceImpl(
            BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            ReservationRepository reservationRepository,
            MemberRepository memberRepository,
            BookRepository bookRepository,
            BookItemRepository bookItemRepository,
            StaffRepository staffRepository,
            StaffAccountRepository staffAccountRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.bookItemRepository = bookItemRepository;
        this.staffRepository = staffRepository;
        this.staffAccountRepository = staffAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("activeBorrows", borrowRepository.countByStatusIgnoreCase("Active"));
        data.put("pendingBorrows", reservationRepository.countByNormalizedStatuses(
                List.of("PENDING", "DEPOSIT_PAID", "READY")));
        data.put("overdueBorrows", borrowDetailRepository.countByStatusIgnoreCase("Overdue"));
        data.put("totalMembers", memberRepository.count());
        data.put("totalBooks", bookRepository.countByStatusIgnoreCase("Active"));
        data.put("availableItems", bookItemRepository.countByStatusIgnoreCase("Available"));
        data.put("recentBorrows", borrowRepository.findTop5ByOrderByBorrowDateDesc());
        data.put("monthStats", getLastSixMonthStats());
        data.put("currentDate", LocalDate.now());
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStaffListViewData getStaffList(int page, String keyword) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("staffId").ascending());
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Page<Staff> staffPage = normalizedKeyword.isEmpty()
                ? staffRepository.findAll(pageable)
                : staffRepository.searchStaffByKeyword(normalizedKeyword, pageable);

        Map<Integer, StaffAccount> accountByUserId = new HashMap<>();
        for (Staff staff : staffPage.getContent()) {
            if (staff.getUser() != null && staff.getUser().getId() != null) {
                staffAccountRepository.findByStaff_User_Id(staff.getUser().getId())
                        .ifPresent(account -> accountByUserId.put(staff.getUser().getId(), account));
            }
        }
        return new AdminStaffListViewData(staffPage, accountByUserId);
    }

    private List<Map<String, Object>> getLastSixMonthStats() {
        List<Map<String, Object>> monthStats = new ArrayList<>();
        List<Long> borrowCounts = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        long maxCount = 0;

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime startDate = month.atDay(1).atStartOfDay();
            LocalDateTime endDate = month.plusMonths(1).atDay(1).atStartOfDay();
            long count = borrowRepository.countByBorrowDateGreaterThanEqualAndBorrowDateLessThan(startDate, endDate);
            borrowCounts.add(count);
            maxCount = Math.max(maxCount, count);
        }

        for (int i = 0; i < borrowCounts.size(); i++) {
            YearMonth month = currentMonth.minusMonths(5L - i);
            long count = borrowCounts.get(i);
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("label", month.format(DateTimeFormatter.ofPattern("MM/yyyy")));
            stat.put("count", count);
            stat.put("height", calculateBarHeight(count, maxCount));
            monthStats.add(stat);
        }
        return monthStats;
    }

    private int calculateBarHeight(long count, long maxCount) {
        return maxCount == 0 ? 8 : (int) Math.max(8, count * 120 / maxCount);
    }
}
