package com.lms.controller.member;

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

    // UC-5.1: View Benefits & Privileges
    @GetMapping("/benefits")
    public String viewBenefits(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            String username = principal.getName();
            Member member = membershipService.getMemberByUsername(username);
            
            if (member != null && member.getTier() != null) {
                model.addAttribute("tierBenefits", member.getTier());
            } else {
                model.addAttribute("tierBenefits", null);
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể tải quyền lợi thành viên: " + e.getMessage());
        }
        
        return "member/benefits";
    }

    // UC-5.2: View Membership Tier
    @GetMapping("/tier")
    public String viewMembershipTier(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            String username = principal.getName();
            Member member = membershipService.getMemberByUsername(username);
            List<MembershipTier> allTiers = membershipTierRepository.findAll();

            BigDecimal currentSpent = new BigDecimal("150.00");
            MembershipTier nextTier = null;
            BigDecimal amountNeeded = BigDecimal.ZERO;

            if (member != null && member.getTier() != null && allTiers != null) {
                for (MembershipTier tier : allTiers) {
                    if (tier.getCondition() != null && member.getTier().getCondition() != null) {
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
            }

            model.addAttribute("member", member);
            model.addAttribute("allTiers", allTiers);
            model.addAttribute("currentSpent", currentSpent);
            model.addAttribute("nextTier", nextTier);
            model.addAttribute("amountNeeded", amountNeeded);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể tải thông tin hạng thành viên: " + e.getMessage());
        }

        return "member/membership-tier";
    }
}