package com.lms.repository;

import com.lms.entity.Staff;
import com.lms.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

public interface StaffRepository extends JpaRepository<Staff, Integer> {

        Optional<Staff> findByUserId(Integer userId);

        Page<Staff> findByStaffTypeIgnoreCase(String staffType, Pageable pageable);

        long countByStaffTypeIgnoreCase(String staffType);

        @Query("SELECT COUNT(s) FROM Staff s " +
                        "WHERE LOWER(s.staffType) = LOWER(:staffType) " +
                        "AND s.user.status = :status")
        long countByStaffTypeAndUserStatus(@Param("staffType") String staffType,
                        @Param("status") UserStatus status);

        @Query("SELECT s " +
                        "FROM Staff s " +
                        "WHERE LOWER(s.staffType) = LOWER(:staffType) " +
                        "AND (" +
                        "LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR EXISTS (" +
                        "SELECT account.id FROM StaffAccount account " +
                        "WHERE account.staff = s " +
                        "AND (LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(account.status) LIKE LOWER(CONCAT('%', :keyword, '%')))" +
                        ")" +
                        ")")
        Page<Staff> searchByStaffTypeAndKeyword(@Param("staffType") String staffType,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT s " +
                        "FROM Staff s " +
                        "WHERE LOWER(s.staffType) = LOWER(:staffType) " +
                        "AND (:status IS NULL OR s.user.status = :status) " +
                        "AND (:keyword = '' " +
                        "OR LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR EXISTS (" +
                        "SELECT account.id FROM StaffAccount account " +
                        "WHERE account.staff = s " +
                        "AND LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                        "))")
        Page<Staff> searchLibrariansWithStatus(
                        @Param("staffType") String staffType,
                        @Param("keyword") String keyword,
                        @Param("status") UserStatus status,
                        Pageable pageable);

        @Query("SELECT s " +
                        "FROM Staff s " +
                        "WHERE LOWER(s.staffType) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR EXISTS (" +
                        "SELECT account.id FROM StaffAccount account " +
                        "WHERE account.staff = s " +
                        "AND (LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(account.status) LIKE LOWER(CONCAT('%', :keyword, '%')))" +
                        ")")
        Page<Staff> searchStaffByKeyword(@Param("keyword") String keyword, Pageable pageable);

        long countByUser_Status(UserStatus status);

        @Query("SELECT s FROM Staff s " +
                        "WHERE (:status IS NULL OR s.user.status = :status) " +
                        "AND (:staffType = '' OR LOWER(s.staffType) = LOWER(:staffType)) " +
                        "AND (:keyword = '' " +
                        "OR LOWER(s.staffType) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(s.user.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR EXISTS (SELECT account.id FROM StaffAccount account " +
                        "WHERE account.staff = s " +
                        "AND LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
        Page<Staff> searchStaffWithStatus(@Param("keyword") String keyword,
                        @Param("status") UserStatus status,
                        @Param("staffType") String staffType,
                        Pageable pageable);
}
