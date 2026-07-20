package com.lms.controller.admin;

import com.lms.controller.LocalizedControllerSupport;
import com.lms.entity.MembershipTier;
import com.lms.exception.ApplicationException;
import com.lms.exception.ValidationException;
import com.lms.service.MembershipService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("newTier", new MembershipTier());
        model.addAttribute("memberCounts", membershipService.getMemberCountByTier());
        return "admin/membership-tiers";
    }

    /** Thêm mới hoặc cập nhật hạng thành viên */
    @PostMapping("/save")
    public String saveMembershipTier(@ModelAttribute MembershipTier tier,
                                     RedirectAttributes redirectAttributes) {
        try {
            membershipService.saveTier(tier);
            redirectAttributes.addFlashAttribute("success", message("backend.tier.saved"));
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", message("backend.tier.saveSystemError"));
        }
        return "redirect:/admin/tiers";
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
            redirectAttributes.addFlashAttribute("error", message("backend.tier.deleteSystemError"));
        }
        return "redirect:/admin/tiers";
    }
}
