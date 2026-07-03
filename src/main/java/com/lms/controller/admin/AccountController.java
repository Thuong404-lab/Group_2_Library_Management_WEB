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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/accounts")
public class AccountController {

    private static final String EMAIL_PATTERN =
            "^[A-Za-z0-9]+(?:[._%+-][A-Za-z0-9]+)*@"
                    + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$";

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

        if (!trimmedEmail.matches(EMAIL_PATTERN)) {
            return createAccountFormWithError(model, source, "email", "Email không đúng định dạng.");
        }

        if (trimmedPhone.isEmpty()) {
            return createAccountFormWithError(model, source, "phone",
                    "Số điện thoại không được để trống.");
        }

        if (!trimmedPhone.matches("^(?!0{10}$)0\\d{9}$")) {
            return createAccountFormWithError(model, source, "phone",
                    "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng số 0");
        }

        if (trimmedUsername.isEmpty()) {
            return createAccountFormWithError(model, source, "username", "Username không được để trống.");
        }

        if (!trimmedUsername.matches("[a-zA-Z0-9_]{3,20}")) {
            return createAccountFormWithError(model, source, "username",
                    "Username phải từ 3-20 ký tự, chỉ gồm chữ cái, chữ số và dấu gạch dưới.");
        }

        if (password.isBlank()) {
            return createAccountFormWithError(model, source, "password", "Mật khẩu không được để trống.");
        }

        if (password.length() < 6) {
            return createAccountFormWithError(model, source, "password",
                    "Mật khẩu phải có ít nhất 6 ký tự.");
        }

        if (!roleName.equals("MEMBER") && !roleName.equals("ADMIN") && !roleName.equals("LIBRARIAN")) {
            return createAccountFormWithError(model, source, "accountType", "Loại tài khoản không hợp lệ.");
        }

        if (!isValidStatus(status)) {
            return createAccountFormWithError(model, source, "status",
                    "Trạng thái tài khoản không hợp lệ.");
        }

        if (memberAccountRepository.existsByUsername(trimmedUsername)
                || staffAccountRepository.existsByUsername(trimmedUsername)) {
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
        user.setFullName(trimmedFullName);
        user.setEmail(trimmedEmail);
        user.setPhone(trimmedPhone);
        user.setStatus(toUserStatus(status));
        userRepository.save(user);

        if (roleName.equals("MEMBER")) {
            Member member = new Member();
            member.setUser(user);
            member.setTier(selectedTier);
            memberRepository.save(member);

            MemberAccount account = new MemberAccount();
            account.setMember(member);
            account.setUsername(trimmedUsername);
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
            account.setUsername(trimmedUsername);
            account.setPasswordHash(passwordEncoder.encode(password));
            account.setStatus(status);
            account.getRoles().add(role);
            staffAccountRepository.save(account);
        }

        redirectAttributes.addFlashAttribute("success", "Tạo tài khoản thành công.");
        return redirectBySource(source);
    }

    // Update Account bằng Modal
    @GetMapping("/edit/{id}/validate")
    @ResponseBody
    public Map<String, String> validateAccountUpdate(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "") String fullName,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false) Integer tierId,
            @RequestParam(required = false) String staffType,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "members") String source) {
        return collectAccountUpdateErrors(id, fullName, email, phone, username, tierId, staffType, status, source);
    }

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

        Map<String, String> errors = collectAccountUpdateErrors(
                id, fullName, email, phone, username, tierId, staffType, status, source);
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("editAccountId", id);
            redirectAttributes.addFlashAttribute("editFormValues", editFormValues);
            redirectAttributes.addFlashAttribute("editFieldErrors", errors);
            String modalId = "staff".equalsIgnoreCase(source)
                    ? "updateStaffModal" + id
                    : "updateMemberModal" + id;
            return redirectBySource(source) + "#" + modalId;
        }

        if ("staff".equalsIgnoreCase(source)) {
            StaffAccount account = staffAccountRepository.findById(id).orElseThrow();
            User user = account.getStaff().getUser();
            user.setFullName(trimmedFullName);
            user.setEmail(trimmedEmail);
            user.setPhone(trimmedPhone);
            user.setStatus(toUserStatus(status));

            account.setUsername(trimmedUsername);
            account.setStatus(status);

            Staff staff = account.getStaff();
            staff.setStaffType(staffType.trim());
            staffRepository.save(staff);
            userRepository.save(user);
            staffAccountRepository.save(account);

            redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
            return redirectBySource(source);
        }

        Optional<MemberAccount> memberAccountOpt = memberAccountRepository.findById(id);
        if (memberAccountOpt.isPresent()) {
            MemberAccount account = memberAccountOpt.get();
            User user = account.getMember().getUser();
            user.setFullName(trimmedFullName);
            user.setEmail(trimmedEmail);
            user.setPhone(trimmedPhone);
            user.setStatus(toUserStatus(status));

            account.setUsername(trimmedUsername);
            account.setStatus(status);

            Member member = account.getMember();
            MembershipTier tier = membershipTierRepository.findById(tierId).orElseThrow();
            member.setTier(tier);
            memberRepository.save(member);
            userRepository.save(user);
            memberAccountRepository.save(account);

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

        if ("staff".equalsIgnoreCase(source)) {
            StaffAccount account = staffAccountRepository.findById(id).orElse(null);
            if (account == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
                return redirectBySource(source);
            }
            account.setStatus("Inactive");
            if (account.getStaff().getUser() != null) {
                account.getStaff().getUser().setStatus(UserStatus.Inactive);
            }
            staffAccountRepository.save(account);
            redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công.");
            return redirectBySource(source);
        }

        MemberAccount account = memberAccountRepository.findById(id).orElse(null);
        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản.");
            return redirectBySource(source);
        }
        account.setStatus("Inactive");
        if (account.getMember().getUser() != null) {
            account.getMember().getUser().setStatus(UserStatus.Inactive);
        }
        memberAccountRepository.save(account);
        redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công.");
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

    private String createAccountFormWithError(Model model, String source, String fieldName, String message) {
        model.addAttribute("fieldErrors", Map.of(fieldName, message));
        prepareCreateAccountPage(model, source);
        return "admin/create-account";
    }

    private Map<String, String> collectAccountUpdateErrors(
            Integer accountId,
            String fullName,
            String email,
            String phone,
            String username,
            Integer tierId,
            String staffType,
            String status,
            String source) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean staffSource = "staff".equalsIgnoreCase(source);
        MemberAccount memberAccount = staffSource ? null : memberAccountRepository.findById(accountId).orElse(null);
        StaffAccount staffAccount = staffSource ? staffAccountRepository.findById(accountId).orElse(null) : null;
        User user = memberAccount != null && memberAccount.getMember() != null
                ? memberAccount.getMember().getUser()
                : staffAccount != null && staffAccount.getStaff() != null
                        ? staffAccount.getStaff().getUser()
                        : null;

        if (user == null) {
            errors.put("_global", "Không tìm thấy tài khoản.");
            return errors;
        }

        String trimmedFullName = fullName == null ? "" : fullName.trim();
        String trimmedEmail = email == null ? "" : email.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();
        String trimmedUsername = username == null ? "" : username.trim();

        if (trimmedFullName.isEmpty()) {
            errors.put("fullName", "Họ tên không được để trống.");
        }

        if (trimmedUsername.isEmpty()) {
            errors.put("username", "Username không được để trống.");
        } else if (!trimmedUsername.matches("[a-zA-Z0-9_]{3,20}")) {
            errors.put("username",
                    "Username phải từ 3-20 ký tự, chỉ gồm chữ cái, chữ số và dấu gạch dưới.");
        } else if ((staffSource
                && (staffAccountRepository.existsByUsernameAndIdNot(trimmedUsername, accountId)
                        || memberAccountRepository.existsByUsername(trimmedUsername)))
                || (!staffSource
                        && (memberAccountRepository.existsByUsernameAndIdNot(trimmedUsername, accountId)
                                || staffAccountRepository.existsByUsername(trimmedUsername)))) {
            errors.put("username", "Username đã tồn tại.");
        }

        if (trimmedEmail.isEmpty()) {
            errors.put("email", "Email không được để trống.");
        } else if (!trimmedEmail.matches(EMAIL_PATTERN)) {
            errors.put("email", "Email không đúng định dạng.");
        } else if (userRepository.existsByEmailAndIdNot(trimmedEmail, user.getId())) {
            errors.put("email", "Email đã được sử dụng.");
        }

        if (trimmedPhone.isEmpty()) {
            errors.put("phone", "Số điện thoại không được để trống.");
        } else if (!trimmedPhone.matches("^(?!0{10}$)0\\d{9}$")) {
            errors.put("phone",
                    "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng số 0 và không được toàn số 0.");
        } else if (userRepository.existsByPhoneAndIdNot(trimmedPhone, user.getId())) {
            errors.put("phone", "Số điện thoại đã được sử dụng.");
        }

        if (!staffSource && (tierId == null || !membershipTierRepository.existsById(tierId))) {
            errors.put("tierId", "Hạng thành viên không hợp lệ.");
        } else if (staffSource
                && !"Admin".equals(staffType)
                && !"Librarian".equals(staffType)) {
            errors.put("staffType", "Loại nhân viên không hợp lệ.");
        }

        if (!isValidStatus(status)) {
            errors.put("status", "Trạng thái tài khoản không hợp lệ.");
        }

        return errors;
    }

    private boolean isValidStatus(String status) {
        return "Active".equals(status) || "Inactive".equals(status) || "Blocked".equals(status);
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
