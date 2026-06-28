package com.lms.repository;

import com.lms.entity.PasswordResetToken;
import com.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PasswordResetTokenRepository - Repository cho PasswordResetToken
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}
