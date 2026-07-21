package com.lms.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MemberAccountDeletionRepository {

    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public MemberAccountDeletionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasBusinessHistory(Integer memberId) {
        Integer result = jdbcTemplate.queryForObject("""
                SELECT CASE WHEN
                    EXISTS (SELECT 1 FROM dbo.Borrows WHERE member_id = ?)
                    OR EXISTS (SELECT 1 FROM dbo.Reservations WHERE member_id = ?)
                    OR EXISTS (SELECT 1 FROM dbo.Feedbacks WHERE member_id = ?)
                    OR EXISTS (SELECT 1 FROM dbo.BookAcquisitionRequests WHERE member_id = ?)
                    OR EXISTS (SELECT 1 FROM dbo.Transactions t
                               JOIN dbo.Wallets w ON w.wallet_id = t.wallet_id
                               WHERE w.member_id = ?)
                    OR EXISTS (SELECT 1 FROM dbo.PayOSPayments WHERE member_id = ?)
                THEN 1 ELSE 0 END
                """, Integer.class, memberId, memberId, memberId, memberId, memberId, memberId);
        return Integer.valueOf(1).equals(result);
    }

    public void deleteAggregate(Integer accountId, Integer memberId, Integer userId) {
        entityManager.flush();
        entityManager.clear();

        // Preserve audit records while removing their reference to the deleted user.
        jdbcTemplate.update("UPDATE dbo.SystemLogs SET user_id = NULL WHERE user_id = ?", userId);
        jdbcTemplate.update("UPDATE dbo.PayOSPaymentAuditLogs SET actor_user_id = NULL WHERE actor_user_id = ?", userId);

        jdbcTemplate.update("DELETE FROM dbo.Member_Accounts WHERE id = ?", accountId);
        jdbcTemplate.update("DELETE FROM dbo.Wallets WHERE member_id = ?", memberId);
        jdbcTemplate.update("DELETE FROM dbo.Members WHERE member_id = ?", memberId);
        jdbcTemplate.update("DELETE FROM dbo.Users WHERE user_id = ?", userId);
    }
}
