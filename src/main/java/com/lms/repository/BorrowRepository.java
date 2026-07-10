package com.lms.repository;

import com.lms.entity.Borrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    // BỔ SUNG: Tìm kiếm lịch sử yêu cầu mượn sách của độc giả
    @Query("select month(b.borrowDate), year(b.borrowDate), count(b) " +
            "from Borrow b " +
            "where b.borrowDate >= :startDate and b.borrowDate < :endDate " +
            "group by year(b.borrowDate), month(b.borrowDate) " +
            "order by year(b.borrowDate), month(b.borrowDate)")
    List<Object[]> countMonthlyBorrows(LocalDateTime startDate, LocalDateTime endDate);

    List<Borrow> findByMember_MemberIdOrderByBorrowDateDesc(Integer memberId);
}
