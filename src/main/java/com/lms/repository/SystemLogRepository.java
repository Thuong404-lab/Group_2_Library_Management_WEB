package com.lms.repository;

import com.lms.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

public interface SystemLogRepository extends JpaRepository<SystemLog, Integer> {

    List<SystemLog> findByAccount_AccountId(Integer accountId);

    @EntityGraph(attributePaths = {"account", "account.user"})
    Page<SystemLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"account", "account.user"})
    @Query("""
            SELECT log
            FROM SystemLog log
            LEFT JOIN log.account account
            LEFT JOIN account.user user
            WHERE (:action IS NULL OR :action = '' OR LOWER(log.actionType) LIKE LOWER(CONCAT('%', :action, '%')))
              AND (
                    :keyword IS NULL OR :keyword = ''
                    OR LOWER(log.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(log.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY log.createdAt DESC
            """)
    Page<SystemLog> searchLogs(@Param("action") String action,
                               @Param("keyword") String keyword,
                               Pageable pageable);
}