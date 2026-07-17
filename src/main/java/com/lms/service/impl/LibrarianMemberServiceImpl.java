package com.lms.service.impl;

import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.dto.request.UpdateMemberAccountRequest;
import com.lms.dto.response.MemberListViewData;
import com.lms.entity.MemberAccount;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.enums.ActionType;
import com.lms.enums.UserStatus;
import com.lms.exception.DataProcessingException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.UserRepository;
import com.lms.service.AuditLogService;
import com.lms.service.LibrarianMemberService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Member account list, update, deactivate, and status changes maintained by
 * Pham Kien Quoc for librarian account-management use cases.
 */
@Service
public class LibrarianMemberServiceImpl implements LibrarianMemberService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final MemberAccountRepository memberAccountRepository;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public LibrarianMemberServiceImpl(
            MemberAccountRepository memberAccountRepository,
            MemberRepository memberRepository,
            MembershipTierRepository membershipTierRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.memberAccountRepository = memberAccountRepository;
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberListViewData getMemberList(int page, String keyword, String status, String tier) {
        PageRequest pageable = PageRequest.of(page, 10, Sort.by("id").ascending());
        String normalizedKeyword = trim(keyword);
        String normalizedStatus = trim(status);
        String normalizedTier = trim(tier);
        Page<MemberAccount> accounts = memberAccountRepository.searchMemberAccountsWithFilters(
                normalizedKeyword, normalizedStatus, normalizedTier, pageable);

        Map<Integer, Member> memberByUserId = new HashMap<>();
        for (MemberAccount account : accounts.getContent()) {
            if (account.getMember().getUser() != null && account.getMember().getUser().getId() != null) {
                memberRepository.findByUserId(account.getMember().getUser().getId())
                        .ifPresent(member -> memberByUserId.put(account.getMember().getUser().getId(), member));
            }
        }
        return new MemberListViewData(accounts, memberByUserId, getMembershipTiers(), getMemberSummaryCounts());
    }

    private Map<String, Long> getMemberSummaryCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", memberAccountRepository.count());
        counts.put("active", memberAccountRepository.countByStatusIgnoreCase("Active"));
        counts.put("inactive", memberAccountRepository.countByStatusIgnoreCase("Inactive"));
        counts.put("blocked", memberAccountRepository.countByStatusIgnoreCase("Blocked"));
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipTier> getMembershipTiers() {
        return membershipTierRepository.findAll(Sort.by("tierId").ascending());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> validateCreate(CreateMemberAccountRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String username = trim(request.getUsername());

        if (!email.isEmpty() && userRepository.existsByEmail(email)) {
            errors.put("email", messages.get("backend.account.emailUsed"));
        }
        if (!phone.isEmpty() && userRepository.existsByPhone(phone)) {
            errors.put("phone", messages.get("backend.account.phoneUsed"));
        }
        if (!username.isEmpty() && memberAccountRepository.existsByUsername(username)) {
            errors.put("username", messages.get("backend.account.usernameExists"));
        }
        if (request.getTierId() != null && !membershipTierRepository.existsById(request.getTierId())) {
            errors.put("tierId", messages.get("validation.tier"));
        }
        if (request.getStatus() != null
                && !"Active".equals(request.getStatus())
                && !"Inactive".equals(request.getStatus())) {
            errors.put("status", messages.get("validation.status"));
        }
        return errors;
    }

    @Override
    @Transactional
    public void createMember(CreateMemberAccountRequest request) {
        Map<String, String> errors = validateCreate(request);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors.values().iterator().next());
        }

        MembershipTier tier = membershipTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new ValidationException(messages.get("validation.tier")));
        Role memberRole = roleRepository.findByNameIgnoreCase("MEMBER")
                .orElseThrow(() -> new DataProcessingException(messages.get("backend.account.roleNotFound", "MEMBER")));

        User user = new User();
        user.setFullName(trim(request.getFullName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));
        user.setStatus(toUserStatus(request.getStatus()));
        userRepository.save(user);

        Member member = new Member();
        member.setUser(user);
        member.setTier(tier);
        memberRepository.save(member);

        MemberAccount account = new MemberAccount();
        account.setMember(member);
        account.setUsername(trim(request.getUsername()));
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setStatus(request.getStatus());
        account.getRoles().add(memberRole);
        memberAccountRepository.save(account);

        auditLogService.log(
                ActionType.CREATE_ACCOUNT,
                messages.get("backend.account.audit.createdMember", account.getUsername()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> validateUpdate(Integer accountId, UpdateMemberAccountRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        MemberAccount account = memberAccountRepository.findById(accountId).orElse(null);
        if (account == null || account.getMember().getUser() == null) {
            errors.put("_global", messages.get("backend.account.updateTargetNotFound"));
            return errors;
        }

        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());

        if (!username.isEmpty()
                && memberAccountRepository.existsByUsernameAndIdNot(username, account.getId())) {
            errors.put("username", messages.get("backend.account.usernameExists"));
        }
        if (!email.isEmpty() && userRepository.existsByEmailAndIdNot(email, account.getMember().getUser().getId())) {
            errors.put("email", messages.get("backend.account.emailUsed"));
        }
        if (!phone.isEmpty() && userRepository.existsByPhoneAndIdNot(phone, account.getMember().getUser().getId())) {
            errors.put("phone", messages.get("backend.account.phoneUsed"));
        }
        if (request.getTierId() != null && !membershipTierRepository.existsById(request.getTierId())) {
            errors.put("tierId", messages.get("validation.tier"));
        }
        if (request.getStatus() != null
                && !"Active".equals(request.getStatus())
                && !"Inactive".equals(request.getStatus())
                && !"Blocked".equals(request.getStatus())) {
            errors.put("status", messages.get("validation.status"));
        }
        return errors;
    }

    @Override
    @Transactional
    public void updateMember(Integer accountId, UpdateMemberAccountRequest request) {
        Map<String, String> errors = validateUpdate(accountId, request);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors.values().iterator().next());
        }

        MemberAccount account = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.account.notFound")));
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

    @Override
    @Transactional
    public boolean deactivateMember(Integer accountId) {
        MemberAccount account = memberAccountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return false;
        }
        applyStatus(account, "Inactive");
        memberAccountRepository.save(account);

        auditLogService.log(
                ActionType.DEACTIVATE_ACCOUNT,
                messages.get("backend.account.audit.deactivatedMember", account.getUsername()));
        return true;
    }

    @Override
    @Transactional
    public void changeMemberStatus(Integer accountId, String status) {
        if (!"Active".equals(status) && !"Inactive".equals(status) && !"Blocked".equals(status)) {
            throw new ValidationException(messages.get("validation.status"));
        }
        MemberAccount account = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.account.notFound")));
        applyStatus(account, status);
        memberAccountRepository.save(account);
    }

    private void applyStatus(MemberAccount account, String status) {
        account.setStatus(status);
        if (account.getMember() != null && account.getMember().getUser() != null) {
            account.getMember().getUser().setStatus(toUserStatus(status));
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private UserStatus toUserStatus(String status) {
        try {
            return UserStatus.valueOf(status);
        } catch (IllegalArgumentException ignored) {
            return UserStatus.Active;
        }
    }
}
