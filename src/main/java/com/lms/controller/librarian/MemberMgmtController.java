package com.lms.controller.librarian;

import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.config.CustomUserDetails;
import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.dto.request.UpdateMemberAccountRequest;
import com.lms.entity.Member;
import com.lms.entity.Staff;
import com.lms.entity.Transaction;
import com.lms.enums.TransactionChannel;
import com.lms.enums.TransactionStatus;
import com.lms.enums.TransactionType;
import com.lms.repository.MemberRepository;
import com.lms.repository.StaffRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.FinancialService;
import com.lms.service.FineBatchPaymentService;
import com.lms.dto.response.MemberListViewData;
import com.lms.dto.response.TopUpMemberOption;
import com.lms.service.LibrarianMemberService;
import com.lms.service.OverdueReminderService;
import com.lms.service.OverdueViolationQueryService;
import com.lms.service.TopUpPolicy;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.util.UUID;
import java.util.Arrays;

/**
 * Librarian member-management UC flows maintained by Pham Kien Quoc:
 * UC-14.1, UC-14.3, UC-21.1, and wallet top-up desk integration.
 */
@Controller
@RequestMapping("/librarian")
public class MemberMgmtController extends LocalizedControllerSupport {
    private static final String TOP_UP_TYPE = "TOP_UP";

    private final LibrarianMemberService memberService;
    private final FinancialService financialService;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final OverdueReminderService overdueReminderService;
    private final OverdueViolationQueryService overdueViolationQueryService;
    private final FineBatchPaymentService fineBatchPaymentService;
    private final StaffRepository staffRepository;

    public MemberMgmtController(LibrarianMemberService memberService,
            FinancialService financialService,
            TransactionRepository transactionRepository,
            MemberRepository memberRepository,
            OverdueReminderService overdueReminderService,
            OverdueViolationQueryService overdueViolationQueryService,
            FineBatchPaymentService fineBatchPaymentService,
            StaffRepository staffRepository) {
        this.memberService = memberService;
        this.financialService = financialService;
        this.transactionRepository = transactionRepository;
        this.memberRepository = memberRepository;
        this.overdueReminderService = overdueReminderService;
        this.overdueViolationQueryService = overdueViolationQueryService;
        this.fineBatchPaymentService = fineBatchPaymentService;
        this.staffRepository = staffRepository;
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
        model.addAttribute("defaultTier", data.tiers().isEmpty() ? null : data.tiers().get(0));
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
            RedirectAttributes redirectAttributes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String filterStatus,
            @RequestParam(required = false, defaultValue = "") String filterTier) {

        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateCreate(request));
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("fieldErrors", errors);
            redirectAttributes.addFlashAttribute("formValues", createFormValues(request));
            redirectAttributes.addFlashAttribute("openCreateModal", true);
            return redirectToMembers(redirectAttributes, page, keyword, filterStatus, filterTier);
        }

        try {
            memberService.createMember(request);
            redirectAttributes.addFlashAttribute("success", message("backend.member.created"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            redirectAttributes.addFlashAttribute("formValues", createFormValues(request));
            redirectAttributes.addFlashAttribute("openCreateModal", true);
        }
        return redirectToMembers(redirectAttributes, page, keyword, filterStatus, filterTier);
    }

    @PostMapping("/members/edit/{id}")
    public String updateMemberAccount(
            @PathVariable Integer id,
            @Valid @ModelAttribute UpdateMemberAccountRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String filterStatus,
            @RequestParam(required = false, defaultValue = "") String filterTier) {

        Map<String, String> errors = bindingErrors(bindingResult);
        mergeErrors(errors, memberService.validateUpdate(id, request));
        if (!errors.isEmpty()) {
            preserveEditForm(redirectAttributes, id, request, errors);
            return redirectToMembers(redirectAttributes, page, keyword, filterStatus, filterTier);
        }

        try {
            memberService.updateMember(id, request);
            redirectAttributes.addFlashAttribute("success", message("backend.member.updated"));
        } catch (ApplicationException exception) {
            preserveEditForm(redirectAttributes, id, request, Map.of("_global", exception.getMessage()));
        }
        return redirectToMembers(redirectAttributes, page, keyword, filterStatus, filterTier);
    }

    @PostMapping("/members/delete/{id}")
    public String deleteMemberAccount(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String filterStatus,
            @RequestParam(required = false, defaultValue = "") String filterTier) {
        try {
            memberService.deleteMember(id);
            redirectAttributes.addFlashAttribute("success", message("backend.member.deleted"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return redirectToMembers(redirectAttributes, page, keyword, filterStatus, filterTier);
    }

    @PostMapping("/members/status/{id}")
    public String changeMemberStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String filterStatus,
            @RequestParam(required = false, defaultValue = "") String filterTier,
            RedirectAttributes redirectAttributes) {
        try {
            memberService.changeMemberStatus(id, status);
            redirectAttributes.addFlashAttribute("success", message("backend.account.statusUpdated"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToMembers(redirectAttributes, page, keyword, filterStatus, filterTier);
    }

    @GetMapping("/members/fines")
    public String manageFines(Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var overdueViolations = overdueViolationQueryService.getActiveOverdueViolations();
        model.addAttribute("overdueViolations", overdueViolations);
        model.addAttribute("pendingFines", financialService.getPendingFines());
        model.addAttribute("finePerDay", overdueViolationQueryService.getConfiguredFinePerDay());
        model.addAttribute("overdueViolationCount", overdueViolations.size());
        model.addAttribute("overdueMemberCount", overdueViolations.stream()
                .map(violation -> violation.memberId())
                .distinct()
                .count());
        addCurrentUser(model, userDetails);
        return "librarian/fines";
    }

    @GetMapping("/members/fines/payment/{borrowId}")
    public String showBorrowFinePayment(@PathVariable Integer borrowId,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        List<Transaction> fines = fineBatchPaymentService.getPendingForBorrow(borrowId);
        if (fines.isEmpty()) {
            redirectAttributes.addFlashAttribute("success", message("backend.payment.noFinesDue"));
            return "redirect:/librarian/members/fines";
        }
        BigDecimal total = fines.stream()
                .map(fine -> fine.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("borrowId", borrowId);
        model.addAttribute("fines", fines);
        model.addAttribute("total", total);
        model.addAttribute("member", fines.get(0).getWallet().getMember());
        addCurrentUser(model, userDetails);
        return "librarian/fine-return-payment";
    }

    @PostMapping("/members/fines/payment/{borrowId}/cash")
    public String payBorrowFinesByCash(@PathVariable Integer borrowId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            fineBatchPaymentService.payBorrowFinesByCash(borrowId, requireStaff(userDetails));
            redirectAttributes.addFlashAttribute("success", message("backend.fine.cashPaid"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/librarian/members/fines/payment/" + borrowId;
        }
        return "redirect:/librarian/members/fines";
    }

    @PostMapping("/members/fines/payment/{borrowId}/wallet")
    public String payBorrowFinesByWallet(@PathVariable Integer borrowId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            fineBatchPaymentService.payBorrowFinesFromWallet(borrowId, requireStaff(userDetails));
            redirectAttributes.addFlashAttribute("success", message("backend.fine.walletPaid"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/librarian/members/fines/payment/" + borrowId;
        }
        return "redirect:/librarian/members/fines";
    }

    @PostMapping("/members/fines/{fineId}/cash-payment")
    public String payFineByCash(@PathVariable Integer fineId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            financialService.payFineByCash(fineId, requireStaff(userDetails));
            redirectAttributes.addFlashAttribute("success", message("backend.fine.cashPaid"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/librarian/members/fines";
    }

    @PostMapping("/members/fines/{fineId}/wallet-payment")
    public String payFineByWallet(@PathVariable Integer fineId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            financialService.payFineByWalletAtDesk(fineId, requireStaff(userDetails));
            redirectAttributes.addFlashAttribute("success", message("backend.fine.walletPaid"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/librarian/members/fines";
    }

    @PostMapping("/members/fines/remind/{borrowDetailId}")
    public String remindOverdueMember(
            @PathVariable Integer borrowDetailId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            overdueReminderService.sendReturnReminder(borrowDetailId, requireStaff(userDetails));
            redirectAttributes.addFlashAttribute(
                    "success", message("backend.overdue.reminderSent"));
        } catch (ApplicationException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/librarian/members/fines";
    }

    @GetMapping("/members/transactions")
    public String viewAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "q", required = false, defaultValue = "") String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<Transaction> transactionPage = financialService.getAllTransactions(
                page, query, type, status, channel, fromDate, toDate);
        if (transactionPage.getTotalPages() > 0 && page >= transactionPage.getTotalPages()) {
            page = transactionPage.getTotalPages() - 1;
            transactionPage = financialService.getAllTransactions(
                    page, query, type, status, channel, fromDate, toDate);
        }

        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("query", query == null ? "" : query.trim());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedChannel", channel);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        addTransactionOptions(model);
        addCurrentUser(model, userDetails);
        return "librarian/transactions";
    }

    private void addTransactionOptions(Model model) {
        Map<String, String> typeLabels = new LinkedHashMap<>();
        Map<String, String> typeClasses = new LinkedHashMap<>();
        for (TransactionType type : TransactionType.values()) {
            typeLabels.put(type.name(), message(type.getMessageKey()));
            typeClasses.put(type.name(), type.getCssClass());
        }

        Map<String, String> statusOptions = new LinkedHashMap<>();
        Map<String, String> statusLabels = new LinkedHashMap<>();
        Map<String, String> statusClasses = new LinkedHashMap<>();
        Arrays.stream(TransactionStatus.values()).forEach(status -> {
            String label = message(status.getMessageKey());
            statusLabels.put(status.getDatabaseValue(), label);
            statusClasses.put(status.getDatabaseValue(), status.getCssClass());
            if (status != TransactionStatus.PAID) {
                statusOptions.put(status.getDatabaseValue(), label);
            }
        });

        Map<String, String> channelLabels = new LinkedHashMap<>();
        for (TransactionChannel channel : TransactionChannel.values()) {
            channelLabels.put(channel.name(), message(channel.getMessageKey()));
        }

        model.addAttribute("transactionTypeOptions", typeLabels);
        model.addAttribute("transactionTypeLabels", typeLabels);
        model.addAttribute("transactionTypeClasses", typeClasses);
        model.addAttribute("transactionStatusOptions", statusOptions);
        model.addAttribute("transactionStatusLabels", statusLabels);
        model.addAttribute("transactionStatusClasses", statusClasses);
        model.addAttribute("transactionChannelOptions", channelLabels);
        model.addAttribute("transactionChannelLabels", channelLabels);
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        addTopupDeskStats(model);
        model.addAttribute("recentTopups",
                transactionRepository.findRecentCompletedTopUps(
                        TOP_UP_TYPE, TopUpPolicy.COMPLETED_STATUSES,
                        PageRequest.of(0, TopUpPolicy.RECENT_TRANSACTION_LIMIT)));
        model.addAttribute("topupMinAmount", TopUpPolicy.MIN_AMOUNT);
        model.addAttribute("topupMaxAmount", TopUpPolicy.MAX_AMOUNT);
        model.addAttribute("topupQuickAmounts", TopUpPolicy.QUICK_AMOUNTS);
        model.addAttribute("topupRequestId", UUID.randomUUID().toString());
        Object selectedMemberId = model.getAttribute("memberPhone");
        if (selectedMemberId != null) {
            parseMemberId(selectedMemberId.toString()).flatMap(memberId -> memberRepository
                    .searchTopUpMembers("", memberId, PageRequest.of(0, 1)).stream().findFirst())
                    .ifPresent(member -> model.addAttribute("selectedTopupMember", member));
        }
        addCurrentUser(model, userDetails);
        return "librarian/topup-desk";
    }

    @GetMapping("/members/topup/search")
    @ResponseBody
    public List<TopUpMemberOption> searchTopupMembers(
            @RequestParam(name = "q", defaultValue = "") String query) {
        String keyword = query == null ? "" : query.trim();
        Integer memberId = parseMemberId(keyword).orElse(null);
        if (keyword.length() < 2 && memberId == null) {
            return List.of();
        }
        return memberRepository.searchTopUpMembers(
                keyword, memberId, PageRequest.of(0, TopUpPolicy.MEMBER_SEARCH_LIMIT)).getContent();
    }

    @PostMapping("/members/topup")
    public String topUpMemberAccount(
            @RequestParam String memberPhone,
            @RequestParam BigDecimal amount,
            @RequestParam String requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            var staff = requireStaff(userDetails);
            financialService.topUpMemberAccount(memberPhone, amount, requestId, staff);
            redirectAttributes.addFlashAttribute("success", message("backend.topup.success"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("memberPhone", memberPhone);
            redirectAttributes.addFlashAttribute("amount", amount);
        }

        return "redirect:/librarian/members/topup";
    }

    private void addTopupDeskStats(Model model) {
        LocalDate today = LocalDate.now(TopUpPolicy.LIBRARY_ZONE);
        Page<Transaction> todayTopupPage = transactionRepository.findCompletedTopUpsByDateRange(
                TOP_UP_TYPE,
                TopUpPolicy.COMPLETED_STATUSES,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
                PageRequest.of(0, TopUpPolicy.RECENT_TRANSACTION_LIMIT));
        BigDecimal todayTotal = transactionRepository.sumCompletedTopUpsByDateRange(
                TOP_UP_TYPE, TopUpPolicy.COMPLETED_STATUSES,
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        model.addAttribute("todayTopups", todayTopupPage.getContent());
        model.addAttribute("todayTopupCount", todayTopupPage.getTotalElements());
        model.addAttribute("todayTopupTotal", todayTotal == null ? BigDecimal.ZERO : todayTotal);
    }

    private Staff requireStaff(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUser() == null || userDetails.getUser().getId() == null) {
            throw new com.lms.exception.ValidationException(message("backend.financial.staffRequired"));
        }
        return staffRepository.findByUserId(userDetails.getUser().getId())
                .orElseThrow(() -> new com.lms.exception.ValidationException(
                        message("backend.financial.staffRequired")));
    }

    private java.util.Optional<Integer> parseMemberId(String value) {
        if (value == null) {
            return java.util.Optional.empty();
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT)
                .replaceFirst("^(TV-|MEM-)", "");
        if (!normalized.matches("\\d+")) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(Integer.valueOf(normalized));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }

    private Map<String, String> bindingErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        bindingResult.getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
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
        return values;
    }

    private void preserveEditForm(RedirectAttributes redirectAttributes,
            Integer accountId,
            UpdateMemberAccountRequest request,
            Map<String, String> errors) {
        Map<String, Object> values = new HashMap<>();
        values.put("fullName", trim(request.getFullName()));
        values.put("username", trim(request.getUsername()));
        values.put("email", trim(request.getEmail()));
        values.put("phone", trim(request.getPhone()));
        values.put("status", request.getStatus());
        values.put("accountVersion", request.getAccountVersion());
        values.put("userVersion", request.getUserVersion());
        redirectAttributes.addFlashAttribute("editAccountId", accountId);
        redirectAttributes.addFlashAttribute("editFormValues", values);
        redirectAttributes.addFlashAttribute("editFieldErrors", errors);
        redirectAttributes.addFlashAttribute("error", errors.values().iterator().next());
    }

    private String redirectToMembers(RedirectAttributes redirectAttributes,
            int page,
            String keyword,
            String status,
            String tier) {
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        redirectAttributes.addAttribute("keyword", trim(keyword));
        redirectAttributes.addAttribute("status", trim(status));
        redirectAttributes.addAttribute("tier", trim(tier));
        return "redirect:/librarian/members";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
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
            String localizedStatus = message("status.active");
            if ("Inactive".equalsIgnoreCase(rawStatus))
                localizedStatus = message("status.blocked");
            else if ("Banned".equalsIgnoreCase(rawStatus))
                localizedStatus = message("status.banned");
            data.put("status", localizedStatus);
            data.put("rawStatus", rawStatus);

            String tierName = member.getTier() != null ? member.getTier().getTierName() : "Standard";
            if ("Standard".equalsIgnoreCase(tierName))
                tierName = message("tier.standard");
            else if ("Premium".equalsIgnoreCase(tierName))
                tierName = message("tier.premium");
            else if ("VIP".equalsIgnoreCase(tierName))
                tierName = message("tier.vip");
            data.put("tier", tierName);

        } catch (ApplicationException e) {
            data.put("found", false);
            data.put("error", e.getMessage());
        }
        return data;
    }
}
