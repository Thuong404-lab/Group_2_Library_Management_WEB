package com.lms.controller.member;
import com.lms.exception.ApplicationException;

import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.service.MembershipService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/**
 * MembershipController - Quản lý Hạng Thành viên & Quyền lợi
 * Người phụ trách: Huỳnh Gia Hưng (CE190488) - Được cập nhật đồng bộ logic hệ thống động
 */
@Controller
@RequestMapping("/member/membership")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    // UC-5.1: View Benefits & Privileges (Xem chi tiết quyền lợi hạng thẻ)
    @GetMapping("/benefits")
    public String viewMembershipBenefits(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            String username = principal.getName();
            Member member = membershipService.getMemberByUsername(username);
            if (member != null) {
                model.addAttribute("tierBenefits", member.getTier());
            } else {
                model.addAttribute("tierBenefits", null);
            }
        } catch (ApplicationException e) {
            model.addAttribute("errorMessage", "Không thể tải quyền lợi thành viên: " + e.getMessage());
        }

        // Hướng tới view đồng bộ chung
        return "member/membership-benefits";
    }

    // UC-5.2: View Membership Tier (Xem tiến trình nâng hạng thẻ thành viên)
    @GetMapping("/tier")
    public String viewMembershipTierStatus(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            String username = principal.getName();
            Member member = membershipService.getMemberByUsername(username);
            List<MembershipTier> allTiers = membershipService.getAllTiers();

            double currentSpent = 0.0;
            MembershipTier nextTier = null;
            BigDecimal amountNeeded = BigDecimal.ZERO;

            if (member != null) {
                // Lấy chi tiêu thực tế thông qua tầng Service xử lý động
                currentSpent = membershipService.getAccumulatedSpending(member);
                nextTier = membershipService.getNextTier(member.getTier());

                if (nextTier != null && nextTier.getCondition() != null) {
                    // Chuyển đổi currentSpent (double) sang BigDecimal để thực hiện phép trừ chuẩn xác
                    BigDecimal currentSpentBd = BigDecimal.valueOf(currentSpent);

                    // amountNeeded = condition - currentSpent
                    amountNeeded = nextTier.getCondition().subtract(currentSpentBd);

                    // Nếu số tiền cần nạp/chi tiêu thêm âm (đã vượt mốc) thì gán bằng ZERO
                    if (amountNeeded.compareTo(BigDecimal.ZERO) < 0) {
                        amountNeeded = BigDecimal.ZERO;
                    }
                }
            }

            // Đổ toàn bộ các thuộc tính đồng bộ chuẩn sang giao diện Thymeleaf
            model.addAttribute("member", member);
            model.addAttribute("currentSpent", currentSpent);
            model.addAttribute("nextTier", nextTier);
            model.addAttribute("amountNeeded", amountNeeded);
            model.addAttribute("allTiers", allTiers);

        } catch (ApplicationException e) {
            model.addAttribute("errorMessage", "Không thể tải thông tin hạng thành viên: " + e.getMessage());
        }

        return "member/membership-tier";
    }
}