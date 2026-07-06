package com.lms.controller.admin;

import com.lms.service.SystemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    private final SystemService systemService;

    public SettingsController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("policySettings", systemService.getBorrowingPolicySettings());
        return "admin/settings";
    }

    @PostMapping("/policies")
    public String updateBorrowingPolicies(@RequestParam Integer maxBorrowDays,
                                          @RequestParam Integer maxRenewals,
                                          @RequestParam Integer maxBooksPerMember,
                                          @RequestParam Double borrowFeePerBook) {
        systemService.updateBorrowingPolicies(maxBorrowDays, maxRenewals, maxBooksPerMember, borrowFeePerBook);
        return "redirect:/admin/settings?updated";
    }
}
