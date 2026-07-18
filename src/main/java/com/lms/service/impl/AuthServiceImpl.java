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
import org.springframework.web.util.HtmlUtils;

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
        String username = request.getUsername() != null ? request.getUsername().trim() : null;
        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        String fullName = request.getFullName() != null ? request.getFullName().trim() : null;

        if (username == null || !username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new AuthException(messages.get("validation.username"));
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new AuthException(messages.get("validation.passwordMin"));
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthException(messages.get("backend.password.mismatch", "Passwords do not match"));
        }

        validateFullName(fullName);

        if (email == null
                || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new AuthException(messages.get("validation.email"));
        }

        if (phone == null || !phone.matches("^(0|\\+84)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-46-9])\\d{7}$")) {
            throw new AuthException(messages.get("backend.profile.phoneFormat"));
        }


        if (userRepository.existsByPhone(phone)) {
            throw new AuthException(messages.get("backend.account.phoneUsed"));
        }

        if (memberAccountRepository.findByUsername(username).isPresent() || staffAccountRepository.findByUsername(username).isPresent()) {
            throw new AuthException(messages.get("backend.account.usernameExists"));
        }

        if (userRepository.existsByEmail(email)) {
            throw new AuthException(messages.get("backend.account.emailUsed"));
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        createCoreAccount(
                username,
                fullName,
                encodedPassword,
                email,
                phone);
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
        String emailContent = buildPasswordResetEmail(user.getFullName(), resetLink);

        emailService.sendHtmlEmail(user.getEmail(), subject, emailContent);

    }

    private String buildPasswordResetEmail(String fullName, String resetLink) {
        String safeName = HtmlUtils.htmlEscape(fullName == null || fullName.isBlank()
                ? messages.get("backend.auth.resetEmail.defaultName") : fullName);
        String safeResetLink = HtmlUtils.htmlEscape(resetLink);
        return """
                <!doctype html>
                <html><body style="margin:0;padding:0;background:#f5f1eb;font-family:Arial,sans-serif;color:#33271f;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5f1eb;padding:32px 16px;"><tr><td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:600px;background:#fffdf9;border:1px solid #e4d8ca;border-radius:14px;overflow:hidden;">
                    <tr><td style="padding:24px 32px;background:#95602b;color:#fff;text-align:center;font-family:Georgia,serif;font-size:24px;font-weight:bold;">%s</td></tr>
                    <tr><td style="padding:32px;">
                      <p style="margin:0 0 16px;font-size:16px;line-height:1.6;">%s <strong>%s</strong>,</p>
                      <p style="margin:0 0 24px;color:#685b51;font-size:15px;line-height:1.7;">%s</p>
                      <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 24px;"><tr><td style="border-radius:8px;background:#95602b;">
                        <a href="%s" style="display:inline-block;padding:13px 28px;color:#fff;text-decoration:none;font-size:15px;font-weight:bold;">%s</a>
                      </td></tr></table>
                      <div style="padding:14px 16px;background:#faf6f0;border-left:4px solid #c79a69;border-radius:6px;color:#685b51;font-size:13px;line-height:1.6;">%s</div>
                      <p style="margin:22px 0 0;color:#7a6c61;font-size:13px;line-height:1.6;">%s</p>
                    </td></tr>
                    <tr><td style="padding:18px 32px;background:#f2e9df;color:#765b42;text-align:center;font-size:12px;">%s</td></tr>
                  </table>
                </td></tr></table></body></html>
                """.formatted(
                HtmlUtils.htmlEscape(messages.get("backend.auth.resetEmail.brand")),
                HtmlUtils.htmlEscape(messages.get("backend.auth.resetEmail.greeting")), safeName,
                HtmlUtils.htmlEscape(messages.get("backend.auth.resetEmail.instruction")), safeResetLink,
                HtmlUtils.htmlEscape(messages.get("backend.auth.resetEmail.button")),
                HtmlUtils.htmlEscape(messages.get("backend.auth.resetEmail.expiry")),
                HtmlUtils.htmlEscape(messages.get("backend.auth.resetEmail.ignore")),
                HtmlUtils.htmlEscape(messages.get("backend.auth.resetEmail.signature")));
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
