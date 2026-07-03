package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.entity.MemberAccount;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.enums.UserStatus;
import com.lms.repository.MemberAccountRepository;
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

    private final MemberAccountRepository memberAccountRepository;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberMgmtController(MemberAccountRepository memberAccountRepository,
            MemberRepository memberRepository,
            MembershipTierRepository membershipTierRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.memberAccountRepository = memberAccountRepository;
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
        Page<MemberAccount> accounts;

        if (keyword != null && !keyword.trim().isEmpty()) {
            accounts = memberAccountRepository.searchMemberAccounts(keyword.trim(), pageRequest);
        } else {
            accounts = memberAccountRepository.findAll(pageRequest);
        }

        Map<Integer, Member> memberByUserId = new HashMap<>();

        for (MemberAccount account : accounts.getContent()) {
            if (account.getMember() != null && account.getMember().getUser() != null) {
                memberByUserId.put(account.getMember().getUser().getId(), account.getMember());
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

        if (memberAccountRepository.existsByUsername(username.trim())) {
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

        Member member = new Member();
        member.setUser(user);
        member.setTier(tier);
        memberRepository.save(member);

        MemberAccount account = new MemberAccount();
        account.setMember(member);
        account.setUsername(username.trim());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setStatus(status);
        account.getRoles().add(memberRole);
        memberAccountRepository.save(account);

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

        MemberAccount account = memberAccountRepository.findById(id).orElse(null);

        if (account == null || account.getMember() == null || account.getMember().getUser() == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/librarian/members";
        }

        if (memberAccountRepository.existsByUsernameAndIdNot(username.trim(), id)) {
            redirectAttributes.addFlashAttribute("error", "Username đã tồn tại.");
            return "redirect:/librarian/members";
        }

        if (userRepository.existsByEmailAndIdNot(email.trim(), account.getMember().getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại.");
            return "redirect:/librarian/members";
        }

        MembershipTier tier = membershipTierRepository.findById(tierId).orElse(null);

        if (tier == null) {
            redirectAttributes.addFlashAttribute("error", "Hạng thành viên không hợp lệ.");
            return "redirect:/librarian/members";
        }

        User user = account.getMember().getUser();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone.trim());
        user.setStatus(toUserStatus(status));
        userRepository.save(user);

        account.setUsername(username.trim());
        account.setStatus(status);

        Member member = account.getMember();
        member.setTier(tier);
        memberRepository.save(member);
        
        memberAccountRepository.save(account);

        redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành viên thành công.");
        return "redirect:/librarian/members";
    }

    @PostMapping("/members/delete/{id}")
    public String deleteMemberAccount(@PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        MemberAccount account = memberAccountRepository.findById(id).orElse(null);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return "redirect:/librarian/members";
        }

        account.setStatus("Inactive");

        if (account.getMember() != null && account.getMember().getUser() != null) {
            account.getMember().getUser().setStatus(UserStatus.Inactive);
        }

        memberAccountRepository.save(account);

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
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("currentUser", userDetails.getUser());
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