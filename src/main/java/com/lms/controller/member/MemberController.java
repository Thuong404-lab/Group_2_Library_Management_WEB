package com.lms.controller.member;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/member")
public class MemberController {


    @GetMapping("/borrow")
    public String borrow() {
        return "redirect:/member/borrow/management?tab=borrowing";
    }

    @GetMapping("/wallet")
    public String wallet() {
        return "redirect:/member/financial/transactions";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "redirect:/member/interaction/notifications";
    }
}
