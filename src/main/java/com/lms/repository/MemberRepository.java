package com.lms.repository;

import com.lms.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByUserId(Integer userId);
    List<Member> findAll();

    // Kept for compatibility with IDE incremental builds that may still load
    // the previous member-summary implementation. The current service does not
    // call this method after tier summary chips were removed.
    long countByTier_TierNameIgnoreCase(String tierName);

    /** Kiểm tra tier đang được dùng — O(1), thay thế findAll().stream().anyMatch() */
    boolean existsByTier_TierId(Integer tierId);

    /** Số lượng member ở một tier cụ thể */
    long countByTier_TierId(Integer tierId);

    /** GROUP BY tier_id để build map đếm member trong 1 query */
    @Query("SELECT m.tier.tierId, COUNT(m) FROM Member m WHERE m.tier IS NOT NULL GROUP BY m.tier.tierId")
    List<Object[]> countGroupByTierId();

    Optional<Member> findByUserEmail(String email);

    Optional<Member> findByUserPhone(String phone);

    Page<Member> findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(
            String fullName,
            String email,
            String phone,
            Pageable pageable
    );

    @Query("""
            SELECT a.member FROM MemberAccount a WHERE a.username = :username
            """)
    Optional<Member> findByAccountUsername(@Param("username") String username);

    @Query("""
            SELECT account.member
            FROM MemberAccount account
            WHERE LOWER(account.status) = 'active'
            ORDER BY account.member.memberId
            """)
    List<Member> findAllWithActiveAccount();

    @Query("""
            SELECT account.member
            FROM MemberAccount account
            WHERE LOWER(account.status) = 'active'
              AND account.member.memberId IN :memberIds
            """)
    List<Member> findAllWithActiveAccountByMemberIdIn(@Param("memberIds") List<Integer> memberIds);
}
