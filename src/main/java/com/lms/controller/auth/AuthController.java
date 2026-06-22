package com.lms.controller.auth;

import com.lms.dto.request.RegisterRequest;
import com.lms.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController - Xử lý Đăng nhập / Đăng xuất / Đăng ký
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
public class AuthController {


    private AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // UC-9: Login - Hiển thị trang đăng nhập
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // UC-2: Register - Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    // UC-2: Register - Xử lý đăng ký thành viên mới
    @PostMapping("/register")
    public String processRegister(@ModelAttribute("registerRequest") RegisterRequest registerRequest, RedirectAttributes redirectAttributes) {
        try {
            authService.register(registerRequest);
            redirectAttributes.addFlashAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/register";
        }
    }

    // UC-10: Logout - Spring Security tự xử lý qua SecurityConfig
}
