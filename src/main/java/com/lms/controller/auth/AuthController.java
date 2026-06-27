package com.lms.controller.auth;

import com.lms.dto.request.ForgotPasswordRequest;
import com.lms.dto.request.RegisterRequest;
import com.lms.dto.request.ResetPasswordRequest; // Import mới
import com.lms.service.AuthService;
import jakarta.validation.Valid; // Import mới
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // Import mới
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

    // UC-21.2: Reset Password - Hiển thị trang quên mật khẩu
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "forgot-password";
    }

    // UC-21.2: Reset Password - Xử lý yêu cầu quên mật khẩu
    @PostMapping("/forgot-password")
    public String processForgotPassword(@ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest forgotPasswordRequest,
                                        RedirectAttributes redirectAttributes) {
        try {
            authService.requestPasswordReset(forgotPasswordRequest.getEmail());
            redirectAttributes.addFlashAttribute("successMsg", "Nếu email của bạn tồn tại trong hệ thống, một liên kết đặt lại mật khẩu đã được gửi đến email của bạn.");
            return "redirect:/forgot-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    // UC-21.2: Reset Password - Hiển thị trang đặt lại mật khẩu
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra token có hợp lệ không trước khi hiển thị form
            authService.validatePasswordResetToken(token);
            model.addAttribute("resetPasswordRequest", new ResetPasswordRequest());
            model.addAttribute("token", token); // Truyền token vào form để gửi lại
            return "reset-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/login"; // Hoặc trang lỗi khác
        }
    }

    // UC-21.2: Reset Password - Xử lý đặt lại mật khẩu
    @PostMapping("/reset-password")
    public String processResetPassword(@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest resetPasswordRequest,
                                       BindingResult result,
                                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // Nếu có lỗi validation, quay lại trang reset-password với thông báo lỗi
            redirectAttributes.addFlashAttribute("errorMsg", "Vui lòng kiểm tra lại thông tin nhập.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.resetPasswordRequest", result);
            redirectAttributes.addFlashAttribute("resetPasswordRequest", resetPasswordRequest);
            return "redirect:/reset-password?token=" + resetPasswordRequest.getToken();
        }

        if (!resetPasswordRequest.getNewPassword().equals(resetPasswordRequest.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMsg", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "redirect:/reset-password?token=" + resetPasswordRequest.getToken();
        }

        try {
            authService.resetPassword(resetPasswordRequest.getToken(), resetPasswordRequest.getNewPassword());
            redirectAttributes.addFlashAttribute("successMsg", "Mật khẩu của bạn đã được đặt lại thành công. Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/reset-password?token=" + resetPasswordRequest.getToken();
        }
    }

    // UC-10: Logout - Spring Security tự xử lý qua SecurityConfig
}
