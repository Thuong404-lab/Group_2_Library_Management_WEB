package com.lms.repository;

import com.lms.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

public interface AccountRepository extends JpaRepository<Account, Integer> {

    Optional<Account> findByUsername(String username);

    @Query("""
            SELECT DISTINCT a
            FROM Account a
            LEFT JOIN a.roles r
            WHERE LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Account> searchAccounts(@Param("keyword") String keyword, Pageable pageable);
}
