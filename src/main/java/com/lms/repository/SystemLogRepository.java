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

    List<SystemLog> findByUser_Id(Integer userId);

    @EntityGraph(attributePaths = { "user" })
    Page<SystemLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = { "user" })
    @Query("""
            SELECT log
            FROM SystemLog log
            LEFT JOIN log.user user
            WHERE :keyword IS NULL OR :keyword = ''
               OR LOWER(CAST(log.createdAt AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(FUNCTION('FORMAT', log.createdAt, 'dd/MM/yyyy HH:mm:ss')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(FUNCTION('FORMAT', log.createdAt, 'd/M/yyyy HH:mm:ss')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(FUNCTION('FORMAT', log.createdAt, 'd/M/yyyy')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(FUNCTION('FORMAT', log.createdAt, 'd/M')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(log.actionType) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(log.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(log.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR EXISTS (
                    SELECT memberAccount.id
                    FROM MemberAccount memberAccount
                    WHERE memberAccount.member.user = user
                      AND LOWER(memberAccount.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               )
               OR EXISTS (
                    SELECT staffAccount.id
                    FROM StaffAccount staffAccount
                    WHERE staffAccount.staff.user = user
                      AND LOWER(staffAccount.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               )
            ORDER BY log.createdAt DESC
            """)
    Page<SystemLog> searchLogs(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = { "user" })
    @Query("""
            SELECT log
            FROM SystemLog log
            LEFT JOIN log.user user
            WHERE (
                (:section = 'auth' AND log.actionType IN ('LOGIN', 'LOGOUT', 'GOOGLE'))
                OR (:section = 'circulation' AND log.actionType IN ('REQUEST_BORROW', 'REQUEST_RETURN', 'RESERVE_BOOK'))
                OR (:section = 'operations' AND log.actionType NOT IN
                    ('LOGIN', 'LOGOUT', 'GOOGLE', 'REQUEST_BORROW', 'REQUEST_RETURN', 'RESERVE_BOOK'))
            )
            AND (:actionType = '' OR log.actionType = :actionType)
            AND (:keyword = ''
                OR LOWER(CAST(log.createdAt AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(FUNCTION('FORMAT', log.createdAt, 'dd/MM/yyyy HH:mm:ss')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(FUNCTION('FORMAT', log.createdAt, 'd/M/yyyy')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(log.actionType) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(log.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(log.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR EXISTS (
                    SELECT memberAccount.id FROM MemberAccount memberAccount
                    WHERE memberAccount.member.user = user
                    AND LOWER(memberAccount.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                OR EXISTS (
                    SELECT staffAccount.id FROM StaffAccount staffAccount
                    WHERE staffAccount.staff.user = user
                    AND LOWER(staffAccount.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
            )
            ORDER BY log.createdAt DESC
            """)
    Page<SystemLog> searchLogsBySection(@Param("section") String section,
            @Param("keyword") String keyword,
            @Param("actionType") String actionType,
            Pageable pageable);
}
