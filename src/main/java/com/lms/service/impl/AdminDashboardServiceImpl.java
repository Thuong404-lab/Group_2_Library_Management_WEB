package com.lms.service.impl;

import com.lms.dto.response.AdminStaffListViewData;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.enums.UserStatus;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.StaffRepository;
import com.lms.repository.SystemLogRepository;
import com.lms.repository.TransactionRepository;
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
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookItemRepository bookItemRepository;
    private final StaffRepository staffRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final SystemLogRepository systemLogRepository;
    private final TransactionRepository transactionRepository;

    public AdminDashboardServiceImpl(
            BorrowRepository borrowRepository,
            MemberRepository memberRepository,
            BookRepository bookRepository,
            BookItemRepository bookItemRepository,
            StaffRepository staffRepository,
            StaffAccountRepository staffAccountRepository,
            SystemLogRepository systemLogRepository,
            TransactionRepository transactionRepository) {
        this.borrowRepository = borrowRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.bookItemRepository = bookItemRepository;
        this.staffRepository = staffRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.systemLogRepository = systemLogRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalBooks", bookRepository.countByStatusIgnoreCase("Active"));
        data.put("availableItems", bookItemRepository.countByStatusIgnoreCase("Available"));
        data.put("totalMembers", memberRepository.count());
        data.put("totalStaff", staffRepository.count());
        
        // Calculate Total Revenue (Completed transactions)
        java.math.BigDecimal totalRevenue = transactionRepository.sumAmountByStatusAndDateRange(
                "Completed", LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 1, 0, 0));
        data.put("totalRevenue", totalRevenue != null ? totalRevenue : java.math.BigDecimal.ZERO);
        
        // Calculate New Members this month
        // We can just omit new members if we don't want to modify MemberRepository, but let's use active borrows as a placeholder metric for admin as well
        data.put("activeBorrows", borrowRepository.countByStatusIgnoreCase("Active"));
        
        data.put("recentLogs", systemLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5)).getContent());
        data.put("monthStats", getLastSixMonthStats());
        data.put("currentDate", LocalDate.now());
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStaffListViewData getStaffList(int page, String keyword, String status, String staffType) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("staffId").ascending());
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedStaffType = normalizeStaffType(staffType);
        UserStatus selectedStatus = parseUserStatus(status);
        Page<Staff> staffPage = staffRepository.searchStaffWithStatus(
                normalizedKeyword, selectedStatus, normalizedStaffType, pageable);

        Map<Integer, StaffAccount> accountByUserId = new HashMap<>();
        for (Staff staff : staffPage.getContent()) {
            if (staff.getUser() != null && staff.getUser().getId() != null) {
                staffAccountRepository.findByStaff_User_Id(staff.getUser().getId())
                        .ifPresent(account -> accountByUserId.put(staff.getUser().getId(), account));
            }
        }
        Map<String, Long> summaryCounts = new LinkedHashMap<>();
        summaryCounts.put("total", staffRepository.count());
        summaryCounts.put("librarian", staffRepository.countByStaffTypeIgnoreCase("Librarian"));
        summaryCounts.put("admin", staffRepository.countByStaffTypeIgnoreCase("Admin"));
        summaryCounts.put("active", staffRepository.countByUser_Status(UserStatus.Active));
        summaryCounts.put("inactive", staffRepository.countByUser_Status(UserStatus.Inactive));
        summaryCounts.put("blocked", staffRepository.countByUser_Status(UserStatus.Blocked));
        return new AdminStaffListViewData(staffPage, accountByUserId, summaryCounts);
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
        return null;
    }

    private String normalizeStaffType(String staffType) {
        if (staffType == null || staffType.isBlank()) {
            return "";
        }
        if ("Admin".equalsIgnoreCase(staffType.trim())) {
            return "Admin";
        }
        if ("Librarian".equalsIgnoreCase(staffType.trim())) {
            return "Librarian";
        }
        return "";
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
