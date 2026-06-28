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

    // Member Accounts Management
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
    public String createMemberAccount(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam Integer tierId,
            @RequestParam(defaultValue = "Active") String status,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (accountRepository.existsByUsername(username.trim())) {
            model.addAttribute("error", "Username đã tồn tại.");
            model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));
            addCurrentUser(model, userDetails);
            return "librarian/create-member";
        }

        if (userRepository.existsByEmail(email.trim())) {
            model.addAttribute("error", "Email đã tồn tại.");
            model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));
            addCurrentUser(model, userDetails);
            return "librarian/create-member";
        }

        MembershipTier tier = membershipTierRepository.findById(tierId).orElse(null);

        if (tier == null) {
            model.addAttribute("error", "Hạng thành viên không hợp lệ.");
            model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));
            addCurrentUser(model, userDetails);
            return "librarian/create-member";
        }

        Role memberRole = roleRepository.findByNameIgnoreCase("MEMBER")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role MEMBER trong database."));

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone.trim());
        user.setStatus(toUserStatus(status));
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setUsername(username.trim());
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

        if (accountRepository.existsByUsernameAndAccountIdNot(username.trim(), id)) {
            redirectAttributes.addFlashAttribute("error", "Username đã tồn tại.");
            return "redirect:/librarian/members";
        }

        if (userRepository.existsByEmailAndIdNot(email.trim(), account.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại.");
            return "redirect:/librarian/members";
        }

        MembershipTier tier = membershipTierRepository.findById(tierId).orElse(null);

        if (tier == null) {
            redirectAttributes.addFlashAttribute("error", "Hạng thành viên không hợp lệ.");
            return "redirect:/librarian/members";
        }

        User user = account.getUser();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone.trim());
        user.setStatus(toUserStatus(status));
        account.setUsername(username.trim());
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

    private void addCurrentUser(Model model, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getAccount() != null) {
            model.addAttribute("currentUser", userDetails.getAccount().getUser());
        }
    }

    private UserStatus toUserStatus(String status) {
        try {
            return UserStatus.valueOf(status);
        } catch (Exception e) {
            return UserStatus.Active;
        }
    }
}