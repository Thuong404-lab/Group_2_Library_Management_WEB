package com.lms.service.impl;

import com.lms.dto.request.RegisterRequest;
import com.lms.entity.Account;
import com.lms.entity.Member;
import com.lms.entity.PasswordResetToken;
import com.lms.entity.SystemLog;
import com.lms.entity.User;
import com.lms.entity.Wallet;
import com.lms.enums.ActionType;
import com.lms.exception.AuthException;
import com.lms.repository.AccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.PasswordResetTokenRepository;
import com.lms.repository.SystemLogRepository;
import com.lms.repository.UserRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.AuthService;
import com.lms.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final SystemLogRepository systemLogRepository;
    private final String applicationBaseUrl;

    public AuthServiceImpl(UserRepository userRepository,
            AccountRepository accountRepository,
            MemberRepository memberRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            SystemLogRepository systemLogRepository,
            @Value("${app.base-url:http://localhost:8080}") String applicationBaseUrl) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.systemLogRepository = systemLogRepository;
        this.applicationBaseUrl = applicationBaseUrl.replaceAll("/+$", "");
    }

    // UC-2: Đăng ký thành viên mới
    @Override
    @Transactional
    public void register(RegisterRequest request) throws AuthException {
        if (request.getUsername() == null || !request.getUsername().matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new AuthException("Tên đăng nhập không hợp lệ (3-20 ký tự, không chứa khoảng trắng và ký tự đặc biệt)!");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new AuthException("Mật khẩu phải có ít nhất 6 ký tự!");
        }

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new AuthException("Họ và tên không được để trống!");
        }

        if (request.getEmail() == null || !request.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new AuthException("Email không hợp lệ (phải đúng định dạng, ví dụ: ten@gmail.com)!");
        }

        if (request.getPhone() == null || !request.getPhone().matches("^(0|\\+84)[0-9]{9}$")) {
            throw new AuthException("Số điện thoại không hợp lệ (phải gồm 10 số và bắt đầu bằng 0 hoặc +84)!");
        }

        if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AuthException("Tên đăng nhập đã tồn tại!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email đã được sử dụng!");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        createCoreAccount(
                request.getUsername(),
                request.getFullName(),
                encodedPassword,
                request.getEmail(),
                request.getPhone());
    }

    private void createAndSaveLog(Integer accountId,
            String actionType,
            String ipAddress,
            String userAgent,
            String description) {
        Account account = accountRepository.findById(accountId).orElse(null);

        if (account != null) {
            SystemLog log = new SystemLog(account, actionType, ipAddress, userAgent, description);
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

    // UC-21.2: Yêu cầu đặt lại mật khẩu
    @Override
    @Transactional
    public void requestPasswordReset(String email) throws Exception {
        Optional<User> userOptional = userRepository.findByEmail(email.trim());

        if (userOptional.isEmpty()) {
            System.out.println("Email not found for password reset: " + email);
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
        String subject = "Yêu cầu đặt lại mật khẩu của bạn";

        String emailContent = "Xin chào " + user.getFullName() + ",\n\n"
                + "Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng nhấp vào liên kết sau để đặt lại mật khẩu của bạn:\n"
                + resetLink + "\n\n"
                + "Liên kết này sẽ hết hạn sau 24 giờ.\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ hỗ trợ Thư viện";

        emailService.sendEmail(user.getEmail(), subject, emailContent);

        System.out.println("Password reset email sent to: " + user.getEmail());
    }

    // UC-21.2: Xác thực token đặt lại mật khẩu
    @Override
    public void validatePasswordResetToken(String token) throws Exception {
        Optional<PasswordResetToken> resetTokenOptional = passwordResetTokenRepository.findByToken(token);

        if (resetTokenOptional.isEmpty()) {
            throw new Exception("Token đặt lại mật khẩu không hợp lệ.");
        }

        PasswordResetToken resetToken = resetTokenOptional.get();

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new Exception("Token đặt lại mật khẩu đã hết hạn.");
        }
    }

    // UC-21.2: Đặt lại mật khẩu
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) throws Exception {
        Optional<PasswordResetToken> resetTokenOptional = passwordResetTokenRepository.findByToken(token);

        if (resetTokenOptional.isEmpty()) {
            throw new Exception("Token đặt lại mật khẩu không hợp lệ.");
        }

        PasswordResetToken resetToken = resetTokenOptional.get();

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new Exception("Token đặt lại mật khẩu đã hết hạn.");
        }

        User user = resetToken.getUser();

        Optional<Account> accountOptional = accountRepository.findByUser(user);

        if (accountOptional.isEmpty()) {
            throw new Exception("Không tìm thấy tài khoản liên kết với người dùng.");
        }

        Account account = accountOptional.get();
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);

        passwordResetTokenRepository.delete(resetToken);
    }
}
