package com.lms.controller.member;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * MemberController - Điều hướng các trang chức năng của Member.
 * (UI Design Phase - Hardcoded mapping)
 */
@Controller
@RequestMapping("/member")
public class MemberController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "member/dashboard";
    }

    @GetMapping("/borrow")
    public String borrow() {
        return "member/borrow";
    }

    @GetMapping("/wallet")
    public String wallet() {
        return "member/wallet";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "redirect:/member/interaction/notifications";
    }

    // Đã xóa @GetMapping("/profile") vì bị trùng với ProfileController có sẵn trong hệ thống
}
