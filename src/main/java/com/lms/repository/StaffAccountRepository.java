package com.lms.repository;

import com.lms.entity.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface StaffAccountRepository extends JpaRepository<StaffAccount, Integer> {
    Optional<StaffAccount> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM StaffAccount account JOIN FETCH account.staff WHERE account.username = :username")
    Optional<StaffAccount> findByUsernameForNotificationSend(@Param("username") String username);

    Optional<StaffAccount> findByStaff_User_Email(String email);

    Optional<StaffAccount> findByStaff_User_Id(Integer userId);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Integer id);

    long countByStatusIgnoreCase(String status);

    @Query(value = """
            SELECT account
            FROM StaffAccount account
            JOIN FETCH account.staff staff
            JOIN FETCH staff.user user
            WHERE LOWER(staff.staffType) = LOWER(:staffType)
              AND (:status = '' OR LOWER(account.status) = LOWER(:status))
              AND (:keyword = ''
                   OR LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
            countQuery = """
            SELECT COUNT(account)
            FROM StaffAccount account
            JOIN account.staff staff
            JOIN staff.user user
            WHERE LOWER(staff.staffType) = LOWER(:staffType)
              AND (:status = '' OR LOWER(account.status) = LOWER(:status))
              AND (:keyword = ''
                   OR LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<StaffAccount> searchDirectory(
            @Param("staffType") String staffType,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
            SELECT account.status, COUNT(account)
            FROM StaffAccount account
            JOIN account.staff staff
            WHERE LOWER(staff.staffType) = LOWER(:staffType)
            GROUP BY account.status
            """)
    List<Object[]> countDirectoryByStatus(@Param("staffType") String staffType);
}
