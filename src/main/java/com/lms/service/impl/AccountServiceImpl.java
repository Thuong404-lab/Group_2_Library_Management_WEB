package com.lms.service.impl;

import com.lms.dto.request.AdminAccountCreateRequest;
import com.lms.dto.request.AdminAccountUpdateRequest;
import com.lms.dto.response.AdminAccountListViewData;
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
import com.lms.exception.DataProcessingException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.StaffRepository;
import com.lms.repository.UserRepository;
import com.lms.service.AccountService;
import com.lms.service.AuditLogService;
import com.lms.service.LocalizedMessageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private static final int SYSTEM_ADMIN_ACCOUNT_ID = 1;
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9]+(?:[._%+-][A-Za-z0-9]+)*@"
            + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^(0|\\+84)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-46-9])\\d{7}$";
    private static final String USERNAME_PATTERN = "[a-zA-Z0-9_]{3,20}";
    private static final String FULL_NAME_PATTERN = "^[\\p{L}]+(?:\\s+[\\p{L}]+)*$";
    private static final String FULL_NAME_WORD_PATTERN = "^[\\p{L}]{1,15}(?:\\s+[\\p{L}]{1,15}){0,7}$";
    private static final String FULL_NAME_TRIPLE_REPEAT_PATTERN = ".*([\\p{L}])\\1\\1.*";
    private static final String FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN = "^([\\p{L}])\\1+$";

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
    public AdminAccountListViewData getMemberAccountList(int page, String keyword, String status, String tier) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by("id").ascending());
        String normalizedKeyword = trim(keyword);
        String normalizedStatus = trim(status);
        String normalizedTier = trim(tier);
        Page<MemberAccount> accounts = memberAccountRepository.searchMemberAccountsWithFilters(
                normalizedKeyword, normalizedStatus, normalizedTier, pageable);
        Map<Integer, Member> memberByUserId = new HashMap<>();
        for (MemberAccount account : accounts.getContent()) {
            if (account.getMember() != null && account.getMember().getUser() != null) {
                memberByUserId.put(account.getMember().getUser().getId(), account.getMember());
            }
        }
        List<MembershipTier> tiers = membershipTierRepository.findAll(Sort.by("tierId").ascending());
        Map<String, Long> summaryCounts = new LinkedHashMap<>();
        summaryCounts.put("total", memberAccountRepository.count());
        summaryCounts.put("active", memberAccountRepository.countByStatusIgnoreCase("Active"));
        summaryCounts.put("inactive", memberAccountRepository.countByStatusIgnoreCase("Inactive"));
        summaryCounts.put("blocked", memberAccountRepository.countByStatusIgnoreCase("Blocked"));
        return new AdminAccountListViewData(accounts, memberByUserId, tiers, summaryCounts);
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
        String status = "Active";

        MembershipTier selectedTier = null;
        if ("MEMBER".equals(roleName)) {
            selectedTier = membershipTierRepository.findAll().stream()
                    .filter(tier -> "Regular".equalsIgnoreCase(tier.getTierName()))
                    .findFirst()
                    .orElseThrow(() -> new AccountFormValidationException(
                            Map.of("tierId", messages.get("backend.account.regularTierNotFound"))));
        }

        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new DataProcessingException(
                        messages.get("backend.account.roleNotFound", roleName)));

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
                messages.get("backend.account.audit.created", username, roleName));
    }

    @Override
    @Transactional
    public void updateAccount(AdminAccountUpdateRequest request, Integer currentAccountId) {
        Map<String, String> errors = validateAccountUpdate(request, currentAccountId);
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
    public Map<String, String> validateAccountUpdate(AdminAccountUpdateRequest request, Integer currentAccountId) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean staffSource = isStaffSource(request.getSource());
        Integer accountId = request.getAccountId();

        MemberAccount memberAccount = staffSource ? null : memberAccountRepository.findById(accountId).orElse(null);
        StaffAccount staffAccount = staffSource ? staffAccountRepository.findById(accountId).orElse(null) : null;
        User user = resolveUser(memberAccount, staffAccount);

        if (user == null) {
            errors.put("_global", messages.get("backend.account.notFound"));
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

        if (staffSource && accountId != null && accountId == SYSTEM_ADMIN_ACCOUNT_ID) {
            if (!"Admin".equals(request.getStaffType())) {
                errors.put("staffType", messages.get("backend.account.systemAdminTypeImmutable"));
            }
            if (!"Active".equalsIgnoreCase(request.getStatus())) {
                errors.put("status", messages.get("backend.account.systemAdminMustRemainActive"));
            }
        }

        if (!staffSource
                && (request.getTierId() == null || !membershipTierRepository.existsById(request.getTierId()))) {
            errors.put("tierId", messages.get("validation.tier"));
        } else if (staffSource && !"Admin".equals(request.getStaffType())
                && !"Librarian".equals(request.getStaffType())) {
            errors.put("staffType", messages.get("backend.account.invalidStaffType"));
        }

        if (!isValidStatus(request.getStatus())) {
            errors.put("status", messages.get("validation.status"));
        } else if (staffSource
                && accountId.equals(currentAccountId)
                && !"Active".equalsIgnoreCase(request.getStatus())) {
            errors.put("status", messages.get("backend.account.cannotDeactivateSelf"));
        }

        return errors;
    }

    @Override
    @Transactional
    public void deleteAccount(Integer accountId, String source, Integer currentAccountId) {
        if (isStaffSource(source)) {
            if (accountId != null && accountId == SYSTEM_ADMIN_ACCOUNT_ID) {
                throw new AccountFormValidationException(
                        Map.of("status", messages.get("backend.account.systemAdminCannotDeactivate")));
            }
            if (accountId != null && accountId.equals(currentAccountId)) {
                throw new AccountFormValidationException(
                        Map.of("status", messages.get("backend.account.cannotDeactivateSelf")));
            }
            StaffAccount account = staffAccountRepository.findById(accountId)
                    .orElseThrow(() -> new AccountFormValidationException(
                            Map.of("_global", messages.get("backend.account.notFound"))));
            account.setStatus("Inactive");
            if (account.getStaff() != null && account.getStaff().getUser() != null) {
                account.getStaff().getUser().setStatus(UserStatus.Inactive);
            }
            staffAccountRepository.save(account);
            auditLogService.log(
                    ActionType.DEACTIVATE_ACCOUNT,
                    messages.get("backend.account.audit.deactivatedStaff", account.getUsername()));
            return;
        }

        MemberAccount account = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountFormValidationException(
                        Map.of("_global", messages.get("backend.account.notFound"))));
        account.setStatus("Inactive");
        if (account.getMember() != null && account.getMember().getUser() != null) {
            account.getMember().getUser().setStatus(UserStatus.Inactive);
        }
        memberAccountRepository.save(account);
        auditLogService.log(
                ActionType.DEACTIVATE_ACCOUNT,
                messages.get("backend.account.audit.deactivatedMember", account.getUsername()));
    }

    @Override
    public String getMemberEmail(Integer accountId) {
        MemberAccount account = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.account.memberNotFound")));
        if (account.getUser() == null || account.getUser().getEmail() == null
                || account.getUser().getEmail().isBlank()) {
            throw new ValidationException(messages.get("backend.account.resetEmailMissing"));
        }
        return account.getUser().getEmail().trim();
    }

    @Override
    public Map<String, String> validateAccountCreate(AdminAccountCreateRequest request) {
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
            errors.put("password", messages.get("backend.account.passwordRequired"));
        } else if (password.length() < 6) {
            errors.put("password", messages.get("validation.passwordMin"));
        }

        if (!"MEMBER".equals(roleName) && !"ADMIN".equals(roleName) && !"LIBRARIAN".equals(roleName)) {
            errors.put("accountType", messages.get("backend.account.invalidAccountType"));
        }

        if (!isValidStatus(request.getStatus())) {
            errors.put("status", messages.get("validation.status"));
        }

        if (!errors.containsKey("username")
                && (memberAccountRepository.existsByUsername(username)
                        || staffAccountRepository.existsByUsername(username))) {
            errors.put("username", messages.get("backend.account.usernameExists"));
        }

        if (!errors.containsKey("email") && userRepository.existsByEmail(email)) {
            errors.put("email", messages.get("backend.account.emailUsed"));
        }

        if (!errors.containsKey("phone") && userRepository.existsByPhone(phone)) {
            errors.put("phone", messages.get("backend.account.phoneUsed"));
        }

        if ("MEMBER".equals(roleName)
                && (request.getTierId() == null || !membershipTierRepository.existsById(request.getTierId()))) {
            errors.put("tierId", messages.get("validation.tier"));
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
        StaffAccount account = staffAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.account.staffNotFound")));
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
                messages.get("backend.account.audit.updatedStaff", account.getUsername()));
    }

    private void updateMemberAccount(AdminAccountUpdateRequest request) {
        MemberAccount account = memberAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.account.memberNotFound")));
        User user = account.getMember().getUser();
        user.setFullName(trim(request.getFullName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));

        account.setUsername(trim(request.getUsername()));
        userRepository.save(user);
        memberAccountRepository.save(account);

        auditLogService.log(
                ActionType.UPDATE_ACCOUNT,
                messages.get("backend.account.audit.updatedMember", account.getUsername()));
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
            errors.put("fullName", messages.get("validation.fullNameRequired"));
        } else if (fullName.length() > 50) {
            errors.put("fullName", messages.get("validation.fullNameMax"));
        } else if (!fullName.matches(FULL_NAME_PATTERN)) {
            errors.put("fullName", messages.get("validation.fullNameLetters"));
        } else if (!fullName.matches(FULL_NAME_WORD_PATTERN)) {
            errors.put("fullName", messages.get("validation.fullNameWords"));
        } else if (fullName.matches(FULL_NAME_TRIPLE_REPEAT_PATTERN)) {
            errors.put("fullName", messages.get("validation.fullNameTriple"));
        } else if (fullName.matches(FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN)) {
            errors.put("fullName", messages.get("validation.fullNameRepeated"));
        }
    }

    private void validateUsername(String username, Map<String, String> errors) {
        if (username.isEmpty()) {
            errors.put("username", messages.get("validation.usernameRequired"));
        } else if (!username.matches(USERNAME_PATTERN)) {
            errors.put("username", messages.get("validation.username"));
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
            errors.put("username", messages.get("backend.account.usernameExists"));
        }
    }

    private void validateEmail(String email, Map<String, String> errors) {
        if (email.isEmpty()) {
            errors.put("email", messages.get("validation.emailRequired"));
        } else if (!email.matches(EMAIL_PATTERN)) {
            errors.put("email", messages.get("validation.email"));
        }
    }

    private void validateEmailForUpdate(String email, Integer userId, Map<String, String> errors) {
        validateEmail(email, errors);
        if (!errors.containsKey("email") && userRepository.existsByEmailAndIdNot(email, userId)) {
            errors.put("email", messages.get("backend.account.emailUsed"));
        }
    }

    private void validatePhone(String phone, Map<String, String> errors) {
        if (phone.isEmpty()) {
            errors.put("phone", messages.get("validation.phoneRequired"));
        } else if (!phone.matches(PHONE_PATTERN)) {
            errors.put("phone", messages.get("backend.profile.phoneFormat"));
        }
    }

    private void validatePhoneForUpdate(String phone, Integer userId, Map<String, String> errors) {
        validatePhone(phone, errors);
        if (!errors.containsKey("phone") && userRepository.existsByPhoneAndIdNot(phone, userId)) {
            errors.put("phone", messages.get("backend.account.phoneUsed"));
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
        } catch (IllegalArgumentException e) {
            return UserStatus.Active;
        }
    }
}
