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

    private MemberAccount getMemberAccount(String username) {
        return memberAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("backend.profile.accountNotFound", username)));
    }

    private StaffAccount getStaffAccount(String username) {
        return staffAccountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("backend.profile.accountNotFound", username)));
    }

    @Override
    public User getProfile(String username) {
        return getMemberAccount(username).getMember().getUser();
    }

    @Override
    @Transactional(readOnly = true)
    public User getStaffProfile(String username) {
        return getStaffAccount(username).getStaff().getUser();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveBorrows(String username) {
        MemberAccount account = getMemberAccount(username);
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
        MemberAccount account = getMemberAccount(currentUsername);
        User user = account.getMember().getUser();

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
            account.setUsername(normalizedUsername);
            memberAccountRepository.save(account);
        }
    }

    @Override
    @Transactional
    public void updateStaffProfile(String username, String fullName, String phone, MultipartFile avatarFile) {
        User user = getStaffAccount(username).getStaff().getUser();
        user.setFullName(validateFullName(fullName));
        validateAndSetPhone(user, phone);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            validateAvatar(avatarFile);
            user.setAvatar(fileUploadService.storeFile(avatarFile));
        }

        userRepository.save(user);
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
        if (normalizedPhone.startsWith("+84")) {
            normalizedPhone = "0" + normalizedPhone.substring(3);
        }
        if (userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
            throw new ConflictException("phone", messages.get("backend.account.phoneUsed"));
        }
        user.setPhone(normalizedPhone);
    }

    @Override
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        validateNewPassword(newPassword);
        MemberAccount account = getMemberAccount(username);
        verifyAndReplacePassword(oldPassword, newPassword, account.getPasswordHash(), account::setPasswordHash);
        memberAccountRepository.save(account);
    }

    @Override
    @Transactional
    public void changeStaffPassword(String username, String oldPassword, String newPassword) {
        validateNewPassword(newPassword);
        StaffAccount account = getStaffAccount(username);
        verifyAndReplacePassword(oldPassword, newPassword, account.getPasswordHash(), account::setPasswordHash);
        staffAccountRepository.save(account);
    }

    private void validateNewPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException(messages.get("validation.passwordMin"));
        }
        if (newPassword.length() > 50) {
            throw new ValidationException(messages.get("validation.passwordMax"));
        }
        if (newPassword.contains(" ")) {
            throw new ValidationException(messages.get("validation.passwordNoSpaces"));
        }
    }

    private void verifyAndReplacePassword(String oldPassword,
                                          String newPassword,
                                          String currentPasswordHash,
                                          java.util.function.Consumer<String> passwordSetter) {
        if (!passwordEncoder.matches(oldPassword, currentPasswordHash)) {
            throw new ValidationException(messages.get("backend.profile.oldPasswordIncorrect"));
        }
        if (oldPassword.equals(newPassword)) {
            throw new ValidationException(messages.get("backend.profile.newPasswordSame"));
        }
        passwordSetter.accept(passwordEncoder.encode(newPassword));
    }
}
