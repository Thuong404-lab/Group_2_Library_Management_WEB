package com.lms.repository;

import com.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // UC-4.1 & UC-16.1: Thêm phương thức tìm kiếm User theo Email (được Spring Data JPA tự động sinh câu lệnh SQL)
    Optional<User> findByEmail(String email);

}