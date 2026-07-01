package com.lms.service.impl;

import com.lms.entity.User;
import com.lms.entity.Account;
import com.lms.repository.UserRepository;
import com.lms.repository.AccountRepository;
import com.lms.service.FileUploadService;
import com.lms.service.ProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * ProfileServiceImpl - Xử lý nghiệp vụ thông tin cá nhân
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;

    public ProfileServiceImpl(UserRepository userRepository,
                              AccountRepository accountRepository,
                              PasswordEncoder passwordEncoder,
                              FileUploadService fileUploadService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
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
    public void updateProfile(String username, String fullName, String email, String phone, MultipartFile avatarFile) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        User user = account.getUser();
        if (user == null) {
            throw new RuntimeException("User profile not found");
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }

        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Email không hợp lệ (phải đúng định dạng, ví dụ: ten@gmail.com)!");
        }

        if (phone != null && !phone.isEmpty() && !phone.matches("^(0|\\+84)[0-9]{9}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (phải gồm 10 số và bắt đầu bằng 0 hoặc +84)!");
        }

        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi người dùng khác!");
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = fileUploadService.storeFile(avatarFile);
            user.setAvatar(avatarUrl);
        }
        
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