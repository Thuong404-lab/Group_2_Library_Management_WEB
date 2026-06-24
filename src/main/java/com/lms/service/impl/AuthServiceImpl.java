package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.enums.ActionType;
import com.lms.repository.*;
import com.lms.service.AuthService;

import com.lms.dto.request.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * AuthServiceImpl - Xử lý Logic Xác thực
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    private final AccountRepository accountRepository;

    private final MemberRepository memberRepository;

    private final WalletRepository walletRepository;

    private final PasswordEncoder passwordEncoder;

    private final SystemLogRepository systemLogRepository;

    public AuthServiceImpl(UserRepository userRepository, AccountRepository accountRepository, MemberRepository memberRepository, WalletRepository walletRepository, PasswordEncoder passwordEncoder, SystemLogRepository systemLogRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemLogRepository = systemLogRepository;
    }


    // UC-2: Đăng ký thành viên mới
    @Override
    @Transactional
    public void register(RegisterRequest request) throws Exception {
        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new Exception("Tên đăng nhập đã tồn tại!");
        }

        // 1. Tạo User
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(com.lms.enums.UserStatus.Active);
        user = userRepository.save(user);

        // 2. Tạo Account
        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setUser(user);
        account.setStatus("Active");
        accountRepository.save(account);

        // 3. Tạo Member
        Member member = new Member();
        member.setUser(user);
        // member.setTier(...) // Có thể set hạng Basic sau
        memberRepository.save(member);

        // 4. Tạo Wallet
        Wallet wallet = new Wallet();
        wallet.setMember(member);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);
    }

    private void createAndSaveLog(Integer accountId, String actionType, String ipAddres, String userAgent, String description) {
        Account account = accountRepository.findById(accountId).orElse(null);
        if (account != null) {
            SystemLog log = new SystemLog(account, actionType, ipAddres, userAgent, description);
            systemLogRepository.save(log);
        }
    }


    @Override
    public void logLoginAction(Integer accountId, String ipAddress, String userAgent, String sessionId) {
        String description = "Đăng nhập thành công. Session ID: " + sessionId;
        createAndSaveLog(accountId, ActionType.LOGIN.name(), ipAddress, userAgent, description);

    }

    @Override
    public void logLogoutAction(Integer accountId, String ipAddress, String userAgent, String sessionId) {
        String description = "Đăng xuất thành công. Session ID: " + sessionId;
        createAndSaveLog(accountId, ActionType.LOGOUT.name(), ipAddress, userAgent, description);
    }
}
