package com.lms.controller.admin;

import com.lms.controller.LocalizedControllerSupport;
import com.lms.exception.ApplicationException;
import com.lms.service.SystemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SettingsController - Cấu hình Hệ thống
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */

@Controller
@RequestMapping("/admin/settings")
public class SettingsController extends LocalizedControllerSupport {

    private final SystemService systemService;

    public SettingsController(SystemService systemService) {
        this.systemService = systemService;
    }

    // UC-22.1: Manage Borrowing and Return Policies
    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("settingMap", systemService.getSettingMap());
        return "admin/settings";
    }

    @PostMapping("/policies")
    public String updateBorrowingPolicies(@RequestParam Integer maxBorrowDays,
            @RequestParam Integer maxRenewalDays,
            @RequestParam Integer maxRenewalRequests,
            @RequestParam Integer renewalRejectionCooldownHours,
            @RequestParam(required = false) List<Integer> tierIds,
            @RequestParam(required = false) List<Integer> tierBorrowLimits,
            @RequestParam(required = false) List<BigDecimal> tierSpendingConditions,
            @RequestParam BigDecimal borrowFeePerBook,
            @RequestParam BigDecimal finePerDay,
            @RequestParam BigDecimal damageCompensationAmount,
            @RequestParam Integer damageCompensationThreshold,
            @RequestParam Integer overdueViolationLockLimit,
            @RequestParam Integer bookDisposalConditionThreshold,
            @RequestParam BigDecimal depositAmount,
            RedirectAttributes redirectAttributes) {
        try {
            Map<Integer, Integer> borrowLimitsByTier = new HashMap<>();
            Map<Integer, BigDecimal> spendingConditionsByTier = new HashMap<>();
            if (tierIds != null) {
                for (int i = 0; i < tierIds.size(); i++) {
                    Integer tierId = tierIds.get(i);
                    if (tierId != null) {
                        if (tierBorrowLimits != null && i < tierBorrowLimits.size()) {
                            borrowLimitsByTier.put(tierId, tierBorrowLimits.get(i));
                        }
                        if (tierSpendingConditions != null && i < tierSpendingConditions.size()) {
                            spendingConditionsByTier.put(tierId, tierSpendingConditions.get(i));
                        }
                    }
                }
            }

            systemService.updateBorrowingPolicies(
                    maxBorrowDays,
                    maxRenewalDays,
                    maxRenewalRequests,
                    renewalRejectionCooldownHours,
                    borrowLimitsByTier,
                    spendingConditionsByTier,
                    borrowFeePerBook,
                    finePerDay,
                    damageCompensationAmount,
                    damageCompensationThreshold,
                    overdueViolationLockLimit,
                    bookDisposalConditionThreshold,
                    depositAmount);

            redirectAttributes.addFlashAttribute("success", message("backend.settings.updated"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/settings";
    }
}
