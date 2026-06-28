package com.lms.repository;

import com.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

<<<<<<< HEAD
    // UC-4.1 & UC-16.1: Thêm phương thức tìm kiếm User theo Email (được Spring Data JPA tự động sinh câu lệnh SQL)
    Optional<User> findByEmail(String email);

}
=======
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Integer userId);
}
>>>>>>> 397a84201207f9edbcf0a90deefeee0d71932a23
