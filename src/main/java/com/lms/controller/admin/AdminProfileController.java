package com.lms.controller.admin;

import com.lms.entity.User;
import com.lms.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

/**
 * AdminProfileController - Chỉ dành riêng cho vai trò ADMIN
 */
@Controller
@RequestMapping("/admin/profile")
public class AdminProfileController {

    private final ProfileService profileService;

    public AdminProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String viewAdminProfile(Principal principal, Model model) {
        String username = principal.getName();
        User admin = profileService.getProfile(username);
        model.addAttribute("admin", admin);
        return "admin/profile";
    }
}