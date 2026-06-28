package com.lms.repository;

import com.lms.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

public interface MemberRepository extends JpaRepository<Member, Integer> {

    Optional<Member> findByUserId(Integer userId);

    Optional<Member> findByUserEmail(String email);

    Optional<Member> findByUserPhone(String phone);

    Page<Member> findByUserFullNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrUserPhoneContainingIgnoreCase(
            String fullName,
            String email,
            String phone,
            Pageable pageable);
}