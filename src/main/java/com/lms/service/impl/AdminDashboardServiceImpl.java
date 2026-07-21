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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private static final String ACTIVE_STATUS = "Active";
    private static final String COMPLETED_STATUS = "Completed";
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal ONE_BILLION = BigDecimal.valueOf(1_000_000_000);
    private static final BigDecimal MAX_DISPLAY_PERCENT = BigDecimal.valueOf(999);
    private static final List<String> REVENUE_TRANSACTION_TYPES = List.of(
            "BORROW_FEE", "RENEWAL_FEE", "FINE", "DAMAGE_FEE",
            "PAYMENT", "OVERDUE_FINE", "FEE");

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

        data.put("totalBooks", bookRepository.countByStatusIgnoreCase(ACTIVE_STATUS));
        data.put("totalBookItems", bookItemRepository.countByBook_StatusIgnoreCase(ACTIVE_STATUS));
        data.put("availableItems",
                bookItemRepository.countByBook_StatusIgnoreCaseAndStatusIgnoreCase(ACTIVE_STATUS, "Available"));
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

        BigDecimal monthlyRevenue = transactionRepository.sumRevenueByStatusAndTypesAndDateRange(
                COMPLETED_STATUS, REVENUE_TRANSACTION_TYPES, monthStart, nextMonthStart);
        BigDecimal previousMonthRevenue = transactionRepository.sumRevenueByStatusAndTypesAndDateRange(
                COMPLETED_STATUS, REVENUE_TRANSACTION_TYPES, previousMonthStart, monthStart);
        data.put("monthlyRevenue", defaultAmount(monthlyRevenue));
        data.put("monthlyRevenueDisplayValue", compactRevenueValue(monthlyRevenue));
        data.put("monthlyRevenueDisplayScale", compactRevenueScale(monthlyRevenue));
        data.put("revenueComparisonAvailable", defaultAmount(previousMonthRevenue).signum() > 0);
        BigDecimal revenueChangePercent = revenueChangePercent(monthlyRevenue, previousMonthRevenue);
        data.put("revenueChangeDirection", revenueChangePercent.signum());
        data.put("revenueChangeDisplay", formatRevenueChange(revenueChangePercent));

        data.put("recentLogs", systemLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5)).getContent());
        List<Map<String, Object>> monthStats = getLastSixMonthStats();
        data.put("monthStats", monthStats);
        data.put("hasCirculationData", monthStats.stream()
                .anyMatch(stat -> ((Number) stat.get("count")).longValue() > 0
                        || ((Number) stat.get("returnCount")).longValue() > 0));
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
        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = currentMonth.minusMonths(5);
        LocalDateTime startDate = firstMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<YearMonth, Long> borrowCounts = toMonthlyCounts(
                borrowDetailRepository.countBorrowedItemsByMonth(startDate, endDate));
        Map<YearMonth, Long> returnCounts = toMonthlyCounts(
                borrowDetailRepository.countReturnedItemsByMonth(startDate, endDate));

        for (int i = 0; i < 6; i++) {
            YearMonth month = currentMonth.minusMonths(5L - i);
            long count = borrowCounts.getOrDefault(month, 0L);
            long returnCount = returnCounts.getOrDefault(month, 0L);
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("label", month.format(DateTimeFormatter.ofPattern("MM/yyyy")));
            stat.put("count", count);
            stat.put("returnCount", returnCount);
            monthStats.add(stat);
        }
        return monthStats;
    }

    private Map<YearMonth, Long> toMonthlyCounts(List<Object[]> rows) {
        Map<YearMonth, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            counts.put(YearMonth.of(year, month), ((Number) row[2]).longValue());
        }
        return counts;
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal revenueChangePercent(BigDecimal current, BigDecimal previous) {
        BigDecimal currentAmount = defaultAmount(current);
        BigDecimal previousAmount = defaultAmount(previous);
        if (previousAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return currentAmount.subtract(previousAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousAmount, 0, RoundingMode.HALF_UP);
    }

    private String formatRevenueChange(BigDecimal percent) {
        if (percent.compareTo(MAX_DISPLAY_PERCENT) > 0) {
            return ">999%";
        }
        if (percent.compareTo(MAX_DISPLAY_PERCENT.negate()) < 0) {
            return "<-999%";
        }
        return percent.toPlainString() + "%";
    }

    private String compactRevenueScale(BigDecimal revenue) {
        BigDecimal amount = defaultAmount(revenue).abs();
        if (amount.compareTo(ONE_BILLION) >= 0) {
            return "billion";
        }
        if (amount.compareTo(ONE_MILLION) >= 0) {
            return "million";
        }
        return "full";
    }

    private String compactRevenueValue(BigDecimal revenue) {
        BigDecimal amount = defaultAmount(revenue);
        String scale = compactRevenueScale(amount);
        if ("billion".equals(scale)) {
            return compactNumber(amount.divide(ONE_BILLION, 2, RoundingMode.HALF_UP));
        }
        if ("million".equals(scale)) {
            return compactNumber(amount.divide(ONE_MILLION, 2, RoundingMode.HALF_UP));
        }
        return amount.toPlainString();
    }

    private String compactNumber(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }
}
