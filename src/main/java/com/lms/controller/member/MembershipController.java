package com.lms.controller.member;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * MembershipController - Quản lý Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Controller
@RequestMapping("/member/membership")
public class MembershipController {

    // UC-5.1: View Benefits & Privileges
    @GetMapping("/benefits")
    public String viewBenefits(Principal principal, Model model) {
        // TODO: Implement - Lấy MembershipTier của Member hiện tại
        // TODO: Hiển thị quyền lợi theo Tier (Bronze, Silver, Gold, Platinum)
        return "member/benefits";
    }

    // UC-5.2: View Membership Tier
    @GetMapping("/tier")
    public String viewMembershipTier(Principal principal, Model model) {
        // TODO: Implement - Hiển thị Tier hiện tại + điểm tích lũy
        // TODO: Hiển thị tiến trình lên hạng tiếp theo
        return "member/membership-tier";
    }
}
