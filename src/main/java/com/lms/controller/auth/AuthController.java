package com.lms.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - Xử lý Đăng nhập / Đăng xuất / Đăng ký
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
public class AuthController {

    // UC-9: Login - Hiển thị trang đăng nhập
    @GetMapping("/login")
    public String showLoginPage() {
        // TODO: Implement - Trả về view login.html
        return "login";
    }

    // UC-2: Register - Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        // TODO: Implement - Tạo DTO RegisterRequest, truyền vào model
        return "auth/register";
    }

    // UC-2: Register - Xử lý đăng ký thành viên mới
    @PostMapping("/register")
    public String processRegister(Model model) {
        // TODO: Implement - Validate input, gọi AuthService.register()
        // TODO: Mã hóa password bằng BCryptPasswordEncoder
        // TODO: Tạo Account + Member + Wallet
        return "redirect:/login?registered";
    }

    // UC-10: Logout - Spring Security tự xử lý qua SecurityConfig
    // Chỉ cần cấu hình trong SecurityConfig.java
}
