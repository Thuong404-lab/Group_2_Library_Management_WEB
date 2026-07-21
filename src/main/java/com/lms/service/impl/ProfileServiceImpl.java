package com.lms.service.impl;

import com.lms.entity.User;
import com.lms.entity.MemberAccount;
import com.lms.entity.StaffAccount;
import com.lms.repository.UserRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.service.FileUploadService;
import com.lms.service.LocalizedMessageService;
import com.lms.service.ProfileService;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ConflictException;
import com.lms.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Optional;
import java.util.Set;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final String FULL_NAME_PATTERN = "^[\\p{L}]+(?:\\s+[\\p{L}]+)*$";
    private static final String FULL_NAME_WORD_PATTERN = "^[\\p{L}]{1,15}(?:\\s+[\\p{L}]{1,15}){0,7}$";
    private static final String FULL_NAME_TRIPLE_REPEAT_PATTERN = ".*([\\p{L}])\\1\\1.*";
    private static final String FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN = "^([\\p{L}])\\1+$";
    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final UserRepository userRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final BorrowDetailRepository borrowDetailRepository;
    private final LocalizedMessageService messages;

    public ProfileServiceImpl(UserRepository userRepository,
                              MemberAccountRepository memberAccountRepository,
                              StaffAccountRepository staffAccountRepository,
                              PasswordEncoder passwordEncoder,
                              FileUploadService fileUploadService,
                              BorrowDetailRepository borrowDetailRepository,
                              LocalizedMessageService messages) {
        this.userRepository = userRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
        this.borrowDetailRepository = borrowDetailRepository;
        this.messages = messages;
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
    @Transactional(readOnly = true)
    public long countActiveBorrows(String username) {
        MemberAccount account = memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("backend.profile.accountNotFound", username)));
        return borrowDetailRepository.countActiveBorrowedBooks(account.getMember().getMemberId());
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

        String normalizedUsername = validateUsernameChange(currentUsername, newUsername);

        String normalizedFullName = validateFullName(fullName);
        user.setFullName(normalizedFullName);

        validateAndSetEmail(user, email);
        validateAndSetPhone(user, phone);
        
        if (avatarFile != null && !avatarFile.isEmpty()) {
            validateAvatar(avatarFile);
            String avatarUrl = fileUploadService.storeFile(avatarFile);
            user.setAvatar(avatarUrl);
        }
        
        userRepository.save(user);

        if (!normalizedUsername.equals(currentUsername)) {
            Optional<MemberAccount> memberAccount = memberAccountRepository.findByUsername(currentUsername);
            if (memberAccount.isPresent()) {
                MemberAccount account = memberAccount.get();
                account.setUsername(normalizedUsername);
                memberAccountRepository.save(account);
            } else {
                Optional<StaffAccount> staffAccount = staffAccountRepository.findByUsername(currentUsername);
                if (staffAccount.isPresent()) {
                    StaffAccount account = staffAccount.get();
                    account.setUsername(normalizedUsername);
                    staffAccountRepository.save(account);
                }
            }
        }
    }

    private String validateUsernameChange(String currentUsername, String newUsername) {
        String normalized = newUsername == null ? currentUsername : newUsername.trim();
        if (normalized.isEmpty()) {
            throw new ValidationException("username", messages.get("validation.usernameRequired"));
        }
        if (normalized.length() < 3 || normalized.length() > 100) {
            throw new ValidationException("username", messages.get("validation.usernameLength"));
        }
        if (!normalized.matches("^[a-zA-Z0-9_.]+$")) {
            throw new ValidationException("username", messages.get("validation.usernameFormat"));
        }
        if (!normalized.equals(currentUsername)
                && (memberAccountRepository.findByUsername(normalized).isPresent()
                || staffAccountRepository.findByUsername(normalized).isPresent())) {
            throw new ConflictException("username", messages.get("backend.account.usernameUsed"));
        }
        return normalized;
    }

    private void validateAvatar(MultipartFile avatarFile) {
        if (avatarFile.getSize() > MAX_AVATAR_SIZE) {
            throw new ValidationException("avatar", messages.get("validation.avatarMaxSize"));
        }
        String contentType = avatarFile.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("avatar", messages.get("validation.avatarType"));
        }
    }

    private String validateFullName(String fullNameValue) {
        String fullName = fullNameValue == null ? "" : fullNameValue.trim();
        if (fullName.isEmpty()) {
            throw new ValidationException("fullName", messages.get("validation.fullNameRequired"));
        }
        if (fullName.length() < 2 || fullName.length() > 50) {
            throw new ValidationException("fullName", messages.get("validation.fullNameMax"));
        }
        if (!fullName.matches(FULL_NAME_PATTERN)) {
            throw new ValidationException("fullName", messages.get("validation.fullNameLetters"));
        }
        if (!fullName.matches(FULL_NAME_WORD_PATTERN)) {
            throw new ValidationException("fullName", messages.get("validation.fullNameWords"));
        }
        if (fullName.matches(FULL_NAME_TRIPLE_REPEAT_PATTERN)) {
            throw new ValidationException("fullName", messages.get("validation.fullNameTriple"));
        }
        if (fullName.matches(FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN)) {
            throw new ValidationException("fullName", messages.get("validation.fullNameRepeated"));
        }
        return fullName;
    }

    private void validateAndSetEmail(User user, String email) {
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isEmpty()) {
            throw new ValidationException("email", messages.get("validation.emailRequired"));
        }
        if (normalizedEmail.length() > 255) {
            throw new ValidationException("email", messages.get("validation.emailMax"));
        }
        if (!normalizedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("email", messages.get("validation.email"));
        }
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
            throw new ConflictException("email", messages.get("backend.account.emailUsed"));
        }
        user.setEmail(normalizedEmail);
    }

    private void validateAndSetPhone(User user, String phone) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (normalizedPhone.isEmpty()) {
            throw new ValidationException("phone", messages.get("validation.phoneRequired"));
        }
        if (!normalizedPhone.matches("^(0|\\+84)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-46-9])\\d{7}$")) {
            throw new ValidationException("phone", messages.get("backend.profile.phoneFormat"));
        }
        if (userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
            throw new ConflictException("phone", messages.get("backend.account.phoneUsed"));
        }
        user.setPhone(normalizedPhone);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException(messages.get("validation.passwordMin"));
        }
        if (newPassword.length() > 50) {
            throw new ValidationException(messages.get("validation.passwordMax"));
        }
        if (newPassword.contains(" ")) {
            throw new ValidationException(messages.get("validation.passwordNoSpaces"));
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
