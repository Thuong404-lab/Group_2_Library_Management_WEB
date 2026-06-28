package com.lms.repository;

import com.lms.entity.BorrowDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowDetailRepository extends JpaRepository<BorrowDetail, Integer> {
    long countByStatusIgnoreCase(String status);
    List<BorrowDetail> findTop5ByStatusIgnoreCaseAndDueDateBetweenOrderByDueDateAsc(
            String status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}