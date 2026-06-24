package com.lms.controller.librarian;

import com.lms.entity.Member;
import com.lms.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * MemberMgmtController - Quản lý Hội viên & Tài chính (Phía Thủ thư)
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Controller
@RequestMapping("/librarian/members")
public class MemberMgmtController {

    private final MemberRepository memberRepository;

    public MemberMgmtController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // UC-14.1: View Member List
    @GetMapping
    public String viewMemberList(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(required = false) String search,
                                 Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Member> memberPage;

        if (search == null || search.trim().isEmpty()) {
            memberPage = memberRepository.findAll(pageable);
        } else {
            String keyword = search.trim();
            memberPage = memberRepository
                    .findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(
                            keyword,
                            keyword,
                            keyword,
                            pageable
                    );
        }

        model.addAttribute("memberPage", memberPage);
        model.addAttribute("members", memberPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("search", search);

        return "librarian/member-list";
    }

    // UC-14.2: Manage Fines & Violations
    @GetMapping("/fines")
    public String manageFines(Model model) {
        // TODO: Implement - Hiển thị trang quản lý phạt
        return "librarian/fines";
    }

    @PostMapping("/fines/create")
    public String createFine(@RequestParam Integer memberId,
                             @RequestParam Double amount,
                             @RequestParam String reason,
                             Model model) {
        // TODO: Implement - Tạo khoản phạt mới cho Member
        // TODO: Cập nhật Wallet (trừ tiền, cho phép âm)
        // TODO: Tạo Transaction (type = FINE)
        // TODO: Gửi Notification cho Member
        return "redirect:/librarian/members/fines?created";
    }

    // UC-14.3: View Transaction History (Librarian)
    @GetMapping("/transactions")
    public String viewAllTransactions(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(required = false) String type,
                                      Model model) {
        // TODO: Implement - Xem TOÀN BỘ lịch sử giao dịch hệ thống
        // TODO: Lọc theo loại: TOP_UP, BORROW_FEE, FINE, REFUND
        return "librarian/transactions";
    }

    // UC-14.4: Top Up Member Account
    @GetMapping("/topup")
    public String showTopupDesk(Model model) {
        // TODO: Implement - Hiển thị quầy nạp tiền
        return "librarian/topup-desk";
    }

    @PostMapping("/topup")
    public String topUpMemberAccount(@RequestParam String memberPhone,
                                     @RequestParam Double amount,
                                     Model model) {
        // TODO: Implement - Tìm Member theo SĐT
        // TODO: Cộng tiền vào Wallet
        // TODO: Tạo Transaction (type = TOP_UP)
        // TODO: Gửi Notification xác nhận nạp tiền
        return "redirect:/librarian/members/topup?success";
    }
}
