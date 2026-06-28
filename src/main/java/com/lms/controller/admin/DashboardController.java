package com.lms.controller.admin;

import com.lms.config.CustomUserDetails;
import com.lms.entity.Account;
import com.lms.entity.Staff;
import com.lms.repository.AccountRepository;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardController
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */

@Controller
@RequestMapping("/admin")
public class DashboardController {

    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookItemRepository bookItemRepository;
    private final StaffRepository staffRepository;
    private final AccountRepository accountRepository;

    public DashboardController(BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            ReservationRepository reservationRepository,
            MemberRepository memberRepository,
            BookRepository bookRepository,
            BookItemRepository bookItemRepository,
            StaffRepository staffRepository,
            AccountRepository accountRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.bookItemRepository = bookItemRepository;
        this.staffRepository = staffRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/dashboard")
    public String viewDashboard(Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long activeBorrows = borrowRepository.countByStatusIgnoreCase("Active");
        long pendingReservations = reservationRepository.countByStatusIgnoreCase("Pending");
        long overdueDetails = borrowDetailRepository.countByStatusIgnoreCase("Overdue");
        long totalMembers = memberRepository.count();
        long totalBooks = bookRepository.countByStatusIgnoreCase("Active");
        long availableItems = bookItemRepository.countByStatusIgnoreCase("Available");

        model.addAttribute("activeBorrows", activeBorrows);
        model.addAttribute("pendingBorrows", pendingReservations);
        model.addAttribute("overdueBorrows", overdueDetails);
        model.addAttribute("totalMembers", totalMembers);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("availableItems", availableItems);
        model.addAttribute("recentBorrows", borrowRepository.findTop5ByOrderByBorrowDateDesc());
        model.addAttribute("monthStats", getLastSixMonthStats());
        model.addAttribute("currentDate", LocalDate.now());
        if (userDetails != null && userDetails.getAccount() != null) {
            model.addAttribute("currentUser", userDetails.getAccount().getUser());
        }
        return "admin/dashboard";
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

            long count = borrowRepository
                    .countByBorrowDateGreaterThanEqualAndBorrowDateLessThan(startDate, endDate);
            borrowCounts.add(count);
            if (count > maxCount) {
                maxCount = count;
            }
        }

        for (int i = 0; i < borrowCounts.size(); i++) {
            YearMonth month = currentMonth.minusMonths(5L - i);
            long count = borrowCounts.get(i);
            int height = calculateBarHeight(count, maxCount);
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("label", month.format(DateTimeFormatter.ofPattern("MM/yyyy")));
            stat.put("count", count);
            stat.put("height", height);

            monthStats.add(stat);
        }
        return monthStats;
    }

    private int calculateBarHeight(long count, long maxCount) {
        if (maxCount == 0) {
            return 8;
        }
        return (int) Math.max(8, count * 120 / maxCount);
    }

    @GetMapping("/staff")
    public String viewStaffList(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            Model model) {
        PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("staffId").ascending());
        Page<Staff> staffPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            staffPage = staffRepository.searchStaffByKeyword(keyword.trim(), pageRequest);
        } else {
            staffPage = staffRepository.findAll(pageRequest);
        }
        Map<Integer, Account> accountByUserId = new HashMap<>();

        for (Staff staff : staffPage.getContent()) {
            if (staff.getUser() != null && staff.getUser().getId() != null) {
                accountRepository.findByUserId(staff.getUser().getId())
                        .ifPresent(account -> accountByUserId.put(staff.getUser().getId(), account));
            }
        }
        model.addAttribute("staffPage", staffPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("accountByUserId", accountByUserId);
        return "admin/staff-list";
    }

    @GetMapping("/logs")
    public String viewSystemLogs(@RequestParam(defaultValue = "0") int page,
            Model model) {
        return "admin/system-logs";
    }
}