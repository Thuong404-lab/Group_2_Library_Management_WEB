package com.lms.repository;
import com.lms.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
    Optional<Member> findByUserId(Integer userId);
    Optional<Member> findByUserPhone(String phone);

    Optional<Member> findByUserEmail(String email);

    Page<Member> findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(
            String fullName,
            String email,
            String phone,
            Pageable pageable
    );
}
