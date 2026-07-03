package com.lms.service.impl;

import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.dto.request.UpdateMemberAccountRequest;
import com.lms.dto.response.MemberListViewData;
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
import com.lms.service.LibrarianMemberService;
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

@Service
public class LibrarianMemberServiceImpl implements LibrarianMemberService {

    private final AccountRepository accountRepository;
    private final MemberRepository memberRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public LibrarianMemberServiceImpl(
            AccountRepository accountRepository,
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

    @Override
    @Transactional(readOnly = true)
    public MemberListViewData getMemberList(int page, String keyword) {
        PageRequest pageable = PageRequest.of(page, 10, Sort.by("accountId").ascending());
        String normalizedKeyword = trim(keyword);
        Page<Account> accounts = normalizedKeyword.isEmpty()
                ? accountRepository.findMemberAccounts(pageable)
                : accountRepository.searchMemberAccounts(normalizedKeyword, pageable);

        Map<Integer, Member> memberByUserId = new HashMap<>();
        for (Account account : accounts.getContent()) {
            if (account.getUser() != null && account.getUser().getId() != null) {
                memberRepository.findByUserId(account.getUser().getId())
                        .ifPresent(member -> memberByUserId.put(account.getUser().getId(), member));
            }
        }
        return new MemberListViewData(accounts, memberByUserId, getMembershipTiers());
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
            errors.put("email", "Email đã được sử dụng.");
        }
        if (!phone.isEmpty() && userRepository.existsByPhone(phone)) {
            errors.put("phone", "Số điện thoại đã được sử dụng.");
        }
        if (!username.isEmpty() && accountRepository.existsByUsername(username)) {
            errors.put("username", "Username đã tồn tại.");
        }
        if (request.getTierId() != null && !membershipTierRepository.existsById(request.getTierId())) {
            errors.put("tierId", "Hạng thành viên không hợp lệ.");
        }
        if (request.getStatus() != null
                && !"Active".equals(request.getStatus())
                && !"Inactive".equals(request.getStatus())) {
            errors.put("status", "Trạng thái thành viên không hợp lệ.");
        }
        return errors;
    }

    @Override
    @Transactional
    public void createMember(CreateMemberAccountRequest request) {
        Map<String, String> errors = validateCreate(request);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.values().iterator().next());
        }

        MembershipTier tier = membershipTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new IllegalArgumentException("Hạng thành viên không hợp lệ."));
        Role memberRole = roleRepository.findByNameIgnoreCase("MEMBER")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role MEMBER trong database."));

        User user = new User();
        user.setFullName(trim(request.getFullName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));
        user.setStatus(toUserStatus(request.getStatus()));
        userRepository.save(user);

        Account account = new Account();
        account.setUser(user);
        account.setUsername(trim(request.getUsername()));
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setStatus(request.getStatus());
        account.getRoles().add(memberRole);
        accountRepository.save(account);

        Member member = new Member();
        member.setUser(user);
        member.setTier(tier);
        memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> validateUpdate(Integer accountId, UpdateMemberAccountRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null || account.getUser() == null) {
            errors.put("_global", "Không tìm thấy tài khoản cần cập nhật.");
            return errors;
        }

        String username = trim(request.getUsername());
        String email = trim(request.getEmail());
        String phone = trim(request.getPhone());

        if (!username.isEmpty()
                && accountRepository.existsByUsernameAndAccountIdNot(username, account.getAccountId())) {
            errors.put("username", "Username đã tồn tại.");
        }
        if (!email.isEmpty() && userRepository.existsByEmailAndIdNot(email, account.getUser().getId())) {
            errors.put("email", "Email đã được sử dụng.");
        }
        if (!phone.isEmpty() && userRepository.existsByPhoneAndIdNot(phone, account.getUser().getId())) {
            errors.put("phone", "Số điện thoại đã được sử dụng.");
        }
        if (request.getTierId() != null && !membershipTierRepository.existsById(request.getTierId())) {
            errors.put("tierId", "Hạng thành viên không hợp lệ.");
        }
        if (request.getStatus() != null
                && !"Active".equals(request.getStatus())
                && !"Inactive".equals(request.getStatus())
                && !"Blocked".equals(request.getStatus())) {
            errors.put("status", "Trạng thái tài khoản không hợp lệ.");
        }
        return errors;
    }

    @Override
    @Transactional
    public void updateMember(Integer accountId, UpdateMemberAccountRequest request) {
        Map<String, String> errors = validateUpdate(accountId, request);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.values().iterator().next());
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        MembershipTier tier = membershipTierRepository.findById(request.getTierId())
                .orElseThrow(() -> new IllegalArgumentException("Hạng thành viên không hợp lệ."));
        User user = account.getUser();

        user.setFullName(trim(request.getFullName()));
        user.setEmail(trim(request.getEmail()));
        user.setPhone(trim(request.getPhone()));
        user.setStatus(toUserStatus(request.getStatus()));
        account.setUsername(trim(request.getUsername()));
        account.setStatus(request.getStatus());

        Member member = memberRepository.findByUserId(user.getId()).orElse(new Member());
        member.setUser(user);
        member.setTier(tier);

        userRepository.save(user);
        accountRepository.save(account);
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public boolean deactivateMember(Integer accountId) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return false;
        }
        account.setStatus("Inactive");
        if (account.getUser() != null) {
            account.getUser().setStatus(UserStatus.Inactive);
        }
        accountRepository.save(account);
        return true;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private UserStatus toUserStatus(String status) {
        try {
            return UserStatus.valueOf(status);
        } catch (Exception ignored) {
            return UserStatus.Active;
        }
    }
}
