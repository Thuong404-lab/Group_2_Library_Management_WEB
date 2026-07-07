package com.lms.service.impl;

import com.lms.dto.request.AdminAccountCreateRequest;
import com.lms.dto.request.AdminAccountUpdateRequest;
import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.entity.User;
import com.lms.enums.ActionType;
import com.lms.enums.UserStatus;
import com.lms.exception.AccountFormValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.StaffRepository;
import com.lms.repository.UserRepository;
import com.lms.service.AccountService;
import com.lms.service.AuditLogService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AccountServiceImpl - Business logic quản lý tài khoản của Admin.
 * Áp dụng MVC/Spring Boot: Controller mỏng, Service xử lý nghiệp vụ,
 * Repository chỉ truy vấn dữ liệu.
 *
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
@Service
public class AccountServiceImpl implements AccountService {
    private static final String EMAIL_PATTERN =
            "^[A-Za-z0-9]+(?:[._%+-][A-Za-z0-9]+)*@"
                    + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^(?!0{10}$)0\\d{9}$";
    private static final String USERNAME_PATTERN = "[a-zA-Z0-9_]{3,20}";

    private final MemberAccountRepository memberAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;

    public AccountServiceImpl(MemberAccountRepository memberAccountRepository,
            StaffAccountRepository staffAccountRepository,
            PasswordEncoder passwordEncoder,
            MemberRepository memberRepository,
            MembershipTierRepository membershipTierRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            StaffRepository staffRepository,
            AuditLogService auditLogService) {
        this.memberAccountRepository = memberAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.staffRepository = staffRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public Page<MemberAccount> getMemberAccounts(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return memberAccountRepository.searchMemberAccounts(keyword.trim(), pageable);
        }

        return memberAccountRepository.findAll(pageable);
    }

    @Override
    public Map<Integer, Member> getMemberByUserId(Page<MemberAccount> accounts) {
        Map<Integer, Member> memberByUserId = new HashMap<>();
        for (MemberAccount account : accounts.getContent()) {
            if (account.getMember() != null && account.getMember().getUser() != null) {
                memberByUserId.put(account.getMember().getUser().getId(), account.getMember());
            }
        }
        return memberByUserId;
    }

    @Override
    public List<MembershipTier> getMembershipTiers() {
        return membershipTierRepository.findAll(Sort.by("tierId").ascending());
    }

    @Override
    @Transactional
    public void createAccount(AdminAccountCreateRequest request) {
        Map<String, String> errors = validateAccountCreate(request);
        if (!errors.isEmpty()) {
            throw new AccountFormValidationException(errors);
        }

        String fullName = trim(request.getFullName());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String username = trim(request.getUsername());
        String roleName = normalizeRole(request.getAccountType());
        String status = normalizeStatus(request.getStatus());

        MembershipTier selectedTier = null;
        if ("MEMBER".equals(roleName)) {
            selectedTier = membershipTierRepository.findById(request.getTierId())
                    .orElseThrow(() -> new AccountFormValidationException(
                            Map.of("tierId", "Hạng thành viên không hợp lệ.")));
        }

        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role " + roleName + " trong database."));

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(toUserStatus(status));
        userRepository.save(user);

        if ("MEMBER".equals(roleName)) {
            createMemberAccount(user, selectedTier, username, request.getPassword(), status, role);
        } else {
            createStaffAccount(user, roleName, username, request.getPassword(), status, role);
        }

        auditLogService.log(
                ActionType.CREATE_ACCOUNT,
                "Tạo tài khoản " + username + " với loại " + roleName + ".");
    }

    @Override
    @Transactional
    public void updateAccount(AdminAccountUpdateRequest request) {
        Map<String, String> errors = validateAccountUpdate(request);
        if (!errors.isEmpty()) {
            throw new AccountFormValidationException(errors);
        }

        if (isStaffSource(request.getSource())) {
            updateStaffAccount(request);
        } else {
            updateMemberAccount(request);
        }
    }

    @Override
    public Map<String, String> validateAccountUpdate(AdminAccountUpdateRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean staffSource = isStaffSource(request.getSource());
        Integer accountId = request.getAccountId();

        MemberAccount memberAccount = staffSource ? null : memberAccountRepository.findById(accountId).orElse(null);
        StaffAccount staffAccount = staffSource ? staffAccountRepository.findById(accountId).orElse(null) : null;
        User user = resolveUser(memberAccount, staffAccount);

        if (user == null) {
            errors.put("_global", "Không tìm thấy tài khoản.");
            return errors;
        }

        String fullName = trim(request.getFullName());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String username = trim(request.getUsername());

        validateFullName(fullName, errors);
        validateUsernameForUpdate(username, accountId, staffSource, errors);
        validateEmailForUpdate(email, user.getId(), errors);
        validatePhoneForUpdate(phone, user.getId(), errors);

        if (!staffSource && (request.getTierId() == null || !membershipTierRepository.existsById(request.getTierId()))) {
            errors.put("tierId", "Hạng thành viên không hợp lệ.");
        } else if (staffSource && !"Admin".equals(request.getStaffType()) && !"Librarian".equals(request.getStaffType())) {
            errors.put("staffType", "Loại nhân viên không hợp lệ.");
        }

        if (!isValidStatus(request.getStatus())) {
            errors.put("status", "Trạng thái tài khoản không hợp lệ.");
        }

        return errors;
    }

    @Override
    @Transactional
    public void deleteAccount(Integer accountId, String source) {
        if (isStaffSource(source)) {
            StaffAccount account = staffAccountRepository.findById(accountId)
                    .orElseThrow(() -> new AccountFormValidationException(
                            Map.of("_global", "Không tìm thấy tài khoản.")));
            account.setStatus("Inactive");
            if (account.getStaff() != null && account.getStaff().getUser() != null) {
                account.getStaff().getUser().setStatus(UserStatus.Inactive);
            }
            staffAccountRepository.save(account);
            auditLogService.log(
                    ActionType.DEACTIVATE_ACCOUNT,
                    "Vô hiệu hóa tài khoản nhân sự " + account.getUsername() + ".");
            return;
        }

        MemberAccount account = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountFormValidationException(
                        Map.of("_global", "Không tìm thấy tài khoản.")));
        account.setStatus("Inactive");
        if (account.getMember() != null && account.getMember().getUser() != null) {
            account.getMember().getUser().setStatus(UserStatus.Inactive);
        }
        memberAccountRepository.save(account);
        auditLogService.log(
                ActionType.DEACTIVATE_ACCOUNT,
                "Vô hiệu hóa tài khoản thành viên " + account.getUsername() + ".");
    }

    private Map<String, String> validateAccountCreate(AdminAccountCreateRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        String fullName = trim(request.getFullName());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String username = trim(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword();
        String roleName = normalizeRole(request.getAccountType());

        validateFullName(fullName, errors);
        validateEmail(email, errors);
        validatePhone(phone, errors);
        validateUsername(username, errors);

        if (password.isBlank()) {
            errors.put("password", "Mật khẩu không được để trống.");
        } else if (password.length() < 6) {
            errors.put("password", "Mật khẩu phải có ít nhất 6 ký tự.");
        }

        if (!"MEMBER".equals(roleName) && !"ADMIN".equals(roleName) && !"LIBRARIAN".equals(roleName)) {
            errors.put("accountType", "Loại tài khoản không hợp lệ.");
        }

        if (!isValidStatus(request.getStatus())) {
            errors.put("status", "Trạng thái tài khoản không hợp lệ.");
        }

        if (!errors.containsKey("username")
                && (memberAccountRepository.existsByUsername(username)
                        || staffAccountRepository.existsByUsername(username))) {
            errors.put("username", "Username đã tồn tại.");
        }

        if (!errors.containsKey("email") && userRepository.existsByEmail(email)) {
            errors.put("email", "Email đã được sử dụng.");
        }

        if (!errors.containsKey("phone") && userRepository.existsByPhone(phone)) {
            errors.put("phone", "Số điện thoại đã được sử dụng.");
        }

        if ("MEMBER".equals(roleName)
                && (request.getTierId() == null || !membershipTierRepository.existsById(request.getTierId()))) {
            errors.put("tierId", "Hạng thành viên không hợp lệ.");
        }

        return errors;
    }

    private void createMemberAccount(User user,
            MembershipTier tier,
            String username,
            String password,
            String status,
            Role role) {
        Member member = new Member();
        member.setUser(user);
        member.setTier(tier);
        memberRepository.save(member);

        MemberAccount account = new MemberAccount();
        account.setMember(member);
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setStatus(status);
        account.getRoles().add(role);
        memberAccountRepository.save(account);
    }

    private void createStaffAccount(User user,
            String roleName,
            String username,
            String password,
            String status,
            Role role) {
        Staff staff = new Staff();
        staff.setUser(user);
        staff.setStaffType("ADMIN".equals(roleName) ? "Admin" : "Librarian");
        staffRepository.save(staff);

        StaffAccount account = new StaffAccount();
        account.setStaff(staff);
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setStatus(status);
        account.getRoles().add(role);
        staffAccountRepository.save(account);
    }

    private void updateStaffAccount(AdminAccountUpdateRequest request) {
        StaffAccount account = staffAccountRepository.findById(request.getAccountId()).orElseThrow();
        User user = account.getStaff().getUser();
        updateUser(user, request);

        account.setUsername(trim(request.getUsername()));
        account.setStatus(normalizeStatus(request.getStatus()));

        Staff staff = account.getStaff();
        staff.setStaffType(request.getStaffType().trim());
        staffRepository.save(staff);
        userRepository.save(user);
        staffAccountRepository.save(account);

        auditLogService.log(
                ActionType.UPDATE_ACCOUNT,
                "Cập nhật tài khoản nhân sự " + account.getUsername() + ".");
    }

    private void updateMemberAccount(AdminAccountUpdateRequest request) {
        MemberAccount account = memberAccountRepository.findById(request.getAccountId()).orElseThrow();
        User user = account.getMember().getUser();
        updateUser(user, request);

        account.setUsername(trim(request.getUsername()));
        account.setStatus(normalizeStatus(request.getStatus()));

        Member member = account.getMember();
        MembershipTier tier = membershipTierRepository.findById(request.getTierId()).orElseThrow();
        member.setTier(tier);
        memberRepository.save(member);
        userRepository.save(user);
        memberAccountRepository.save(account);

        auditLogService.log(
                ActionType.UPDATE_ACCOUNT,
                "Cập nhật tài khoản thành viên " + account.getUsername() + ".");
    }

    private void updateUser(User user, AdminAccountUpdateRequest request) {
        user.setFullName(trim(request.getFullName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));
        user.setStatus(toUserStatus(normalizeStatus(request.getStatus())));
    }

    private User resolveUser(MemberAccount memberAccount, StaffAccount staffAccount) {
        if (memberAccount != null && memberAccount.getMember() != null) {
            return memberAccount.getMember().getUser();
        }
        if (staffAccount != null && staffAccount.getStaff() != null) {
            return staffAccount.getStaff().getUser();
        }
        return null;
    }

    private void validateFullName(String fullName, Map<String, String> errors) {
        if (fullName.isEmpty()) {
            errors.put("fullName", "Họ tên không được để trống.");
        }
    }

    private void validateUsername(String username, Map<String, String> errors) {
        if (username.isEmpty()) {
            errors.put("username", "Username không được để trống.");
        } else if (!username.matches(USERNAME_PATTERN)) {
            errors.put("username", "Username phải từ 3-20 ký tự, chỉ gồm chữ cái, chữ số và dấu gạch dưới.");
        }
    }

    private void validateUsernameForUpdate(String username,
            Integer accountId,
            boolean staffSource,
            Map<String, String> errors) {
        validateUsername(username, errors);
        if (errors.containsKey("username")) {
            return;
        }

        boolean duplicate = staffSource
                ? staffAccountRepository.existsByUsernameAndIdNot(username, accountId)
                        || memberAccountRepository.existsByUsername(username)
                : memberAccountRepository.existsByUsernameAndIdNot(username, accountId)
                        || staffAccountRepository.existsByUsername(username);

        if (duplicate) {
            errors.put("username", "Username đã tồn tại.");
        }
    }

    private void validateEmail(String email, Map<String, String> errors) {
        if (email.isEmpty()) {
            errors.put("email", "Email không được để trống.");
        } else if (!email.matches(EMAIL_PATTERN)) {
            errors.put("email", "Email không đúng định dạng.");
        }
    }

    private void validateEmailForUpdate(String email, Integer userId, Map<String, String> errors) {
        validateEmail(email, errors);
        if (!errors.containsKey("email") && userRepository.existsByEmailAndIdNot(email, userId)) {
            errors.put("email", "Email đã được sử dụng.");
        }
    }

    private void validatePhone(String phone, Map<String, String> errors) {
        if (phone.isEmpty()) {
            errors.put("phone", "Số điện thoại không được để trống.");
        } else if (!phone.matches(PHONE_PATTERN)) {
            errors.put("phone",
                    "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng số 0 và không được toàn số 0.");
        }
    }

    private void validatePhoneForUpdate(String phone, Integer userId, Map<String, String> errors) {
        validatePhone(phone, errors);
        if (!errors.containsKey("phone") && userRepository.existsByPhoneAndIdNot(phone, userId)) {
            errors.put("phone", "Số điện thoại đã được sử dụng.");
        }
    }

    private boolean isValidStatus(String status) {
        return "Active".equals(status) || "Inactive".equals(status) || "Blocked".equals(status);
    }

    private boolean isStaffSource(String source) {
        return "staff".equalsIgnoreCase(source);
    }

    private String normalizeRole(String role) {
        return trim(role).toUpperCase();
    }

    private String normalizeStatus(String status) {
        return trim(status).isEmpty() ? "Active" : trim(status);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private UserStatus toUserStatus(String status) {
        try {
            return UserStatus.valueOf(status);
        } catch (Exception e) {
            return UserStatus.Active;
        }
    }
}
