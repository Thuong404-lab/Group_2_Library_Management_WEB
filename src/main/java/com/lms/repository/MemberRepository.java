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
    Optional<Member> findByUserPhone(String phone);
    List<Member> findAll();

    Optional<Member> findByUserEmail(String email);

    // Dùng @Query để đảm bảo an toàn tuyệt đối, không lo sai quy tắc đặt tên hàm của Spring Data
    @Query("SELECT m FROM Member m WHERE m.user.id = :userId")
    Optional<Member> findByUserId(@Param("userId") Integer userId);

    Optional<Member> findByUserPhone(String phone);

    Page<Member> findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(
            String fullName,
            String email,
            String phone,
            Pageable pageable
    );
}

