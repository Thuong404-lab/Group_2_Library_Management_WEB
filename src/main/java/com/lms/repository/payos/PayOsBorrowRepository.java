package com.lms.repository.payos;

import com.lms.entity.Borrow;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PayOsBorrowRepository extends JpaRepository<Borrow, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Borrow b where b.borrowId = :borrowId")
    Optional<Borrow> findByIdForUpdate(@Param("borrowId") Integer borrowId);
}
