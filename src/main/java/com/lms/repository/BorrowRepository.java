package com.lms.repository;

import com.lms.entity.Borrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Integer> {
    long countByStatusIgnoreCase(String status);
    List<Borrow> findTop5ByOrderByBorrowDateDesc();
    long countByBorrowDateGreaterThanEqualAndBorrowDateLessThan(
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}