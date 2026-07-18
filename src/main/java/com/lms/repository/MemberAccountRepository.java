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
    Optional<MemberAccount> findByMember_User_Phone(String phone);
    Optional<MemberAccount> findByMember_User_Id(Integer userId);
    Optional<MemberAccount> findByMemberMemberId(Integer memberId);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Integer id);
    long countByStatusIgnoreCase(String status);

    @Query("""
            SELECT m
            FROM MemberAccount m
            LEFT JOIN m.member member
            LEFT JOIN member.user user
            WHERE LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(user.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<MemberAccount> searchMemberAccounts(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT m
            FROM MemberAccount m
            LEFT JOIN m.member member
            LEFT JOIN member.user user
            LEFT JOIN member.tier tier
            WHERE (:keyword = ''
                   OR LOWER(m.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status = '' OR LOWER(m.status) = LOWER(:status))
              AND (:tierName = '' OR LOWER(tier.tierName) = LOWER(:tierName))
            """)
    Page<MemberAccount> searchMemberAccountsWithFilters(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("tierName") String tierName,
            Pageable pageable);
}
