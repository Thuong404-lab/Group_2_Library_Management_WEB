package com.lms.service.impl;

import com.lms.dto.response.AdminStaffListViewData;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.enums.UserStatus;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberAccountRepository;
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
    private final BorrowDetailRepository borrowDetailRepository;
    private final MemberRepository memberRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final BookRepository bookRepository;
    private final BookItemRepository bookItemRepository;
    private final StaffRepository staffRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final SystemLogRepository systemLogRepository;
    private final TransactionRepository transactionRepository;

    public AdminDashboardServiceImpl(
            BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            MemberRepository memberRepository,
            MemberAccountRepository memberAccountRepository,
            BookRepository bookRepository,
            BookItemRepository bookItemRepository,
            StaffRepository staffRepository,
            StaffAccountRepository staffAccountRepository,
            SystemLogRepository systemLogRepository,
            TransactionRepository transactionRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.memberRepository = memberRepository;
        this.memberAccountRepository = memberAccountRepository;
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
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = today.plusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime previousMonthStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();

        data.put("totalBooks", bookRepository.countByStatusIgnoreCase("Active"));
        data.put("totalBookItems", bookItemRepository.count());
        data.put("availableItems", bookItemRepository.countByStatusIgnoreCase("Available"));
        data.put("totalMembers", memberRepository.count());
        data.put("activeMembers", memberAccountRepository.countByStatusIgnoreCase("Active"));
        data.put("totalStaff", staffRepository.count());
        data.put("activeStaff", staffRepository.countByUser_Status(UserStatus.Active));
        data.put("activeBorrows", borrowRepository.countByStatusIgnoreCase("Active"));
        data.put("overdueItems", borrowDetailRepository.countByStatusIgnoreCase("Overdue"));
        data.put("attentionItems",
                bookItemRepository.countByStatusIgnoreCase("Damaged")
                        + bookItemRepository.countByStatusIgnoreCase("Lost"));
        data.put("blockedAccounts", memberAccountRepository.countByStatusIgnoreCase("Blocked")
                + staffAccountRepository.countByStatusIgnoreCase("Blocked"));

        java.math.BigDecimal monthlyRevenue = transactionRepository.sumAmountByStatusAndDateRange(
                "Completed", monthStart, nextMonthStart);
        java.math.BigDecimal previousMonthRevenue = transactionRepository.sumAmountByStatusAndDateRange(
                "Completed", previousMonthStart, monthStart);
        data.put("monthlyRevenue", defaultAmount(monthlyRevenue));
        data.put("revenueChangePercent", revenueChangePercent(monthlyRevenue, previousMonthRevenue));

        data.put("recentLogs", systemLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5)).getContent());
        data.put("monthStats", getLastSixMonthStats());
        data.put("currentDate", today);
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
        List<Long> returnCounts = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        long maxCount = 0;

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime startDate = month.atDay(1).atStartOfDay();
            LocalDateTime endDate = month.plusMonths(1).atDay(1).atStartOfDay();
            long count = borrowRepository.countByBorrowDateGreaterThanEqualAndBorrowDateLessThan(startDate, endDate);
            long returned = borrowDetailRepository.countOnTimeReturnsByDateRange(startDate, endDate)
                    + borrowDetailRepository.countLateReturnsByDateRange(startDate, endDate);
            borrowCounts.add(count);
            returnCounts.add(returned);
            maxCount = Math.max(maxCount, Math.max(count, returned));
        }

        for (int i = 0; i < borrowCounts.size(); i++) {
            YearMonth month = currentMonth.minusMonths(5L - i);
            long count = borrowCounts.get(i);
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("label", month.format(DateTimeFormatter.ofPattern("MM/yyyy")));
            stat.put("count", count);
            stat.put("returnCount", returnCounts.get(i));
            stat.put("height", calculateBarHeight(count, maxCount));
            monthStats.add(stat);
        }
        return monthStats;
    }

    private int calculateBarHeight(long count, long maxCount) {
        return maxCount == 0 ? 8 : (int) Math.max(8, count * 120 / maxCount);
    }

    private java.math.BigDecimal defaultAmount(java.math.BigDecimal amount) {
        return amount == null ? java.math.BigDecimal.ZERO : amount;
    }

    private long revenueChangePercent(java.math.BigDecimal current, java.math.BigDecimal previous) {
        java.math.BigDecimal currentAmount = defaultAmount(current);
        java.math.BigDecimal previousAmount = defaultAmount(previous);
        if (previousAmount.signum() == 0) {
            return currentAmount.signum() == 0 ? 0 : 100;
        }
        return currentAmount.subtract(previousAmount)
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(previousAmount, 0, java.math.RoundingMode.HALF_UP)
                .longValue();
    }
}
