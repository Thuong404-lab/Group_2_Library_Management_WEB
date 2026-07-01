package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.entity.Account;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.enums.UserStatus;
import com.lms.repository.AccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

@Controller
@RequestMapping("/librarian")
public class MemberMgmtController {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberMgmtController(AccountRepository accountRepository,
            MemberRepository memberRepository,
            MembershipTierRepository membershipTierRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // UC-18.1: Member Accounts Management
    @GetMapping("/members")
    public String viewMemberList(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("accountId").ascending());
        Page<Account> accounts;

        if (keyword != null && !keyword.trim().isEmpty()) {
            accounts = accountRepository.searchMemberAccounts(keyword.trim(), pageRequest);
        } else {
            accounts = accountRepository.findMemberAccounts(pageRequest);
        }

        Map<Integer, Member> memberByUserId = new HashMap<>();

        for (Account account : accounts.getContent()) {
            if (account.getUser() != null && account.getUser().getId() != null) {
                memberRepository.findByUserId(account.getUser().getId())
                        .ifPresent(member -> memberByUserId.put(account.getUser().getId(), member));
            }
        }

        model.addAttribute("accounts", accounts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("memberByUserId", memberByUserId);
        model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));

        addCurrentUser(model, userDetails);

        return "librarian/member-list";
    }

    @GetMapping("/members/create")
    public String showCreateMemberForm(Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));

        addCurrentUser(model, userDetails);

        return "librarian/create-member";
    }

    @PostMapping("/members/create")
    @Transactional
    public String createMemberAccount(@RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String password,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false, defaultValue = "") String status,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String trimmedFullName = fullName.trim();
        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();

        Map<String, Object> formValues = new HashMap<>();
        formValues.put("fullName", trimmedFullName);
        formValues.put("email", trimmedEmail);
        formValues.put("phone", trimmedPhone);
        formValues.put("username", trimmedUsername);
        formValues.put("tierId", tierId);
        formValues.put("status", status);
        model.addAttribute("formValues", formValues);

        if (trimmedFullName.isEmpty()) {
            return createMemberFormWithError(model, userDetails, "fullName", "Họ tên không được để trống.");
        }

        if (trimmedEmail.isEmpty()) {
            return createMemberFormWithError(model, userDetails, "email", "Email không được để trống.");
        }

        if (!trimmedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return createMemberFormWithError(model, userDetails, "email", "Email không đúng định dạng.");
        }

        if (trimmedUsername.isEmpty()) {
            return createMemberFormWithError(model, userDetails, "username", "Username không được để trống.");
        }

        if (password.isBlank()) {
            return createMemberFormWithError(model, userDetails, "password", "Mật khẩu không được để trống.");
        }

        if (accountRepository.existsByUsername(trimmedUsername)) {
            return createMemberFormWithError(model, userDetails, "username", "Username đã tồn tại.");
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            return createMemberFormWithError(model, userDetails, "email", "Email đã được sử dụng.");
        }

        if (trimmedPhone.isEmpty()) {
            return createMemberFormWithError(model, userDetails, "phone",
                    "Số điện thoại không được để trống.");
        }

        if (!trimmedPhone.matches("\\d{10}")) {
            return createMemberFormWithError(model, userDetails, "phone",
                    "Số điện thoại phải gồm đúng 10 chữ số.");
        }

        if (userRepository.existsByPhone(trimmedPhone)) {
            return createMemberFormWithError(model, userDetails, "phone",
                    "Số điện thoại đã được sử dụng.");
        }

        if (tierId == null) {
            return createMemberFormWithError(model, userDetails, "tierId",
                    "Vui lòng chọn hạng thành viên.");
        }

        if (status.isBlank()) {
            return createMemberFormWithError(model, userDetails, "status",
                    "Vui lòng chọn trạng thái thành viên.");
        }

        MembershipTier tier = membershipTierRepository.findById(tierId).orElse(null);

        if (tier == null) {
            return createMemberFormWithError(model, userDetails, "tierId",
                    "Hạng thành viên không hợp lệ.");
        }

        Role memberRole = roleRepository.findByNameIgnoreCase("MEMBER")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role MEMBER trong database."));

        User user = new User();
        user.setFullName(trimmedFullName);
        user.setEmail(trimmedEmail);
        user.setPhone(trimmedPhone);
        user.setStatus(toUserStatus(status));
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setUsername(trimmedUsername);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setStatus(status);
        account.getRoles().add(memberRole);
        accountRepository.save(account);

        Member member = new Member();
        member.setUser(user);
        member.setTier(tier);
        memberRepository.save(member);

        redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @PostMapping("/members/edit/{id}")
    @Transactional
    public String updateMemberAccount(@PathVariable Integer id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam Integer tierId,
            @RequestParam(defaultValue = "Active") String status,
            RedirectAttributes redirectAttributes) {

        Account account = accountRepository.findById(id).orElse(null);

        if (account == null || account.getUser() == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/librarian/members";
        }

        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();

        if (accountRepository.existsByUsernameAndAccountIdNot(trimmedUsername, account.getAccountId())) {
            redirectAttributes.addFlashAttribute("error", "Username đã tồn tại.");
            return "redirect:/librarian/members";
        }

        if (userRepository.existsByEmailAndIdNot(trimmedEmail, account.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng.");
            return "redirect:/librarian/members";
        }

        if (trimmedPhone.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không được để trống.");
            return "redirect:/librarian/members";
        }

        if (!trimmedPhone.matches("\\d{10}")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại phải gồm đúng 10 chữ số.");
            return "redirect:/librarian/members";
        }

        if (userRepository.existsByPhoneAndIdNot(trimmedPhone, account.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại đã được sử dụng.");
            return "redirect:/librarian/members";
        }

        MembershipTier tier = membershipTierRepository.findById(tierId).orElse(null);

        if (tier == null) {
            redirectAttributes.addFlashAttribute("error", "Hạng thành viên không hợp lệ.");
            return "redirect:/librarian/members";
        }

        User user = account.getUser();
        user.setFullName(fullName.trim());
        user.setEmail(trimmedEmail);
        user.setPhone(trimmedPhone);
        user.setStatus(toUserStatus(status));

        account.setUsername(trimmedUsername);
        account.setStatus(status);

        Member member = memberRepository.findByUserId(user.getId()).orElse(new Member());
        member.setUser(user);
        member.setTier(tier);

        userRepository.save(user);
        accountRepository.save(account);
        memberRepository.save(member);

        redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @PostMapping("/members/delete/{id}")
    public String deleteMemberAccount(@PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        Account account = accountRepository.findById(id).orElse(null);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/librarian/members";
        }

        account.setStatus("Inactive");

        if (account.getUser() != null) {
            account.getUser().setStatus(UserStatus.Inactive);
        }

        accountRepository.save(account);

        redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    // UC-14.2: Manage Fines & Violations
    @GetMapping("/members/fines")
    public String manageFines(Model model) {
        // TODO: Implement - Hiển thị trang quản lý phạt
        return "librarian/fines";
    }

    @PostMapping("/members/fines/create")
    public String createFine(@RequestParam Integer memberId,
            @RequestParam Double amount,
            @RequestParam String reason,
            Model model) {
        // TODO: Implement - Tạo khoản phạt mới cho Member
        // TODO: Cập nhật Wallet
        // TODO: Tạo Transaction
        // TODO: Gửi Notification cho Member
        return "redirect:/librarian/members/fines?created";
    }

    // UC-14.3: View Transaction History
    @GetMapping("/members/transactions")
    public String viewAllTransactions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String type,
            Model model) {
        // TODO: Implement - Xem toàn bộ lịch sử giao dịch hệ thống
        // TODO: Lọc theo loại: TOP_UP, BORROW_FEE, FINE, REFUND
        return "librarian/transactions";
    }

    // UC-14.4: Top Up Member Account
    @GetMapping("/members/topup")
    public String showTopupDesk(Model model) {
        // TODO: Implement - Hiển thị quầy nạp tiền
        return "librarian/topup-desk";
    }

    @PostMapping("/members/topup")
    public String topUpMemberAccount(@RequestParam String memberPhone,
            @RequestParam Double amount,
            Model model) {
        // TODO: Implement - Tìm Member theo SĐT
        // TODO: Cộng tiền vào Wallet
        // TODO: Tạo Transaction
        // TODO: Gửi Notification xác nhận nạp tiền
        return "redirect:/librarian/members/topup?success";
    }

    private void addCurrentUser(Model model, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getAccount() != null) {
            model.addAttribute("currentUser", userDetails.getAccount().getUser());
        }
    }

    private String createMemberFormWithError(Model model, CustomUserDetails userDetails,
            String fieldName, String message) {
        model.addAttribute("fieldErrors", Map.of(fieldName, message));
        model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));
        addCurrentUser(model, userDetails);
        return "librarian/create-member";
    }

    private UserStatus toUserStatus(String status) {
        try {
            return UserStatus.valueOf(status);
        } catch (Exception e) {
            return UserStatus.Active;
        }
    }
}
