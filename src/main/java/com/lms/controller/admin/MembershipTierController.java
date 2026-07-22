package com.lms.controller.admin;

import com.lms.controller.LocalizedControllerSupport;
import com.lms.dto.request.MembershipTierUpdateRequest;
import com.lms.exception.ApplicationException;
import com.lms.exception.ValidationException;
import com.lms.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MembershipTierController — UC-22.3 Quản lý Hạng thành viên (Admin).
 */
@Controller
@RequestMapping("/admin/tiers")
public class MembershipTierController extends LocalizedControllerSupport {

    private final MembershipService membershipService;

    public MembershipTierController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    /** Xem danh sách hạng thành viên + số lượng member ở mỗi hạng */
    @GetMapping
    public String showMembershipTiers(Model model) {
        model.addAttribute("tiers", membershipService.getAllTiers());
        model.addAttribute("memberCounts", membershipService.getMemberCountByTier());
        return "admin/membership-tiers";
    }

    /** Cập nhật thông số của một hạng thành viên hiện có. */
    @PostMapping("/save")
    public String saveMembershipTier(@Valid @ModelAttribute("tierForm") MembershipTierUpdateRequest tier,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                String errorMessage = error.isBindingFailure()
                        ? message("backend.tier.numberInvalid")
                        : error.getDefaultMessage();
                fieldErrors.putIfAbsent(error.getField(), errorMessage);
            }
            preserveInvalidForm(tier, fieldErrors, redirectAttributes);
            redirectAttributes.addFlashAttribute("error", fieldErrors.values().stream()
                    .findFirst().orElse(message("backend.tier.invalidData")));
            return "redirect:/admin/tiers";
        }
        try {
            int synchronizedMembers = membershipService.updateTier(tier);
            redirectAttributes.addFlashAttribute("success",
                    message("backend.tier.savedWithSync", synchronizedMembers));
        } catch (ValidationException e) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            if (e.getField() != null && !e.getField().isBlank()) {
                fieldErrors.put(e.getField(), e.getMessage());
            }
            preserveInvalidForm(tier, fieldErrors, redirectAttributes);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tiers";
    }

    private void preserveInvalidForm(MembershipTierUpdateRequest tier,
                                     Map<String, String> fieldErrors,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("tierForm", tier);
        redirectAttributes.addFlashAttribute("tierFieldErrors", fieldErrors);
        if (tier != null && tier.getTierId() != null) {
            redirectAttributes.addFlashAttribute("openTierId", tier.getTierId());
        }
    }

    /** Xóa hạng thành viên (chỉ khi không còn member nào đang dùng) */
    @PostMapping("/delete/{id}")
    public String deleteMembershipTier(@PathVariable Integer id,
                                       RedirectAttributes redirectAttributes) {
        try {
            membershipService.deleteTier(id);
            redirectAttributes.addFlashAttribute("success", message("backend.tier.deleted"));
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/tiers";
    }
}
