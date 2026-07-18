package com.lms.service.impl;

import com.lms.entity.User;
import com.lms.entity.MemberAccount;
import com.lms.entity.StaffAccount;
import com.lms.repository.UserRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.service.FileUploadService;
import com.lms.service.LocalizedMessageService;
import com.lms.service.ProfileService;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private static final String FULL_NAME_PATTERN = "^[\\p{L}]+(?:\\s+[\\p{L}]+)*$";
    private static final String FULL_NAME_WORD_PATTERN = "^[\\p{L}]{1,15}(?:\\s+[\\p{L}]{1,15}){0,7}$";
    private static final String FULL_NAME_TRIPLE_REPEAT_PATTERN = ".*([\\p{L}])\\1\\1.*";
    private static final String FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN = "^([\\p{L}])\\1+$";

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
        throw new ResourceNotFoundException(messages.get("backend.profile.accountNotFound", username));
    }

    @Override
    public User getProfile(String username) {
        return getUserByUsername(username);
    }

    @Override
    @Transactional
    public void updateProfile(String username, String fullName, String email, String phone, MultipartFile avatarFile) {
        updateProfile(username, username, fullName, email, phone, avatarFile);
    }

    @Override
    @Transactional
    public void updateProfile(String currentUsername, String newUsername, String fullName, String email, String phone, MultipartFile avatarFile) {
        User user = getUserByUsername(currentUsername);

        String normalizedFullName = validateFullName(fullName);
        user.setFullName(normalizedFullName);

        validateAndSetEmail(user, email);
        validateAndSetPhone(user, phone);
        
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = fileUploadService.storeFile(avatarFile);
            user.setAvatar(avatarUrl);
        }
        
        userRepository.save(user);

        if (newUsername != null && !newUsername.trim().isEmpty() && !newUsername.equals(currentUsername)) {
            String trimmedNewUsername = newUsername.trim();
            if (trimmedNewUsername.length() < 3 || trimmedNewUsername.length() > 100) {
                throw new ValidationException(messages.get("validation.usernameLength"));
            }
            if (!trimmedNewUsername.matches("^[a-zA-Z0-9_.]+$")) {
                throw new ValidationException(messages.get("validation.usernameFormat"));
            }
            if (memberAccountRepository.findByUsername(trimmedNewUsername).isPresent() || staffAccountRepository.findByUsername(trimmedNewUsername).isPresent()) {
                throw new ConflictException(messages.get("backend.account.usernameUsed"));
            }
            
            Optional<MemberAccount> memberAccount = memberAccountRepository.findByUsername(currentUsername);
            if (memberAccount.isPresent()) {
                MemberAccount account = memberAccount.get();
                account.setUsername(trimmedNewUsername);
                memberAccountRepository.save(account);
            } else {
                Optional<StaffAccount> staffAccount = staffAccountRepository.findByUsername(currentUsername);
                if (staffAccount.isPresent()) {
                    StaffAccount account = staffAccount.get();
                    account.setUsername(trimmedNewUsername);
                    staffAccountRepository.save(account);
                }
            }
        }
    }

    private String validateFullName(String fullNameValue) {
        String fullName = fullNameValue == null ? "" : fullNameValue.trim();
        if (fullName.isEmpty()) {
            throw new ValidationException(messages.get("validation.fullNameRequired"));
        }
        if (fullName.length() > 50) {
            throw new ValidationException(messages.get("validation.fullNameMax"));
        }
        if (!fullName.matches(FULL_NAME_PATTERN)) {
            throw new ValidationException(messages.get("validation.fullNameLetters"));
        }
        if (!fullName.matches(FULL_NAME_WORD_PATTERN)) {
            throw new ValidationException(messages.get("validation.fullNameWords"));
        }
        if (fullName.matches(FULL_NAME_TRIPLE_REPEAT_PATTERN)) {
            throw new ValidationException(messages.get("validation.fullNameTriple"));
        }
        if (fullName.matches(FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN)) {
            throw new ValidationException(messages.get("validation.fullNameRepeated"));
        }
        return fullName;
    }

    private void validateAndSetEmail(User user, String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException(messages.get("validation.emailRequired"));
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException(messages.get("validation.email"));
        }
        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new ConflictException(messages.get("backend.account.emailUsed"));
        }
        user.setEmail(email);
    }

    private void validateAndSetPhone(User user, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException(messages.get("validation.phoneRequired"));
        }
        if (!phone.matches("^(0|\\+84)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-46-9])\\d{7}$")) {
            throw new ValidationException(messages.get("backend.profile.phoneFormat"));
        }
        if (userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw new ConflictException(messages.get("backend.account.phoneUsed"));
        }
        user.setPhone(phone);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException(messages.get("validation.passwordMin"));
        }
        
        Optional<MemberAccount> memberAccount = memberAccountRepository.findByUsername(username);
        if (memberAccount.isPresent()) {
            MemberAccount account = memberAccount.get();
            if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
                throw new ValidationException(messages.get("backend.profile.oldPasswordIncorrect"));
            }
            if (oldPassword.equals(newPassword)) {
                throw new ValidationException(messages.get("backend.profile.newPasswordSame"));
            }
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            memberAccountRepository.save(account);
            return;
        }

        Optional<StaffAccount> staffAccount = staffAccountRepository.findByUsername(username);
        if (staffAccount.isPresent()) {
            StaffAccount account = staffAccount.get();
            if (!passwordEncoder.matches(oldPassword, account.getPasswordHash())) {
                throw new ValidationException(messages.get("backend.profile.oldPasswordIncorrect"));
            }
            if (oldPassword.equals(newPassword)) {
                throw new ValidationException(messages.get("backend.profile.newPasswordSame"));
            }
            account.setPasswordHash(passwordEncoder.encode(newPassword));
            staffAccountRepository.save(account);
            return;
        }

        throw new ResourceNotFoundException(messages.get("backend.account.notFound"));
    }
}
