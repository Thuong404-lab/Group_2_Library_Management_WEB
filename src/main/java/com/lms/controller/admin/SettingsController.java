package com.lms.controller.admin;
import com.lms.exception.ApplicationException;
import com.lms.exception.ValidationException;

import com.lms.service.SystemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SettingsController - Cấu hình Hệ thống
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    private final SystemService systemService;

    public SettingsController(SystemService systemService) {
        this.systemService = systemService;
    }

    // UC-22.1: Manage Borrowing and Return Policies
    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("settingMap", systemService.getSettingMap());
        model.addAttribute("membershipTiers", systemService.getMembershipTiers());
        return "admin/settings";
    }

    @PostMapping("/policies")
    public String updateBorrowingPolicies(@RequestParam Integer maxBorrowDays,
            @RequestParam Integer maxRenewalDays,
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
            if (tierIds == null || tierBorrowLimits == null || tierSpendingConditions == null
                    || tierIds.isEmpty()
                    || tierIds.size() != tierBorrowLimits.size()
                    || tierIds.size() != tierSpendingConditions.size()) {
                throw new ValidationException("Dữ liệu cấu hình hạng thành viên không hợp lệ.");
            }

            Map<Integer, Integer> borrowLimitsByTier = new LinkedHashMap<>();
            Map<Integer, BigDecimal> spendingConditionsByTier = new LinkedHashMap<>();
            for (int i = 0; i < tierIds.size(); i++) {
                borrowLimitsByTier.put(tierIds.get(i), tierBorrowLimits.get(i));
                spendingConditionsByTier.put(tierIds.get(i), tierSpendingConditions.get(i));
            }

            systemService.updateBorrowingPolicies(
                    maxBorrowDays,
                    maxRenewalDays,
                    borrowLimitsByTier,
                    spendingConditionsByTier,
                    borrowFeePerBook,
                    finePerDay,
                    damageCompensationAmount,
                    damageCompensationThreshold,
                    overdueViolationLockLimit,
                    bookDisposalConditionThreshold,
                    depositAmount);

            redirectAttributes.addFlashAttribute("success", "Cập nhật cấu hình thành công.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/settings";
    }
}
