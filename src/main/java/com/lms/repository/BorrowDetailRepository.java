package com.lms.repository;
import com.lms.entity.BorrowDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BorrowDetailRepository extends JpaRepository<BorrowDetail, Integer> {
}
