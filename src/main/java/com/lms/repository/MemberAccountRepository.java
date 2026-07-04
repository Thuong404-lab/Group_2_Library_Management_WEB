package com.lms.repository;

import com.lms.entity.MemberAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberAccountRepository extends JpaRepository<MemberAccount, Integer> {
    Optional<MemberAccount> findByUsername(String username);
    Optional<MemberAccount> findByMember_User_Email(String email);
    Optional<MemberAccount> findByMember_User_Id(Integer userId);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Integer id);

    @Query("SELECT m FROM MemberAccount m WHERE m.username LIKE %:keyword% OR m.member.user.fullName LIKE %:keyword%")
    Page<MemberAccount> searchMemberAccounts(@Param("keyword") String keyword, Pageable pageable);
}
