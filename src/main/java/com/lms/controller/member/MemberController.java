package com.lms.controller.member;

import com.lms.entity.Member;
import com.lms.repository.BorrowDetailRepository;
import com.lms.service.MembershipService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final MembershipService membershipService;
    private final BorrowDetailRepository borrowDetailRepository;

    public MemberController(MembershipService membershipService, BorrowDetailRepository borrowDetailRepository) {
        this.membershipService = membershipService;
        this.borrowDetailRepository = borrowDetailRepository;
    }

    // Đã sửa đổi: Giữ nguyên dữ liệu thật cũ + Bổ sung dữ liệu bảng xếp hạng thành viên (Top 5)
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        Member member = membershipService.getMemberByUsername(principal.getName());
        model.addAttribute("member", member);

        // 1. Đếm số lượng cuốn sách độc giả này thực sự đang mượn dưới DB (Giữ nguyên của bạn)
        long activeCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        model.addAttribute("activeBorrowsCount", activeCount);

        // 2. BỔ SUNG: Lấy dữ liệu bảng xếp hạng từ Service để truyền ra ngoài giao diện Dashboard
        List<Member> leaderBoard = membershipService.getTopMembersBySpending();
        model.addAttribute("leaderBoard", leaderBoard);

        return "member/dashboard";
    }

    @GetMapping("/borrow")
    public String borrow() { return "member/borrow"; }

    @GetMapping("/wallet")
    public String wallet() { return "member/wallet"; }

    @GetMapping("/notifications")
    public String notifications() {
        return "redirect:/member/interaction/notifications";
    }
}