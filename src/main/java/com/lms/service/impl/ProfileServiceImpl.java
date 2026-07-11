package com.lms.service.impl;

import com.lms.entity.User;
import com.lms.entity.MemberAccount;
import com.lms.entity.StaffAccount;
import com.lms.repository.UserRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.FileUploadService;
import com.lms.service.ProfileService;
import com.lms.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;

    public ProfileServiceImpl(UserRepository userRepository,
                              MemberAccountRepository memberAccountRepository,
                              StaffAccountRepository staffAccountRepository,
                              PasswordEncoder passwordEncoder,
                              FileUploadService fileUploadService) {
        this.userRepository = userRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
    }

    private User getUserByUsername(String username) {
        Optional<MemberAccount> memberAccount = memberAccountRepository.findByUsername(username);
        if (memberAccount.isPresent()) {
            return memberAccount.get().getMember().getUser();
        }
        Optional<StaffAccount> staffAccount = staffAccountRepository.findByUsername(username);
        if (staffAccount.isPresent()) {
            return staffAccount.get().getStaff().getUser();
        }
        throw new ResourceNotFoundException("Account not found for: " + username);
    }

    @Override
    public User getProfile(String username) {
        return getUserByUsername(username);
    }

    @Override
    @Transactional
    public void updateProfile(String username, String fullName, String email, String phone, MultipartFile avatarFile) {
        User user = getUserByUsername(username);

        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống!");
        }
        user.setFullName(fullName);

        validateAndSetEmail(user, email);
        validateAndSetPhone(user, phone);
        
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = fileUploadService.storeFile(avatarFile);
            user.setAvatar(avatarUrl);
        }
        
        userRepository.save(user);
    }

    private void validateAndSetEmail(User user, String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống!");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Email không hợp lệ (phải đúng định dạng, ví dụ: ten@gmail.com)!");
        }
        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi người dùng khác!");
        }
        user.setEmail(email);
    }

    private void validateAndSetPhone(User user, String phone) {
        if (phone != null && !phone.trim().isEmpty()) {
            if (!phone.matches("^(0|\\+84)\\d{9}$")) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ (phải gồm 10 số và bắt đầu bằng 0 hoặc +84)!");
            }
            if (userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng bởi người dùng khác!");
            }
            user.setPhone(phone);
        } else if (phone != null) {
            user.setPhone("");
        }
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự!");
        }
        
        Optional<MemberAccount> memberAccount = memberAccountRepository.findByUsername(username);
        if (memberAccount.isPresent()) {
            MemberAccount account = memberAccount.get();
            if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
                throw new IllegalArgumentException("Mật khẩu cũ không chính xác!");
            }
            if (oldPassword.equals(newPassword)) {
                throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ!");
            }
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            memberAccountRepository.save(account);
            return;
        }

        Optional<StaffAccount> staffAccount = staffAccountRepository.findByUsername(username);
        if (staffAccount.isPresent()) {
            StaffAccount account = staffAccount.get();
            if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
                throw new IllegalArgumentException("Mật khẩu cũ không chính xác!");
            }
            if (oldPassword.equals(newPassword)) {
                throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ!");
            }
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            staffAccountRepository.save(account);
            return;
        }

        throw new ResourceNotFoundException("Không tìm thấy tài khoản!");
    }
}