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
import jakarta.transaction.Transactional;
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

    private static final int SYSTEM_ADMIN_ACCOUNT_ID = 1;
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9]+(?:[._%+-][A-Za-z0-9]+)*@"
            + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$";
    private static final String PHONE_PATTERN = "^(?!0{10}$)0\\d{9}$";
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
                            Map.of("tierId", "Không tìm thấy hạng thành viên Thường.")));
        }

        Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new DataProcessingException(
                        "Không tìm thấy role " + roleName + " trong hệ thống."));

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

        if (staffSource && accountId != null && accountId == SYSTEM_ADMIN_ACCOUNT_ID) {
            if (!"Admin".equals(request.getStaffType())) {
                errors.put("staffType", "Không thể thay đổi loại tài khoản của Admin tổng.");
            }
            if (!"Active".equalsIgnoreCase(request.getStatus())) {
                errors.put("status", "Admin tổng phải luôn ở trạng thái hoạt động.");
            }
        }

        if (!staffSource
                && (request.getTierId() == null || !membershipTierRepository.existsById(request.getTierId()))) {
            errors.put("tierId", "Hạng thành viên không hợp lệ.");
        } else if (staffSource && !"Admin".equals(request.getStaffType())
                && !"Librarian".equals(request.getStaffType())) {
            errors.put("staffType", "Loại nhân viên không hợp lệ.");
        }

        if (!isValidStatus(request.getStatus())) {
            errors.put("status", "Trạng thái tài khoản không hợp lệ.");
        } else if (staffSource
                && accountId.equals(currentAccountId)
                && !"Active".equalsIgnoreCase(request.getStatus())) {
            errors.put("status", "Bạn không thể khóa hoặc vô hiệu hóa tài khoản đang đăng nhập.");
        }

        return errors;
    }

    @Override
    @Transactional
    public void deleteAccount(Integer accountId, String source, Integer currentAccountId) {
        if (isStaffSource(source)) {
            if (accountId != null && accountId == SYSTEM_ADMIN_ACCOUNT_ID) {
                throw new AccountFormValidationException(
                        Map.of("status", "Không thể xóa hoặc vô hiệu hóa tài khoản Admin tổng."));
            }
            if (accountId != null && accountId.equals(currentAccountId)) {
                throw new AccountFormValidationException(
                        Map.of("status", "Bạn không thể vô hiệu hóa tài khoản đang đăng nhập."));
            }
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

    @Override
    public String getMemberEmail(Integer accountId) {
        MemberAccount account = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản thành viên."));
        if (account.getUser() == null || account.getUser().getEmail() == null
                || account.getUser().getEmail().isBlank()) {
            throw new ValidationException("Tài khoản thành viên chưa có email để nhận liên kết đặt lại mật khẩu.");
        }
        return account.getUser().getEmail().trim();
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
        StaffAccount account = staffAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản nhân sự."));
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
        MemberAccount account = memberAccountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản thành viên."));
        User user = account.getMember().getUser();
        user.setFullName(trim(request.getFullName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));

        account.setUsername(trim(request.getUsername()));
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
        } else if (fullName.length() > 50) {
            errors.put("fullName", "Họ tên không được vượt quá 50 ký tự.");
        } else if (!fullName.matches(FULL_NAME_PATTERN)) {
            errors.put("fullName", "Họ tên chỉ được chứa chữ cái và khoảng trắng.");
        } else if (!fullName.matches(FULL_NAME_WORD_PATTERN)) {
            errors.put("fullName", "Họ tên chỉ được có tối đa 8 từ và mỗi từ không quá 15 ký tự.");
        } else if (fullName.matches(FULL_NAME_TRIPLE_REPEAT_PATTERN)) {
            errors.put("fullName", "Họ tên không được có một ký tự lặp lại 3 lần liên tiếp.");
        } else if (fullName.matches(FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN)) {
            errors.put("fullName", "Họ tên không được chỉ gồm một ký tự lặp lại.");
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
        } catch (IllegalArgumentException e) {
            return UserStatus.Active;
        }
    }
}
