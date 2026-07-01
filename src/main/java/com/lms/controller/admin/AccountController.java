package com.lms.controller.admin;

import com.lms.entity.Account;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.Staff;
import com.lms.entity.User;
import com.lms.enums.UserStatus;
import com.lms.repository.AccountRepository;
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

/**
 * AccountController - Quản lý tài khoản thành viên
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StaffRepository staffRepository;

    public AccountController(AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            MemberRepository memberRepository,
            MembershipTierRepository membershipTierRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            StaffRepository staffRepository) {
        this.accountRepository = accountRepository;
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
    public String createAccount(@RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String password,
            @RequestParam(required = false, defaultValue = "") String accountType,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "members") String source,
            Model model,
            RedirectAttributes redirectAttributes) {

        String trimmedFullName = fullName.trim();
        String trimmedEmail = email.trim();
        String trimmedPhone = phone.trim();
        String trimmedUsername = username.trim();
        String roleName = accountType.trim().toUpperCase();

        Map<String, Object> formValues = new HashMap<>();
        formValues.put("fullName", trimmedFullName);
        formValues.put("email", trimmedEmail);
        formValues.put("phone", trimmedPhone);
        formValues.put("username", trimmedUsername);
        formValues.put("accountType", roleName);
        formValues.put("tierId", tierId);
        formValues.put("status", status);
        model.addAttribute("formValues", formValues);

        if (trimmedFullName.isEmpty()) {
            return createAccountFormWithError(model, source, "fullName", "Họ tên không được để trống.");
        }

        if (trimmedEmail.isEmpty()) {
            return createAccountFormWithError(model, source, "email", "Email không được để trống.");
        }

        if (!trimmedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return createAccountFormWithError(model, source, "email", "Email không đúng định dạng.");
        }

        if (trimmedPhone.isEmpty()) {
            return createAccountFormWithError(model, source, "phone",
                    "Số điện thoại không được để trống.");
        }

        if (!trimmedPhone.matches("\\d{10}")) {
            return createAccountFormWithError(model, source, "phone",
                    "Số điện thoại phải gồm đúng 10 chữ số.");
        }

        if (trimmedUsername.isEmpty()) {
            return createAccountFormWithError(model, source, "username", "Username không được để trống.");
        }

        if (password.isBlank()) {
            return createAccountFormWithError(model, source, "password", "Mật khẩu không được để trống.");
        }

        if (!roleName.equals("MEMBER") && !roleName.equals("ADMIN") && !roleName.equals("LIBRARIAN")) {
            return createAccountFormWithError(model, source, "accountType", "Loại tài khoản không hợp lệ.");
        }

        if (status.isBlank()) {
            return createAccountFormWithError(model, source, "status",
                    "Vui lòng chọn trạng thái tài khoản.");
        }

        if (accountRepository.existsByUsername(trimmedUsername)) {
            return createAccountFormWithError(model, source, "username", "Username đã tồn tại.");
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            return createAccountFormWithError(model, source, "email", "Email đã được sử dụng.");
        }

        if (userRepository.existsByPhone(trimmedPhone)) {
            return createAccountFormWithError(model, source, "phone", "Số điện thoại đã được sử dụng.");
        }

        MembershipTier selectedTier = null;

        if (roleName.equals("MEMBER")) {
            if (tierId == null) {
                return createAccountFormWithError(model, source, "tierId",
                        "Vui lòng chọn hạng thành viên.");
            }

            selectedTier = membershipTierRepository.findById(tierId).orElse(null);

            if (selectedTier == null) {
                return createAccountFormWithError(model, source, "tierId",
                        "Hạng thành viên không hợp lệ.");
            }
        }

        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleName + " trong database."));

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
        account.getRoles().add(role);
        accountRepository.save(account);

        if (roleName.equals("MEMBER")) {
            Member member = new Member();
            member.setUser(user);
            member.setTier(selectedTier);
            memberRepository.save(member);
        } else {
            Staff staff = new Staff();
            staff.setUser(user);

            if (roleName.equals("ADMIN")) {
                staff.setStaffType("Admin");
            } else {
                staff.setStaffType("Librarian");
            }

            staffRepository.save(staff);
        }

        redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành công.");
        return redirectBySource(source);
    }

    // Update Account bằng Modal
    @PostMapping("/edit/{id}")
    @Transactional
    public String updateAccount(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false) String staffType,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "members") String source,
            RedirectAttributes redirectAttributes) {

        Account account = accountRepository.findById(id).orElse(null);

        if (account == null || account.getUser() == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return redirectBySource(source);
        }

        String trimmedFullName = fullName.trim();
        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();
        String trimmedPhone = phone.trim();

        Map<String, Object> editFormValues = new HashMap<>();
        editFormValues.put("fullName", trimmedFullName);
        editFormValues.put("username", trimmedUsername);
        editFormValues.put("email", trimmedEmail);
        editFormValues.put("phone", trimmedPhone);
        editFormValues.put("tierId", tierId);
        editFormValues.put("staffType", staffType);
        editFormValues.put("status", status);

        if (trimmedFullName.isEmpty()) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "fullName", "Họ tên không được để trống.");
        }

        if (trimmedUsername.isEmpty()) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "username", "Username không được để trống.");
        }

        if (trimmedEmail.isEmpty()) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "email", "Email không được để trống.");
        }

        if (!trimmedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "email", "Email không đúng định dạng.");
        }

        if (trimmedPhone.isEmpty()) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "phone", "Số điện thoại không được để trống.");
        }

        if (!trimmedPhone.matches("\\d{10}")) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "phone", "Số điện thoại phải gồm đúng 10 chữ số.");
        }

        if (accountRepository.existsByUsernameAndAccountIdNot(trimmedUsername, id)) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "username", "Username đã tồn tại.");
        }

        if (userRepository.existsByEmailAndIdNot(trimmedEmail, account.getUser().getId())) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "email", "Email đã được sử dụng.");
        }

        if (userRepository.existsByPhoneAndIdNot(trimmedPhone, account.getUser().getId())) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "phone", "Số điện thoại đã được sử dụng.");
        }

        if (!status.equals("Active") && !status.equals("Inactive") && !status.equals("Blocked")) {
            return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                    "status", "Trạng thái tài khoản không hợp lệ.");
        }

        User user = account.getUser();
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);
        MembershipTier selectedTier = null;
        if (member != null) {
            if (tierId == null) {
                return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                        "tierId", "Vui lòng chọn hạng thành viên.");
            }

            selectedTier = membershipTierRepository.findById(tierId).orElse(null);

            if (selectedTier == null) {
                return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                        "tierId", "Hạng thành viên không hợp lệ.");
            }
        }

        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);
        String normalizedStaffType = null;

        if (staff != null) {
            normalizedStaffType = staffType == null ? "" : staffType.trim();

            if (!normalizedStaffType.equals("Admin") && !normalizedStaffType.equals("Librarian")) {
                return redirectWithEditError(redirectAttributes, source, id, editFormValues,
                        "staffType", normalizedStaffType.isEmpty()
                                ? "Vui lòng chọn loại nhân viên."
                                : "Loại nhân viên không hợp lệ.");
            }
        }

        user.setFullName(trimmedFullName);
        user.setEmail(trimmedEmail);
        user.setPhone(trimmedPhone);
        user.setStatus(toUserStatus(status));

        account.setUsername(trimmedUsername);
        account.setStatus(status);

        if (member != null) {
            member.setTier(selectedTier);
            memberRepository.save(member);
        }

        if (staff != null) {
            staff.setStaffType(normalizedStaffType);
            staffRepository.save(staff);
        }

        userRepository.save(user);
        accountRepository.save(account);

        redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
        return redirectBySource(source);
    }

    // Delete Account bằng Modal
    @PostMapping("/delete/{id}")
    public String deleteAccount(@PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "members") String source,
            RedirectAttributes redirectAttributes) {
        Account account = accountRepository.findById(id).orElse(null);

        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return redirectBySource(source);
        }

        account.setStatus("Inactive");

        if (account.getUser() != null) {
            account.getUser().setStatus(UserStatus.Inactive);
        }

        accountRepository.save(account);

        redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công.");
        return redirectBySource(source);
    }

    private void prepareCreateAccountPage(Model model, String source) {
        model.addAttribute("tiers", membershipTierRepository.findAll(Sort.by("tierId").ascending()));

        if ("staff".equalsIgnoreCase(source)) {
            model.addAttribute("source", "staff");
            model.addAttribute("backText", "← Quản lý nhân sự");
            model.addAttribute("backUrl", "/admin/staff");
        } else {
            model.addAttribute("source", "members");
            model.addAttribute("backText", "← Quản lý thành viên");
            model.addAttribute("backUrl", "/admin/accounts");
        }
    }

    private String createAccountFormWithError(Model model, String source, String fieldName, String message) {
        model.addAttribute("fieldErrors", Map.of(fieldName, message));
        prepareCreateAccountPage(model, source);
        return "admin/create-account";
    }

    private String redirectWithEditError(RedirectAttributes redirectAttributes, String source, Integer accountId,
            Map<String, Object> formValues, String fieldName, String message) {
        redirectAttributes.addFlashAttribute("editAccountId", accountId);
        redirectAttributes.addFlashAttribute("editFormValues", formValues);
        redirectAttributes.addFlashAttribute("editFieldErrors", Map.of(fieldName, message));
        String modalId = "staff".equalsIgnoreCase(source)
                ? "updateStaffModal" + accountId
                : "updateMemberModal" + accountId;
        return redirectBySource(source) + "#" + modalId;
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
