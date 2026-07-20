package com.lms.repository;

import com.lms.entity.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffAccountRepository extends JpaRepository<StaffAccount, Integer> {
    Optional<StaffAccount> findByUsername(String username);

    Optional<StaffAccount> findByStaff_User_Email(String email);

    Optional<StaffAccount> findByStaff_User_Id(Integer userId);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Integer id);

    long countByStatusIgnoreCase(String status);
}
