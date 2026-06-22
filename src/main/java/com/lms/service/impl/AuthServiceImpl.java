package com.lms.service.impl;

import com.lms.service.AuthService;

import com.lms.dto.request.RegisterRequest;
import com.lms.entity.Account;
import com.lms.entity.Member;
import com.lms.entity.User;
import com.lms.entity.Wallet;
import com.lms.repository.AccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.UserRepository;
import com.lms.repository.WalletRepository;
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

    public AuthServiceImpl(UserRepository userRepository, AccountRepository accountRepository, MemberRepository memberRepository, WalletRepository walletRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
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
        user.setStatus(com.lms.enums.UserStatus.ACTIVE);
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

    // UC-9.1: Ghi log đăng nhập
    @Override
    public void logLoginAction(Integer accountId, String ipAddress, String userAgent, String sessionId) {
        // TODO: Implement - Insert vào bảng SystemLogs
    }

    // UC-9.1: Ghi log đăng xuất
    @Override
    public void logLogoutAction(Integer accountId, String ipAddress, String userAgent, String sessionId) {
        // TODO: Implement - Insert vào bảng SystemLogs
    }
}
