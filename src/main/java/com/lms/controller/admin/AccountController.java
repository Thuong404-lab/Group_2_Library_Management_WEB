package com.lms.controller.admin;

import com.lms.entity.MemberAccount;
import com.lms.entity.StaffAccount;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.Staff;
import com.lms.entity.User;
import com.lms.enums.UserStatus;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.StaffRepository;
import com.lms.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    private final MemberAccountRepository memberAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StaffRepository staffRepository;

    public AccountController(MemberAccountRepository memberAccountRepository,
            StaffAccountRepository staffAccountRepository,
            PasswordEncoder passwordEncoder,
            MemberRepository memberRepository,
            MembershipTierRepository membershipTierRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            StaffRepository staffRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.staffRepository = staffRepository;
    }

    // Search Accounts
    @GetMapping
    public String listAccounts(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model) {
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

        return "admin/accounts";
    }

    @GetMapping("/search")
    public String searchAccounts(@RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return listAccounts(page, keyword, model);
    }

    // Create Account
    @GetMapping("/create")
    public String showCreateForm(@RequestParam(required = false, defaultValue = "members") String source,
            Model model) {
        prepareCreateAccountPage(model, source);
        return "admin/create-account";
    }

    @PostMapping("/create")
    @Transactional
    public String createAccount(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String accountType,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(defaultValue = "Active") String status,
            @RequestParam(required = false, defaultValue = "members") String source,
            Model model,
            RedirectAttributes redirectAttributes) {

        String roleName = accountType.trim().toUpperCase();

        if (!roleName.equals("MEMBER") && !roleName.equals("ADMIN") && !roleName.equals("LIBRARIAN")) {
            model.addAttribute("error", "Loại tài khoản không hợp lệ.");
            prepareCreateAccountPage(model, source);
            return "admin/create-account";
        }

        if (memberAccountRepository.existsByUsername(username.trim()) || staffAccountRepository.existsByUsername(username.trim())) {
            model.addAttribute("error", "Username đã tồn tại.");
            prepareCreateAccountPage(model, source);
            return "admin/create-account";
        }

        if (userRepository.existsByEmail(email.trim())) {
            model.addAttribute("error", "Email đã tồn tại.");
            prepareCreateAccountPage(model, source);
            return "admin/create-account";
        }

        MembershipTier selectedTier = null;

        if (roleName.equals("MEMBER")) {
            if (tierId == null) {
                model.addAttribute("error", "Vui lòng chọn hạng thành viên.");
                prepareCreateAccountPage(model, source);
                return "admin/create-account";
            }

            selectedTier = membershipTierRepository.findById(tierId).orElse(null);

            if (selectedTier == null) {
                model.addAttribute("error", "Hạng thành viên không hợp lệ.");
                prepareCreateAccountPage(model, source);
                return "admin/create-account";
            }
        }

        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleName + " trong database."));

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone.trim());
        user.setStatus(toUserStatus(status));
        userRepository.save(user);

        if (roleName.equals("MEMBER")) {
            Member member = new Member();
            member.setUser(user);
            member.setTier(selectedTier);
            memberRepository.save(member);

            MemberAccount account = new MemberAccount();
            account.setMember(member);
            account.setUsername(username.trim());
            account.setPasswordHash(passwordEncoder.encode(password));
            account.setStatus(status);
            account.getRoles().add(role);
            memberAccountRepository.save(account);
        } else {
            Staff staff = new Staff();
            staff.setUser(user);
            if (roleName.equals("ADMIN")) {
                staff.setStaffType("Admin");
            } else {
                staff.setStaffType("Librarian");
            }
            staffRepository.save(staff);

            StaffAccount account = new StaffAccount();
            account.setStaff(staff);
            account.setUsername(username.trim());
            account.setPasswordHash(passwordEncoder.encode(password));
            account.setStatus(status);
            account.getRoles().add(role);
            staffAccountRepository.save(account);
        }

        redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành công.");
        return redirectBySource(source);
    }

    // Update Account bằng Modal
    @PostMapping("/edit/{id}")
    @Transactional
    public String updateAccount(@PathVariable Integer id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam String username,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false) String staffType,
            @RequestParam(defaultValue = "Active") String status,
            @RequestParam(required = false, defaultValue = "members") String source,
            RedirectAttributes redirectAttributes) {

        Optional<MemberAccount> memberAccountOpt = memberAccountRepository.findById(id);
        if (memberAccountOpt.isPresent()) {
            MemberAccount account = memberAccountOpt.get();
            if (memberAccountRepository.existsByUsernameAndIdNot(username.trim(), id)) {
                redirectAttributes.addFlashAttribute("error", "Username đã tồn tại.");
                return redirectBySource(source);
            }

            if (userRepository.existsByEmailAndIdNot(email.trim(), account.getMember().getUser().getId())) {
                redirectAttributes.addFlashAttribute("error", "Email đã tồn tại.");
                return redirectBySource(source);
            }

            User user = account.getMember().getUser();
            user.setFullName(fullName.trim());
            user.setEmail(email.trim());
            user.setPhone(phone.trim());
            user.setStatus(toUserStatus(status));

            account.setUsername(username.trim());
            account.setStatus(status);

            Member member = account.getMember();
            if (tierId == null) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn hạng thành viên.");
                return redirectBySource(source);
            }

            MembershipTier tier = membershipTierRepository.findById(tierId).orElse(null);
            if (tier == null) {
                redirectAttributes.addFlashAttribute("error", "Hạng thành viên không hợp lệ.");
                return redirectBySource(source);
            }

            member.setTier(tier);
            memberRepository.save(member);
            userRepository.save(user);
            memberAccountRepository.save(account);

            redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
            return redirectBySource(source);
        }

        Optional<StaffAccount> staffAccountOpt = staffAccountRepository.findById(id);
        if (staffAccountOpt.isPresent()) {
            StaffAccount account = staffAccountOpt.get();
            if (staffAccountRepository.existsByUsernameAndIdNot(username.trim(), id)) { // Need to add existsByUsernameAndIdNot to StaffAccountRepository!
                redirectAttributes.addFlashAttribute("error", "Username đã tồn tại.");
                return redirectBySource(source);
            }

            if (userRepository.existsByEmailAndIdNot(email.trim(), account.getStaff().getUser().getId())) {
                redirectAttributes.addFlashAttribute("error", "Email đã tồn tại.");
                return redirectBySource(source);
            }

            User user = account.getStaff().getUser();
            user.setFullName(fullName.trim());
            user.setEmail(email.trim());
            user.setPhone(phone.trim());
            user.setStatus(toUserStatus(status));

            account.setUsername(username.trim());
            account.setStatus(status);

            Staff staff = account.getStaff();
            if (staffType != null && !staffType.trim().isEmpty()) {
                String normalizedStaffType = staffType.trim();
                if (!normalizedStaffType.equals("Admin") && !normalizedStaffType.equals("Librarian")) {
                    redirectAttributes.addFlashAttribute("error", "Loại nhân viên không hợp lệ.");
                    return redirectBySource(source);
                }
                staff.setStaffType(normalizedStaffType);
                staffRepository.save(staff);
            }

            userRepository.save(user);
            staffAccountRepository.save(account);

            redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
            return redirectBySource(source);
        }

        redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
        return redirectBySource(source);
    }

    // Delete Account bằng Modal
    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "members") String source,
            RedirectAttributes redirectAttributes) {
        
        Optional<MemberAccount> memberAccountOpt = memberAccountRepository.findById(id);
        if (memberAccountOpt.isPresent()) {
            MemberAccount account = memberAccountOpt.get();
            account.setStatus("Inactive");
            if (account.getMember().getUser() != null) {
                account.getMember().getUser().setStatus(UserStatus.Inactive);
            }
            memberAccountRepository.save(account);
            redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công.");
            return redirectBySource(source);
        }

        Optional<StaffAccount> staffAccountOpt = staffAccountRepository.findById(id);
        if (staffAccountOpt.isPresent()) {
            StaffAccount account = staffAccountOpt.get();
            account.setStatus("Inactive");
            if (account.getStaff().getUser() != null) {
                account.getStaff().getUser().setStatus(UserStatus.Inactive);
            }
            staffAccountRepository.save(account);
            redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công.");
            return redirectBySource(source);
        }

        redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
        return redirectBySource(source);
    }

    private void prepareCreateAccountPage(Model model, String source) {
        model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));

        if ("staff".equalsIgnoreCase(source)) {
            model.addAttribute("source", "staff");
            model.addAttribute("backText", "← Về danh sách nhân viên");
            model.addAttribute("backUrl", "/admin/staff");
        } else {
            model.addAttribute("source", "members");
            model.addAttribute("backText", "← Về danh sách thành viên");
            model.addAttribute("backUrl", "/admin/accounts");
        }
    }

    private String redirectBySource(String source) {
        if ("staff".equalsIgnoreCase(source)) {
            return "redirect:/admin/staff";
        }

        return "redirect:/admin/accounts";
    }

    private UserStatus toUserStatus(String status) {
        try {
            return UserStatus.valueOf(status);
        } catch (Exception e) {
            return UserStatus.Active;
        }
    }
}