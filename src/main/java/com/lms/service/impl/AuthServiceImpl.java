package com.lms.service.impl;

import com.lms.entity.*;
import com.lms.enums.ActionType;
import com.lms.exception.AuthException;
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

    public AuthServiceImpl(UserRepository userRepository, AccountRepository accountRepository,
            MemberRepository memberRepository, WalletRepository walletRepository, PasswordEncoder passwordEncoder,
            SystemLogRepository systemLogRepository) {
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
    public void register(RegisterRequest request) throws AuthException {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new AuthException("Tên đăng nhập không được để trống!");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new AuthException("Mật khẩu phải có ít nhất 6 ký tự!");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new AuthException("Họ và tên không được để trống!");
        }
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            throw new AuthException("Email không hợp lệ (phải chứa ký tự @)!");
        }
        if (request.getPhone() == null || request.getPhone().length() < 10) {
            throw new AuthException("Số điện thoại phải có ít nhất 10 số!");
        }
        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AuthException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email đã được sử dụng!");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        createCoreAccount(request.getUsername(), request.getFullName(), encodedPassword, request.getEmail(), request.getPhone());

    }

    private void createAndSaveLog(Integer accountId, String actionType, String ipAddres, String userAgent,
            String description) {
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

    @Override
    public Account createCoreAccount(String userName, String fullName, String pass, String email, String phone) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(com.lms.enums.UserStatus.Active);
        user = userRepository.save(user);

        Account account = new Account();
        account.setUsername(userName);
        account.setPasswordHash(pass);
        account.setUser(user);
        account.setStatus("Active");
        accountRepository.save(account);

        Member member = new Member();
        member.setUser(user);
        memberRepository.save(member);

        Wallet wallet = new Wallet();
        wallet.setMember(member);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);
        return account;
    }
}
