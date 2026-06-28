package com.lms.repository;

import com.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
<<<<<<< HEAD
    boolean existsByEmail(String email);
}
=======

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Integer userId);
}
>>>>>>> 6d3c78b6792dfd9160778a90e85d05cb6dca4c8f
