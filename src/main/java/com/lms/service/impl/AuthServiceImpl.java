package com.lms.service.impl;

import com.lms.dto.request.RegisterRequest;
import com.lms.entity.*;
import com.lms.enums.ActionType;
import com.lms.exception.AuthException;
import com.lms.exception.DataProcessingException;
import com.lms.exception.ValidationException;
import com.lms.repository.*;
import com.lms.service.AuthService;
import com.lms.service.EmailService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Password reset flow maintained by Pham Kien Quoc for UC-21.2.
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private static final String FULL_NAME_PATTERN = "^[\\p{L}]+(?:\\s+[\\p{L}]+)*$";
    private static final String FULL_NAME_WORD_PATTERN = "^[\\p{L}]{1,15}(?:\\s+[\\p{L}]{1,15}){0,7}$";
    private static final String FULL_NAME_TRIPLE_REPEAT_PATTERN = ".*([\\p{L}])\\1\\1.*";
    private static final String FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN = "^([\\p{L}])\\1+$";

    private final UserRepository userRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final SystemLogRepository systemLogRepository;
    private final String applicationBaseUrl;
    private final RoleRepository roleRepository;
    private final MembershipTierRepository membershipTierRepository;

    public AuthServiceImpl(UserRepository userRepository,
            MemberAccountRepository memberAccountRepository,
            StaffAccountRepository staffAccountRepository,
            MemberRepository memberRepository,
            WalletRepository walletRepository,
            RoleRepository roleRepository,
            MembershipTierRepository membershipTierRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            SystemLogRepository systemLogRepository,
            @Value("${app.base-url:http://localhost:8080}") String applicationBaseUrl) {
        this.userRepository = userRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.roleRepository = roleRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.systemLogRepository = systemLogRepository;
        this.applicationBaseUrl = applicationBaseUrl.replaceAll("/+$", "");
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) throws AuthException {
        if (request.getUsername() == null || !request.getUsername().matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new AuthException(messages.get("validation.username"));
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new AuthException(messages.get("validation.passwordMin"));
        }

        validateFullName(request.getFullName());

        if (request.getEmail() == null
                || !request.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new AuthException(messages.get("validation.email"));
        }

        if (request.getPhone() == null
                || !request.getPhone().matches("^(0|\\+84)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-46-9])\\d{7}$")) {
            throw new AuthException(messages.get("backend.profile.phoneFormat"));
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AuthException(messages.get("backend.account.phoneUsed"));
        }

        if (memberAccountRepository.findByUsername(request.getUsername()).isPresent()
                || staffAccountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AuthException(messages.get("backend.account.usernameExists"));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(messages.get("backend.account.emailUsed"));
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        createCoreAccount(
                request.getUsername(),
                request.getFullName().trim(),
                encodedPassword,
                request.getEmail(),
                request.getPhone());
    }

    private void validateFullName(String fullNameValue) throws AuthException {
        String fullName = fullNameValue == null ? "" : fullNameValue.trim();
        if (fullName.isEmpty()) {
            throw new AuthException(messages.get("validation.fullNameRequired"));
        }
        if (fullName.length() > 50) {
            throw new AuthException(messages.get("validation.fullNameMax"));
        }
        if (!fullName.matches(FULL_NAME_PATTERN)) {
            throw new AuthException(messages.get("validation.fullNameLetters"));
        }
        if (!fullName.matches(FULL_NAME_WORD_PATTERN)) {
            throw new AuthException(messages.get("validation.fullNameWords"));
        }
        if (fullName.matches(FULL_NAME_TRIPLE_REPEAT_PATTERN)) {
            throw new AuthException(messages.get("validation.fullNameTriple"));
        }
        if (fullName.matches(FULL_NAME_SINGLE_CHARACTER_REPEAT_PATTERN)) {
            throw new AuthException(messages.get("validation.fullNameRepeated"));
        }
    }

    private void createAndSaveLog(Integer userId,
            String actionType,
            String ipAddress,
            String userAgent,
            String description) {
        User user = userRepository.findById(userId).orElse(null);

        if (user != null) {
            SystemLog log = new SystemLog(user, actionType, ipAddress, userAgent, description);
            systemLogRepository.save(log);
        }
    }

    @Override
    public void logLoginAction(Integer userId, String ipAddress, String userAgent) {
        createAndSaveLog(userId, ActionType.LOGIN.name(), ipAddress, userAgent,
                messages.get("backend.auth.audit.login"));
    }

    @Override
    public void logGoogleLoginAction(Integer userId, String ipAddress, String userAgent) {
        createAndSaveLog(userId, ActionType.GOOGLE.name(), ipAddress, userAgent,
                messages.get("backend.auth.audit.google_login"));
    }

    @Override
    public void logLogoutAction(Integer userId, String ipAddress, String userAgent) {
        createAndSaveLog(userId, ActionType.LOGOUT.name(), ipAddress, userAgent,
                messages.get("backend.auth.audit.logout"));
    }

    @Override
    public MemberAccount createCoreAccount(String userName, String fullName, String pass, String email, String phone) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(com.lms.enums.UserStatus.Active);
        user = userRepository.save(user);

        Member member = new Member();
        member.setUser(user);

        membershipTierRepository.findAll(Sort.by("tierId").ascending())
                .stream().findFirst().ifPresent(member::setTier);

        member = memberRepository.save(member);

        MemberAccount account = new MemberAccount();
        account.setUsername(userName);
        account.setPasswordHash(pass);
        account.setMember(member);
        account.setStatus("Active");

        account = memberAccountRepository.save(account);

        Wallet wallet = new Wallet();
        wallet.setMember(member);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);

        return account;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = email == null ? "" : email.trim();
        if (normalizedEmail.isBlank()) {
            throw new ValidationException(messages.get("validation.emailRequired"));
        }
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByUser(user)
                .orElseGet(PasswordResetToken::new);
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(expiryDate);
        passwordResetTokenRepository.save(resetToken);

        String resetLink = applicationBaseUrl + "/reset-password?token=" + token;
        String subject = messages.get("backend.auth.resetEmail.subject");
        String emailContent = messages.get("backend.auth.resetEmail.content", user.getFullName(), resetLink);

        emailService.sendEmail(user.getEmail(), subject, emailContent);

    }

    @Override
    public void validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> resetTokenOptional = passwordResetTokenRepository.findByToken(token);

        if (resetTokenOptional.isEmpty()) {
            throw new ValidationException(messages.get("backend.auth.resetTokenInvalid"));
        }

        PasswordResetToken resetToken = resetTokenOptional.get();

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new ValidationException(messages.get("backend.auth.resetTokenExpired"));
        }
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> resetTokenOptional = passwordResetTokenRepository.findByToken(token);

        if (resetTokenOptional.isEmpty()) {
            throw new ValidationException(messages.get("backend.auth.resetTokenInvalid"));
        }

        PasswordResetToken resetToken = resetTokenOptional.get();

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new ValidationException(messages.get("backend.auth.resetTokenExpired"));
        }

        User user = resetToken.getUser();

        Optional<MemberAccount> memberAccOpt = memberAccountRepository.findByMember_User_Email(user.getEmail());
        if (memberAccOpt.isPresent()) {
            MemberAccount memberAccount = memberAccOpt.get();
            memberAccount.setPasswordHash(passwordEncoder.encode(newPassword));
            memberAccountRepository.save(memberAccount);
        } else {
            Optional<StaffAccount> staffAccOpt = staffAccountRepository.findByStaff_User_Email(user.getEmail());
            if (staffAccOpt.isPresent()) {
                StaffAccount staffAccount = staffAccOpt.get();
                staffAccount.setPasswordHash(passwordEncoder.encode(newPassword));
                staffAccountRepository.save(staffAccount);
            } else {
                throw new DataProcessingException(messages.get("backend.auth.linkedAccountNotFound"));
            }
        }

        passwordResetTokenRepository.delete(resetToken);
    }
}
