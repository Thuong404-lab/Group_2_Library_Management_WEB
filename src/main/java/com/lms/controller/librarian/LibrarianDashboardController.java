package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.dto.response.LibrarianListViewData;
import com.lms.service.LibrarianDashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/librarian")
public class LibrarianDashboardController {

    private final LibrarianDashboardService dashboardService;

    public LibrarianDashboardController(LibrarianDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String viewDashboard(
            @RequestParam(defaultValue = "0") int bookPage,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model,
            @RequestParam(defaultValue = "0") int reviewPage,
            @RequestParam(defaultValue = "0") int requestPage,
            @RequestParam(required = false, defaultValue = "") String section,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if ("notifications".equalsIgnoreCase(section)) {
            return "redirect:/librarian/interaction/notifications/new";
        }
        if ("reviews".equalsIgnoreCase(section)) {
            return "redirect:/librarian/interaction/reviews";
        }
        if ("acquisition".equalsIgnoreCase(section)) {
            return "redirect:/librarian/interaction/acquisition-requests";
        }
        if ("reports".equalsIgnoreCase(section) || "statistics".equalsIgnoreCase(section)) {
            return "redirect:/librarian/reports";
        }
        if ("users".equalsIgnoreCase(section)) {
            return "redirect:/librarian/members";
        }
        model.mergeAttributes(dashboardService.getDashboardData(bookPage, reviewPage, requestPage, keyword));
        model.addAttribute("keyword", keyword);
        addCurrentUser(model, userDetails);
        return "librarian/dashboard";
    }

    @GetMapping("/users")
    public String viewUserManagement() {
        return "redirect:/librarian/members";
    }

    @GetMapping("/statistics")
    public String viewStatistics() {
        return "redirect:/librarian/reports";
    }

    @GetMapping("/librarians")
    public String viewLibrarianList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String status,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        LibrarianListViewData data = dashboardService.getLibrarianList(page, keyword, status);
        model.addAttribute("staffPage", data.staffPage());
        model.addAttribute("accountByUserId", data.accountByUserId());
        model.addAttribute("librarianSummary", data.summaryCounts());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        addCurrentUser(model, userDetails);
        return "librarian/librarian-list";
    }

    private void addCurrentUser(Model model, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
    }
}
