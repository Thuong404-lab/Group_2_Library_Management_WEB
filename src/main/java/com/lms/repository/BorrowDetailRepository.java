package com.lms.repository;

import com.lms.entity.BorrowDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // BỔ SUNG: Tìm các chi tiết thuộc về một đơn mượn tổng cụ thể
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.borrowId = :borrowId")
    List<BorrowDetail> findByBorrowId(@Param("borrowId") Integer borrowId);

    // BỔ SUNG: Đếm số sách ĐANG mượn thực tế của Member (Trạng thái Borrowed hoặc Overdue)
    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId AND bd.status IN ('Borrowed', 'Overdue')")
    long countActiveBorrowedBooks(@Param("memberId") Integer memberId);

    @Query("SELECT bd FROM BorrowDetail bd JOIN MemberAccount ma ON bd.borrow.member = ma.member WHERE ma.username = :username AND bd.status IN ('Borrowed', 'Overdue')")
    List<BorrowDetail> findActiveBorrowDetailsByUsername(@Param("username") String username);

    @Query("SELECT bd FROM BorrowDetail bd JOIN MemberAccount ma ON bd.borrow.member = ma.member WHERE ma.username = :username AND bd.status = 'Returned'")
    List<BorrowDetail> findReturnedBorrowDetailsByUsername(@Param("username") String username);
}