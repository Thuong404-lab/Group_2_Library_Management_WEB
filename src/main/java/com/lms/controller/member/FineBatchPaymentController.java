package com.lms.controller.member;

import com.lms.exception.ApplicationException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.UnauthorizedException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.entity.Member;
import com.lms.repository.MemberRepository;
import com.lms.service.FineBatchPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/member/payments/fines")
public class FineBatchPaymentController extends LocalizedControllerSupport {
    private final FineBatchPaymentService paymentService;
    private final MemberRepository memberRepository;

    public FineBatchPaymentController(FineBatchPaymentService paymentService,
            MemberRepository memberRepository) {
        this.paymentService = paymentService;
        this.memberRepository = memberRepository;
    }

    @PostMapping("/pay-all")
    public String payAll(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Member member = currentMember(principal);
            paymentService.payAllFromWallet(member.getMemberId());
            redirectAttributes.addFlashAttribute("success", message("backend.financial.allFinesPaid"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/member/financial/transactions";
    }

    private Member currentMember(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException(message("backend.payment.loginRequired"));
        }
        String login = principal.getName();
        return memberRepository.findByAccountUsername(login)
                .or(() -> memberRepository.findByUserEmail(login))
                .or(() -> memberRepository.findByUserPhone(login))
                .orElseThrow(() -> new ResourceNotFoundException(message("backend.member.currentNotFound")));
    }
}
