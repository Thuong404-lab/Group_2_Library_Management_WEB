package com.lms.service.impl;

import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.dto.request.UpdateMemberAccountRequest;
import com.lms.dto.response.MemberListViewData;
import com.lms.entity.MemberAccount;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.entity.Wallet;
import com.lms.enums.ActionType;
import com.lms.enums.UserStatus;
import com.lms.exception.ConflictException;
import com.lms.exception.DataProcessingException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MemberAccountDeletionRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.UserRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.AuditLogService;
import com.lms.service.LibrarianMemberService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Member account list, update, deactivate, and status changes maintained by
 * Pham Kien Quoc for librarian account-management use cases.
 */
@Service
public class LibrarianMemberServiceImpl implements LibrarianMemberService {

    private static final int PAGE_SIZE = 10;
    private static final String MEMBER_ROLE = "ROLE_MEMBER";

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final MemberAccountRepository memberAccountRepository;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final WalletRepository walletRepository;
    private final MemberAccountDeletionRepository memberAccountDeletionRepository;

    public LibrarianMemberServiceImpl(
            MemberAccountRepository memberAccountRepository,
            MemberRepository memberRepository,
            MembershipTierRepository membershipTierRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            WalletRepository walletRepository,
            MemberAccountDeletionRepository memberAccountDeletionRepository) {
        this.memberAccountRepository = memberAccountRepository;
        this.memberRepository = memberRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.walletRepository = walletRepository;
        this.memberAccountDeletionRepository = memberAccountDeletionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberListViewData getMemberList(int page, String keyword, String status, String tier) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("id").ascending());
        String normalizedKeyword = trim(keyword);
        String normalizedStatus = trim(status);
        String normalizedTier = trim(tier);
        Page<MemberAccount> accounts = memberAccountRepository.searchMemberAccountsWithFilters(
                normalizedKeyword, normalizedStatus, normalizedTier, pageable);

        Map<Integer, Member> memberByUserId = new HashMap<>();
        for (MemberAccount account : accounts.getContent()) {
            Member member = account.getMember();
            if (member != null && member.getUser() != null && member.getUser().getId() != null) {
                memberByUserId.put(member.getUser().getId(), member);
            }
        }
        return new MemberListViewData(accounts, memberByUserId, getMembershipTiers(), getMemberSummaryCounts());
    }

    private Map<String, Long> getMemberSummaryCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", memberAccountRepository.count());
        counts.put("active", memberAccountRepository.countByStatusIgnoreCase(UserStatus.Active.name()));
        counts.put("inactive", memberAccountRepository.countByStatusIgnoreCase(UserStatus.Inactive.name()));
        counts.put("blocked", memberAccountRepository.countByStatusIgnoreCase(UserStatus.Blocked.name()));
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipTier> getMembershipTiers() {
        return membershipTierRepository.findAll(Sort.by("condition").ascending().and(Sort.by("tierId").ascending()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> validateCreate(CreateMemberAccountRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());
        String username = trim(request.getUsername());

        if (!email.isEmpty() && userRepository.existsByEmailIgnoreCase(email)) {
            errors.put("email", messages.get("backend.account.emailUsed"));
        }
        if (!phone.isEmpty() && userRepository.existsByPhone(phone)) {
            errors.put("phone", messages.get("backend.account.phoneUsed"));
        }
        if (!username.isEmpty() && memberAccountRepository.existsByUsernameIgnoreCase(username)) {
            errors.put("username", messages.get("backend.account.usernameExists"));
        }
        if (request.getPassword() != null
                && request.getConfirmPassword() != null
                && !request.getPassword().equals(request.getConfirmPassword())) {
            errors.put("confirmPassword", messages.get("validation.passwordMismatch"));
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

        MembershipTier tier = membershipTierRepository.findFirstByOrderByConditionAscTierIdAsc()
                .orElseThrow(() -> new DataProcessingException(messages.get("backend.account.defaultTierNotFound")));
        Role memberRole = roleRepository.findByNameIgnoreCase(MEMBER_ROLE)
                .orElseThrow(() -> new DataProcessingException(
                        messages.get("backend.account.roleNotFound", MEMBER_ROLE)));

        String status = UserStatus.Active.name();

        MemberAccount account;
        try {
            User user = new User();
            user.setFullName(trim(request.getFullName()));
            user.setEmail(trim(request.getEmail()));
            user.setPhone(trim(request.getPhone()));
            user.setStatus(UserStatus.Active);
            userRepository.saveAndFlush(user);

            Member member = new Member();
            member.setUser(user);
            member.setTier(tier);
            memberRepository.save(member);

            Wallet wallet = new Wallet();
            wallet.setMember(member);
            wallet.setBalance(BigDecimal.ZERO);
            walletRepository.save(wallet);

            account = new MemberAccount();
            account.setMember(member);
            account.setUsername(trim(request.getUsername()));
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            account.setStatus(status);
            account.getRoles().add(memberRole);
            memberAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(messages.get("backend.account.concurrentDuplicate"), exception);
        }

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
                && memberAccountRepository.existsByUsernameIgnoreCaseAndIdNot(username, account.getId())) {
            errors.put("username", messages.get("backend.account.usernameExists"));
        }
        if (!email.isEmpty() && userRepository.existsByEmailIgnoreCaseAndIdNot(email, account.getMember().getUser().getId())) {
            errors.put("email", messages.get("backend.account.emailUsed"));
        }
        if (!phone.isEmpty() && userRepository.existsByPhoneAndIdNot(phone, account.getMember().getUser().getId())) {
            errors.put("phone", messages.get("backend.account.phoneUsed"));
        }
        if (!isValidStatus(request.getStatus())) {
            errors.put("status", messages.get("validation.status"));
        }
        User user = account.getMember().getUser();
        if (!Objects.equals(account.getVersion(), request.getAccountVersion())
                || !Objects.equals(user.getVersion(), request.getUserVersion())) {
            errors.put("_global", messages.get("validation.concurrentUpdate"));
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
        String previousStatus = account.getStatus();

        user.setFullName(trim(request.getFullName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));
        account.setUsername(trim(request.getUsername()));
        applyStatus(account, requireStatus(request.getStatus()));

        userRepository.save(user);
        memberAccountRepository.save(account);

        auditLogService.log(
                ActionType.UPDATE_ACCOUNT,
                messages.get("backend.account.audit.updatedMember", account.getUsername()));
        if (!Objects.equals(previousStatus, account.getStatus())) {
            auditLogService.log(ActionType.UPDATE_ACCOUNT,
                    messages.get("backend.account.audit.statusChanged", account.getUsername(), previousStatus,
                            account.getStatus()));
        }
    }

    @Override
    @Transactional
    public void deleteMember(Integer accountId) {
        MemberAccount account = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.account.notFound")));
        Member member = account.getMember();
        User user = member == null ? null : member.getUser();
        if (member == null || user == null) {
            throw new DataProcessingException(messages.get("backend.account.incompleteMemberData"));
        }
        if (memberAccountDeletionRepository.hasActiveBusiness(member.getMemberId())) {
            throw new ConflictException(messages.get("backend.member.deleteHasHistory"));
        }

        String username = account.getUsername();
        try {
            memberAccountDeletionRepository.deleteAggregate(
                    account.getId(), member.getMemberId(), user.getId());
        } catch (DataAccessException exception) {
            throw new ConflictException(messages.get("backend.member.deleteConflict"), exception);
        }
        auditLogService.log(ActionType.DELETE_ACCOUNT,
                messages.get("backend.account.audit.deletedMember", username));
    }

    @Override
    @Transactional
    public void changeMemberStatus(Integer accountId, String status) {
        UserStatus requestedStatus = requireStatus(status);
        MemberAccount account = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.account.notFound")));
        String previousStatus = account.getStatus();
        applyStatus(account, requestedStatus);
        memberAccountRepository.save(account);
        auditLogService.log(ActionType.UPDATE_ACCOUNT,
                messages.get("backend.account.audit.statusChanged", account.getUsername(), previousStatus,
                        requestedStatus.name()));
    }

    private void applyStatus(MemberAccount account, UserStatus status) {
        account.setStatus(status.name());
        if (account.getMember() != null && account.getMember().getUser() != null) {
            account.getMember().getUser().setStatus(status);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isValidStatus(String status) {
        try {
            UserStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return false;
        }
    }

    @Override
    public boolean checkMemberDeletability(Integer accountId) {
        MemberAccount account = memberAccountRepository.findById(accountId).orElse(null);
        if (account == null || account.getMember() == null) {
            return false;
        }
        return !memberAccountDeletionRepository.hasActiveBusiness(account.getMember().getMemberId());
    }

    private UserStatus requireStatus(String status) {
        if (!isValidStatus(status)) {
            throw new ValidationException(messages.get("validation.status"));
        }
        return UserStatus.valueOf(status);
    }
}
