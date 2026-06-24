package com.lms.repository;

import com.lms.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    // Tìm kiếm tài khoản hệ thống dựa trên Username đăng nhập
    Optional<Account> findByUsername(String username);

}