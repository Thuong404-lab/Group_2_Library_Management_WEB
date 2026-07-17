package com.lms.controller.admin;
import com.lms.exception.ApplicationException;

import com.lms.entity.MembershipTier;
import com.lms.service.MembershipService;
import com.lms.exception.ValidationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MembershipTierController - Quản lý Hạng thành viên (Librarian)
 */
@Controller
@RequestMapping("/admin/tiers")
public class MembershipTierController {

    private final MembershipService membershipService;

    public MembershipTierController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    // UC-22.3: Xem danh sách Hạng thành viên
    @GetMapping
    public String showMembershipTiers(Model model) {
        model.addAttribute("tiers", membershipService.getAllTiers());
        model.addAttribute("newTier", new MembershipTier());
        return "admin/membership-tiers";
    }

    // Thêm hoặc Cập nhật Hạng thành viên
    @PostMapping("/save")
    public String saveMembershipTier(@ModelAttribute MembershipTier tier, RedirectAttributes redirectAttributes) {
        try {
            membershipService.saveTier(tier);
            redirectAttributes.addFlashAttribute("success", "Lưu hạng thành viên thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi hệ thống khi lưu hạng thành viên. Vui lòng thử lại sau!");
        }
        return "redirect:/admin/tiers";
    }

    // Xóa Hạng thành viên
    @PostMapping("/delete/{id}")
    public String deleteMembershipTier(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            membershipService.deleteTier(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa hạng thành viên thành công!");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", "Đã xảy ra lỗi hệ thống khi xóa hạng thành viên. Vui lòng thử lại sau!");
        }
        return "redirect:/admin/tiers";
    }
}
