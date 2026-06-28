package com.lms.repository;

import com.lms.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

public interface MemberRepository extends JpaRepository<Member, Integer> {

    @Query("SELECT m FROM Member m WHERE m.user.id = :userId")
    Optional<Member> findByUserId(@Param("userId") Integer userId);

    Optional<Member> findByUserEmail(String email);

    Optional<Member> findByUserPhone(String phone);

    Page<Member> findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(
            String fullName,
            String email,
            String phone,
            Pageable pageable);
}