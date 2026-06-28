package com.lms.repository;

import com.lms.entity.Account;
import com.lms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)
public interface AccountRepository extends JpaRepository<Account, Integer> {

    // Tìm kiếm tài khoản hệ thống dựa trên Username đăng nhập
    Optional<Account> findByUsername(String username);

    // Tìm kiếm tài khoản dựa trên Email người dùng (Phục vụ Login OAuth2/Google)
    Optional<Account> findByUser_Email(String email);

    Optional<Account> findByUserId(Integer userId);

    Optional<Account> findByUser(User user);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndAccountIdNot(String username, Integer accountId);

    @Query("""
            SELECT DISTINCT a
            FROM Account a
            JOIN a.roles r
            WHERE LOWER(r.name) = LOWER('MEMBER')
            """)
    Page<Account> findMemberAccounts(Pageable pageable);

    @Query("""
            SELECT DISTINCT a
            FROM Account a
            JOIN a.roles r
            WHERE LOWER(r.name) = LOWER('MEMBER')
              AND (
                    LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(a.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(a.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Account> searchMemberAccounts(@Param("keyword") String keyword, Pageable pageable);

}