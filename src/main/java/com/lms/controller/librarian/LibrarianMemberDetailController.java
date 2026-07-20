package com.lms.controller.librarian;

import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.config.CustomUserDetails;
import com.lms.entity.Member;
import com.lms.entity.Transaction;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.FinancialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Read-only member detail flow linked from the librarian member list.
 */
@Controller
@RequestMapping("/librarian/members")
public class LibrarianMemberDetailController extends LocalizedControllerSupport {
    private static final int TRANSACTION_PAGE_SIZE = 8;

    private final MemberRepository memberRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final FinancialService financialService;

    public LibrarianMemberDetailController(MemberRepository memberRepository,
            MemberAccountRepository memberAccountRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            FinancialService financialService) {
        this.memberRepository = memberRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.financialService = financialService;
    }

    @GetMapping("/{memberId}/details")
    public String viewMemberDetail(@PathVariable Integer memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String type,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(
                        () -> new com.lms.exception.ForbiddenException(message("backend.member.notFoundOrForbidden")));

        int safePage = Math.max(page, 0);
        String selectedType = type == null ? "" : type.trim();
        PageRequest pageable = PageRequest.of(safePage, TRANSACTION_PAGE_SIZE);
        Page<Transaction> transactionPage = selectedType.isBlank()
                ? transactionRepository.findByWalletMemberMemberIdOrderByTransactionDateDesc(memberId, pageable)
                : transactionRepository
                        .findByWalletMemberMemberIdAndTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
                                memberId, selectedType, pageable);

        model.addAttribute("member", member);
        model.addAttribute("memberAccount", memberAccountRepository.findByMemberMemberId(memberId).orElse(null));
        model.addAttribute("wallet", walletRepository.findByMemberMemberId(memberId).orElse(null));
        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", safePage);
        model.addAttribute("selectedType", selectedType);
        model.addAttribute("refundableReservations", financialService.getRefundableReservationDeposits(memberId));
        model.addAttribute("reservationDepositAmount", financialService.getReservationDepositAmount());
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
        return "librarian/member-detail";
    }

    @PostMapping("/{memberId}/reservations/{reservationId}/refund")
    public String refundReservationDeposit(@PathVariable Integer memberId,
            @PathVariable Integer reservationId,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            financialService.refundReservationDeposit(memberId, reservationId);
            redirectAttributes.addFlashAttribute("success", message("backend.financial.refundApproved"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        if ("refunds".equals(returnTo)) {
            return "redirect:/librarian/members/refunds";
        }
        return "redirect:/librarian/members/" + memberId + "/details";
    }
}
