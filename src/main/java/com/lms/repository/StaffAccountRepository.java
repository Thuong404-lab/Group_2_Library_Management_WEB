package com.lms.repository;

import com.lms.entity.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
