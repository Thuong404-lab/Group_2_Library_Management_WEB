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
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAllAttributes(dashboardService.getDashboardData(bookPage));
        addCurrentUser(model, userDetails);
        return "librarian/dashboard";
    }

    @GetMapping("/librarians")
    public String viewLibrarianList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        LibrarianListViewData data = dashboardService.getLibrarianList(page, keyword);
        model.addAttribute("staffPage", data.staffPage());
        model.addAttribute("accountByUserId", data.accountByUserId());
        model.addAttribute("keyword", keyword);
        addCurrentUser(model, userDetails);
        return "librarian/librarian-list";
    }

    private void addCurrentUser(Model model, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
    }
}
