package com.lms.service.impl;

import com.lms.entity.User;
import com.lms.entity.Account;
import com.lms.repository.UserRepository;
import com.lms.repository.AccountRepository;
import com.lms.service.ProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProfileServiceImpl - Xử lý nghiệp vụ thông tin cá nhân
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileServiceImpl(UserRepository userRepository,
                              AccountRepository accountRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Lấy thông tin cá nhân (User) dựa theo Username của tài khoản đang kết nối
    @Override
    public User getProfile(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found for: " + username));

        if (account.getUser() == null) {
            throw new RuntimeException("No profile user linked to this account: " + username);
        }

        return account.getUser();
    }

    @Override
    @Transactional
    public void updateProfile(String username, String fullName, String email, String phone) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        User user = account.getUser();
        if (user == null) {
            throw new RuntimeException("User profile not found");
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect old password");
        }

        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password cannot be identical to the old one");
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }
}