package com.lms.controller.librarian;
import com.lms.exception.ApplicationException;

import com.lms.config.CustomUserDetails;
import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.dto.request.UpdateMemberAccountRequest;
import com.lms.entity.Member;
import com.lms.entity.Transaction;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.FinancialService;
import com.lms.dto.response.MemberListViewData;
import com.lms.service.LibrarianMemberService;
import com.lms.service.OverdueReminderService;
import com.lms.service.OverdueViolationQueryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Librarian member-management UC flows maintained by Pham Kien Quoc:
 * UC-14.1, UC-14.3, UC-21.1, and wallet top-up desk integration.
 */
@Controller
@RequestMapping("/librarian")
public class MemberMgmtController {
    private static final String TOP_UP_TYPE = "TOP_UP";
    private static final int MEMBER_SEARCH_LIMIT = 5;

    private final LibrarianMemberService memberService;
    private final FinancialService financialService;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final OverdueReminderService overdueReminderService;
    private final OverdueViolationQueryService overdueViolationQueryService;

    public MemberMgmtController(LibrarianMemberService memberService,
                                FinancialService financialService,
                                TransactionRepository transactionRepository,
                                MemberRepository memberRepository,
                                OverdueReminderService overdueReminderService,
                                OverdueViolationQueryService overdueViolationQueryService) {
        this.memberService = memberService;
        this.financialService = financialService;
        this.transactionRepository = transactionRepository;
        this.memberRepository = memberRepository;
        this.overdueReminderService = overdueReminderService;
        this.overdueViolationQueryService = overdueViolationQueryService;
    }

    @GetMapping("/members")
    public String viewMemberList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String tier,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        MemberListViewData data = memberService.getMemberList(page, keyword, status, tier);
        model.addAttribute("accounts", data.accounts());
        model.addAttribute("memberByUserId", data.memberByUserId());
        model.addAttribute("tiers", data.tiers());
        model.addAttribute("memberSummary", data.summaryCounts());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedTier", tier);
        addCurrentUser(model, userDetails);
        return "librarian/member-list";
    }

    @PostMapping("/members/create")
    public String createMemberAccount(
            @Valid @ModelAttribute CreateMemberAccountRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateCreate(request));
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("fieldErrors", errors);
            redirectAttributes.addFlashAttribute("formValues", createFormValues(request));
            redirectAttributes.addFlashAttribute("openCreateModal", true);
            return "redirect:/librarian/members";
        }

        memberService.createMember(request);
        redirectAttributes.addFlashAttribute(
                "success", "Tạo tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @GetMapping("/members/create/validate")
    @ResponseBody
    public Map<String, String> validateMemberCreateFields(
            @Valid @ModelAttribute CreateMemberAccountRequest request,
            BindingResult bindingResult) {
        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateCreate(request));
        return errors;
    }

    @PostMapping("/members/edit/{id}")
    public String updateMemberAccount(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateMemberAccountRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateUpdate(id, request));
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error", errors.values().iterator().next());
            return "redirect:/librarian/members";
        }

        memberService.updateMember(id, request);
        redirectAttributes.addFlashAttribute(
                "success", "Cập nhật tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @GetMapping("/members/edit/{id}/validate")
    @ResponseBody
    public Map<String, String> validateMemberUpdateFields(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateMemberAccountRequest request,
            BindingResult bindingResult) {

        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateUpdate(id, request));
        return errors;
    }

    @PostMapping("/members/delete/{id}")
    public String deleteMemberAccount(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        if (!memberService.deactivateMember(id)) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
        } else {
            redirectAttributes.addFlashAttribute(
                    "success", "Xóa tài khoản thành viên thành công.");
        }
        return "redirect:/librarian/members";
    }

    @PostMapping("/members/status/{id}")
    public String changeMemberStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirectAttributes) {
        try {
            memberService.changeMemberStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        redirectAttributes.addAttribute("keyword", keyword);
        return "redirect:/librarian/members";
    }

    @GetMapping("/members/fines")
    public String manageFines(@RequestParam(required = false, defaultValue = "") String memberKeyword,
                              Model model,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        var overdueViolations = overdueViolationQueryService.getActiveOverdueViolations();
        model.addAttribute("fineMembers", memberRepository.findAll());
        model.addAttribute("overdueViolations", overdueViolations);
        model.addAttribute("finePerDay", overdueViolationQueryService.getConfiguredFinePerDay());
        model.addAttribute("overdueViolationCount", overdueViolations.size());
        model.addAttribute("overdueMemberCount", overdueViolations.stream()
                .map(violation -> violation.memberId())
                .distinct()
                .count());
        addCurrentUser(model, userDetails);
        return "librarian/fines";
    }

    @PostMapping("/members/fines/create")
    public String createFine(
            @RequestParam Integer memberId,
            @RequestParam Double amount,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        try {
            financialService.createFine(memberId, amount, reason);
            redirectAttributes.addFlashAttribute("success", "Đã tạo khoản phạt cho thành viên.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/librarian/members/fines";
    }

    @PostMapping("/members/fines/remind/{borrowDetailId}")
    public String remindOverdueMember(
            @PathVariable Integer borrowDetailId,
            RedirectAttributes redirectAttributes) {
        try {
            overdueReminderService.sendReturnReminder(borrowDetailId);
            redirectAttributes.addFlashAttribute(
                    "success", "Đã gửi thông báo nhắc trả sách cho thành viên.");
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/librarian/members/fines";
    }

    @GetMapping("/members/transactions")
    public String viewAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String type,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<Transaction> transactionPage = financialService.getAllTransactions(page, type);

        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedType", type);
        addCurrentUser(model, userDetails);
        return "librarian/transactions";
    }

    @GetMapping("/members/refunds")
    public String viewPendingRefunds(Model model,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("pendingRefundRequests", financialService.getPendingReservationDepositRefunds());
        model.addAttribute("reservationDepositAmount", financialService.getReservationDepositAmount());
        addCurrentUser(model, userDetails);
        return "librarian/refunds";
    }

    @GetMapping("/members/topup")
    public String showTopupDesk(Model model,
                                @RequestParam(required = false, defaultValue = "") String memberKeyword,
                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        addTopupDeskStats(model);
        model.addAttribute("recentTopups", transactionRepository.findTop10ByTransactionTypeIgnoreCaseOrderByTransactionDateDesc(TOP_UP_TYPE));
        model.addAttribute("topupMembers", memberRepository.findAll());
        addCurrentUser(model, userDetails);
        return "librarian/topup-desk";
    }

    @PostMapping("/members/topup")
    public String topUpMemberAccount(
            @RequestParam String memberPhone,
            @RequestParam Double amount,
            RedirectAttributes redirectAttributes) {
        try {
            financialService.topUpMemberAccount(memberPhone, amount);
            redirectAttributes.addFlashAttribute("success", "Nạp tiền vào ví thành viên thành công.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("memberPhone", memberPhone);
            redirectAttributes.addFlashAttribute("amount", amount);
        }

        return "redirect:/librarian/members/topup";
    }

    private void addTopupDeskStats(Model model) {
        LocalDate today = LocalDate.now();
        List<Transaction> todayTopups = transactionRepository.findByTransactionTypeAndDateRange(
                TOP_UP_TYPE,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
        BigDecimal todayTotal = transactionRepository.sumAmountByTransactionTypeAndDateRange(
                TOP_UP_TYPE,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());

        model.addAttribute("todayTopups", todayTopups);
        model.addAttribute("todayTopupCount", todayTopups.size());
        model.addAttribute("todayTopupTotal", todayTotal == null ? BigDecimal.ZERO : todayTotal);
    }

    private Map<String, String> bindingErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    private void mergeErrors(Map<String, String> target, Map<String, String> source) {
        source.forEach(target::putIfAbsent);
    }

    private Map<String, Object> createFormValues(CreateMemberAccountRequest request) {
        Map<String, Object> values = new HashMap<>();
        values.put("fullName", trim(request.getFullName()));
        values.put("email", trim(request.getEmail()));
        values.put("phone", trim(request.getPhone()));
        values.put("username", trim(request.getUsername()));
        values.put("tierId", request.getTierId());
        values.put("status", request.getStatus());
        return values;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private List<Member> searchMembers(String keyword) {
        String normalizedKeyword = trim(keyword);
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        Map<Integer, Member> memberMap = new LinkedHashMap<>();
        if (normalizedKeyword.matches("\\d+")) {
            try {
                memberRepository.findById(Integer.valueOf(normalizedKeyword))
                        .ifPresent(member -> memberMap.put(member.getMemberId(), member));
            } catch (NumberFormatException ignored) {
                // Từ khóa có thể là số điện thoại dài, tiếp tục tìm theo chuỗi.
            }
        }

        memberRepository
                .findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(
                        normalizedKeyword,
                        normalizedKeyword,
                        normalizedKeyword,
                        PageRequest.of(0, MEMBER_SEARCH_LIMIT))
                .getContent()
                .forEach(member -> memberMap.putIfAbsent(member.getMemberId(), member));

        return List.copyOf(memberMap.values());
    }

    private void addCurrentUser(Model model, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
    }
    @GetMapping("/api/member-info")
    @ResponseBody
    public Map<String, Object> getMemberInfo(@RequestParam("identifier") String identifier) {
        Map<String, Object> data = new HashMap<>();
        try {
            Member member = memberRepository.findByUserEmail(identifier.trim())
                    .or(() -> memberRepository.findByUserPhone(identifier.trim()))
                    .orElse(null);
            
            if (member == null || member.getUser() == null) {
                data.put("found", false);
                return data;
            }
            
            data.put("found", true);
            data.put("name", member.getUser().getFullName());
            data.put("email", member.getUser().getEmail());
            data.put("phone", member.getUser().getPhone());
            
            String rawStatus = member.getUser().getStatus().toString();
            String vnStatus = "Hoạt động";
            if ("Inactive".equalsIgnoreCase(rawStatus)) vnStatus = "Khóa";
            else if ("Banned".equalsIgnoreCase(rawStatus)) vnStatus = "Cấm mượn";
            data.put("status", vnStatus);
            data.put("rawStatus", rawStatus);
            
            String tierName = member.getTier() != null ? member.getTier().getTierName() : "Tiêu chuẩn";
            if ("Standard".equalsIgnoreCase(tierName)) tierName = "Hạng Tiêu Chuẩn";
            else if ("Premium".equalsIgnoreCase(tierName)) tierName = "Hạng Cao Cấp";
            else if ("VIP".equalsIgnoreCase(tierName)) tierName = "Hạng VIP";
            data.put("tier", tierName);
            
        } catch (ApplicationException e) {
            data.put("found", false);
            data.put("error", e.getMessage());
        }
        return data;
    }
}
