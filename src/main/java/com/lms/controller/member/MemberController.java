package com.lms.controller.member;

import com.lms.repository.BorrowDetailRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final BorrowDetailRepository borrowDetailRepository;
    private final com.lms.repository.ReservationRepository reservationRepository;

    public MemberController(BorrowDetailRepository borrowDetailRepository,
                            com.lms.repository.ReservationRepository reservationRepository) {
        this.borrowDetailRepository = borrowDetailRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/borrow")
    public String borrow(Principal principal) {
        if (principal == null) return "redirect:/login";
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
