package com.lms.service.impl;

import com.lms.service.AuthService;
import com.lms.service.EmailService; // Import mới

import com.lms.dto.request.RegisterRequest;
import com.lms.entity.Account;
import com.lms.entity.Member;
import com.lms.entity.PasswordResetToken; // Import mới
import com.lms.entity.User;
import com.lms.entity.Wallet;
import com.lms.repository.AccountRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.PasswordResetTokenRepository; // Import mới
import com.lms.repository.UserRepository;
import com.lms.repository.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Import mới
import java.util.Optional;
import java.util.UUID; // Import mới

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
    private final PasswordResetTokenRepository passwordResetTokenRepository; // Dependency mới
    private final EmailService emailService; // Dependency mới

    public AuthServiceImpl(UserRepository userRepository, AccountRepository accountRepository, MemberRepository memberRepository, WalletRepository walletRepository, PasswordEncoder passwordEncoder,
                           PasswordResetTokenRepository passwordResetTokenRepository, EmailService emailService) { // Constructor cập nhật
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
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
        user.setStatus(com.lms.enums.UserStatus.Active);
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

    // UC-21.2: Yêu cầu đặt lại mật khẩu
    @Override
    @Transactional
    public void requestPasswordReset(String email) throws Exception {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            // Không ném lỗi để tránh tiết lộ email có tồn tại hay không
            // Chỉ log hoặc trả về thành công giả
            System.out.println("Email not found for password reset: " + email);
            return;
        }
        User user = userOptional.get();

        // Xóa các token cũ của người dùng này (nếu có)
        passwordResetTokenRepository.deleteByUser(user);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24); // Token hết hạn sau 24 giờ

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepository.save(resetToken);

        // Gửi email
        // TODO: Thay đổi base URL này cho phù hợp với môi trường triển khai của bạn (ví dụ: domain thật)
        String resetLink = "http://localhost:8080/reset-password?token=" + token; 
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
            passwordResetTokenRepository.delete(resetToken); // Xóa token hết hạn
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
            passwordResetTokenRepository.delete(resetToken); // Xóa token hết hạn
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

        passwordResetTokenRepository.delete(resetToken); // Xóa token sau khi sử dụng thành công
    }
}
