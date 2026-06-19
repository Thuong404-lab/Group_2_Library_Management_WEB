package com.lms.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * SessionController - Quản lý thiết bị / Phiên đăng nhập
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
@RequestMapping("/member/sessions")
public class SessionController {

    // UC-9.1: Xem danh sách các thiết bị đang đăng nhập
    @GetMapping
    public String viewActiveSessions(Principal principal, Model model) {
        // TODO: Implement - Lấy danh sách session từ SessionRegistry của Spring Security
        // TODO: Truyền danh sách vào model
        return "member/active-sessions";
    }

    // UC-9.1: Đăng xuất từ xa khỏi một thiết bị
    @PostMapping("/revoke")
    public String revokeSession(@RequestParam String sessionId, Principal principal) {
        // TODO: Implement - Tìm session theo ID trong SessionRegistry
        // TODO: Gọi session.expireNow() để buộc thiết bị đó đăng xuất
        // TODO: Gọi AuthService.logLogoutAction(...) để ghi log
        return "redirect:/member/sessions?revoked";
    }

    // UC-9.1: Xem lịch sử đăng nhập gần đây của chính mình
    @GetMapping("/history")
    public String viewLoginHistory(Principal principal, Model model) {
        // TODO: Implement - Query bảng SystemLogs theo accountId hiện tại
        return "member/login-history";
    }
}
