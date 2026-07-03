package com.lms.controller.admin;

import com.lms.service.SystemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            @RequestParam Integer maxRenewals,
            @RequestParam List<Integer> tierIds,
            @RequestParam List<Integer> tierBorrowLimits,
            @RequestParam Double borrowFeePerBook,
            @RequestParam Double finePerDay,
            @RequestParam Double damageCompensationAmount,
            @RequestParam Integer damageCompensationThreshold,
            @RequestParam Integer overdueViolationLockLimit,
            @RequestParam Integer bookDisposalConditionThreshold,
            @RequestParam Double loyalUpgradeSpendingThreshold,
            @RequestParam Integer standardTierId,
            @RequestParam Integer loyalTierId,
            @RequestParam Double depositAmount,
            RedirectAttributes redirectAttributes) {
        try {
            if (tierIds.size() != tierBorrowLimits.size()) {
                throw new IllegalArgumentException("Dữ liệu giới hạn mượn theo hạng không hợp lệ.");
            }

            Map<Integer, Integer> borrowLimitsByTier = new LinkedHashMap<>();
            for (int i = 0; i < tierIds.size(); i++) {
                borrowLimitsByTier.put(tierIds.get(i), tierBorrowLimits.get(i));
            }

            systemService.updateBorrowingPolicies(
                    maxBorrowDays,
                    maxRenewals,
                    borrowLimitsByTier,
                    borrowFeePerBook,
                    finePerDay,
                    damageCompensationAmount,
                    damageCompensationThreshold,
                    overdueViolationLockLimit,
                    bookDisposalConditionThreshold,
                    loyalUpgradeSpendingThreshold,
                    standardTierId,
                    loyalTierId,
                    depositAmount);

            redirectAttributes.addFlashAttribute("success", "Cập nhật cấu hình thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cập nhật cấu hình thất bại.");
        }

        return "redirect:/admin/settings";
    }
}
