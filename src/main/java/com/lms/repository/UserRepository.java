package com.lms.repository;

import com.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // UC-4.1 & UC-16.1: Find User by Email
    Optional<User> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);

    // Check if email exists for another user (used during update profile)
    boolean existsByEmailAndIdNot(String email, Integer userId);
}