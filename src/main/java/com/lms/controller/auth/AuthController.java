package com.lms.controller.auth;
import com.lms.exception.ApplicationException;

import com.lms.dto.request.ForgotPasswordRequest;
import com.lms.dto.request.RegisterRequest;
import com.lms.dto.request.ResetPasswordRequest;
import com.lms.exception.AuthException;
import com.lms.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController - Xử lý Đăng nhập / Đăng xuất / Đăng ký
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // UC-9: Login - Hiển thị trang đăng nhập
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // Login cho Staff (Admin/Librarian)
    @GetMapping("/staff-login")
    public String showStaffLoginPage() {
        return "staff-login";
    }

    // UC-2: Register - Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    // UC-2: Register - Xử lý đăng ký thành viên mới
    @PostMapping("/register")
    public String processRegister(@ModelAttribute("registerRequest") RegisterRequest registerRequest,
            RedirectAttributes redirectAttributes) {
        try {
            authService.register(registerRequest);
            redirectAttributes.addFlashAttribute("successMsg", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (AuthException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/register";
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Hệ thống đang bảo trì, vui lòng thử lại sau!");
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
    public String processForgotPassword(
            @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest forgotPasswordRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "forgot-password";
        }

        try {
            authService.requestPasswordReset(forgotPasswordRequest.getEmail());
            redirectAttributes.addFlashAttribute("successMsg",
                    "Liên kết đặt lại mật khẩu đã được gửi đến địa chỉ email của bạn. Vui lòng kiểm tra hộp thư để tiếp tục.");
            return "redirect:/forgot-password";
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/forgot-password";
        }
    }

    // UC-21.2: Reset Password - Hiển thị trang đặt lại mật khẩu
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam(value = "token", required = false) String token,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        try {
            if (token != null && !token.isBlank()) {
                authService.validatePasswordResetToken(token);
                session.setAttribute("passwordResetToken", token);
                return "redirect:/reset-password";
            }

            token = (String) session.getAttribute("passwordResetToken");
            if (token == null || token.isBlank()) {
                redirectAttributes.addFlashAttribute("errorMsg", "Token đặt lại mật khẩu không hợp lệ.");
                return "redirect:/login";
            }

            authService.validatePasswordResetToken(token);
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken(token);
            model.addAttribute("resetPasswordRequest", request);
            return "reset-password";
        } catch (ApplicationException e) {
            session.removeAttribute("passwordResetToken");
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/login";
        }
    }

    // UC-21.2: Reset Password - Xử lý đặt lại mật khẩu
    @PostMapping("/reset-password")
    public String processResetPassword(
            @Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest resetPasswordRequest,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        if (result.hasErrors()) {
            return "reset-password";
        }

        if (!resetPasswordRequest.getNewPassword().equals(resetPasswordRequest.getConfirmPassword())) {
            result.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Mật khẩu mới và xác nhận mật khẩu không khớp.");
            return "reset-password";
        }

        try {
            authService.resetPassword(resetPasswordRequest.getToken(), resetPasswordRequest.getNewPassword());
            session.removeAttribute("passwordResetToken");
            redirectAttributes.addFlashAttribute("successMsg",
                    "Mật khẩu của bạn đã được đặt lại thành công. Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (ApplicationException e) {
            session.removeAttribute("passwordResetToken");
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/login";
        }
    }

    // UC-10: Logout - Spring Security tự xử lý qua SecurityConfig
}
