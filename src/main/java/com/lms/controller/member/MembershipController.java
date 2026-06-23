package com.lms.controller.member;

// =================================================================
// Huỳnh Gia Hưng bổ sung đoạn này: Import các class cần thiết cho cả 2 UC
import com.lms.entity.MembershipTier;
import com.lms.entity.Member;
import com.lms.service.MembershipService;
import com.lms.repository.MembershipTierRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
// =================================================================

/**
 * MembershipController - Quản lý Hạng Thành viên
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Controller
@RequestMapping("/member/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final MembershipTierRepository membershipTierRepository;

    public MembershipController(MembershipService membershipService, MembershipTierRepository membershipTierRepository) {
        this.membershipService = membershipService;
        this.membershipTierRepository = membershipTierRepository;
    }

    // UC-5.1: View Benefits & Privileges (GIỮ NGUYÊN)
    @GetMapping("/benefits")
    public String viewBenefits(Principal principal, Model model) {
        if (principal != null) {
            Integer currentMemberId = 1;
            MembershipTier tierBenefits = membershipService.getBenefits(currentMemberId);
            model.addAttribute("tierBenefits", tierBenefits);
        }
        return "member/benefits";
    }

    // UC-5.2: View Membership Tier (BỔ SUNG CODE XỬ LÝ)
    @GetMapping("/tier")
    public String viewMembershipTier(Principal principal, Model model) {
        // =================================================================
        // Huỳnh Gia Hưng bổ sung đoạn này: Tính toán tiến trình thăng hạng cho UC-5.2
        if (principal != null) {
            Integer currentMemberId = 1;

            Member member = membershipService.getMembershipTier(currentMemberId);
            List<MembershipTier> allTiers = membershipTierRepository.findAll();

            BigDecimal currentSpent = new BigDecimal("150.00"); // Giả lập số tiền test
            MembershipTier nextTier = null;
            BigDecimal amountNeeded = BigDecimal.ZERO;

            if (member != null && member.getTier() != null) {
                for (MembershipTier tier : allTiers) {
                    if (tier.getCondition().compareTo(member.getTier().getCondition()) > 0) {
                        nextTier = tier;
                        amountNeeded = tier.getCondition().subtract(currentSpent);
                        if (amountNeeded.compareTo(BigDecimal.ZERO) < 0) {
                            amountNeeded = BigDecimal.ZERO;
                        }
                        break;
                    }
                }
            }

            model.addAttribute("member", member);
            model.addAttribute("allTiers", allTiers);
            model.addAttribute("currentSpent", currentSpent);
            model.addAttribute("nextTier", nextTier);
            model.addAttribute("amountNeeded", amountNeeded);
        }
        // =================================================================

        return "member/membership-tier";
    }
}