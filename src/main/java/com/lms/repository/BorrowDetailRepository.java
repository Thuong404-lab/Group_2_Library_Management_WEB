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
            String status, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.borrowId = :borrowId")
    List<BorrowDetail> findByBorrowId(@Param("borrowId") Integer borrowId);

    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId AND bd.status IN ('Borrowed', 'Overdue', 'Return_Pending')")
    long countActiveBorrowedBooks(@Param("memberId") Integer memberId);

    // BỔ SUNG & CẬP NHẬT 1: Lấy danh sách sách hiện tại bao gồm cả Pending và Return_Pending (Vấn đề 7)
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.status IN ('Pending', 'Borrowed', 'Overdue', 'Return_Pending') ORDER BY bd.dueDate ASC")
    List<BorrowDetail> findCurrentBorrowsByMemberId(@Param("memberId") Integer memberId);

    // BỔ SUNG 2: Lấy lịch sử mượn trả trong vòng 1 tháng gần đây (Hiển thị tab Lịch sử)
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.borrow.borrowDate >= :oneMonthAgo ORDER BY bd.borrow.borrowDate DESC")
    List<BorrowDetail> findBorrowHistoryInOneMonth(@Param("memberId") Integer memberId, @Param("oneMonthAgo") LocalDateTime oneMonthAgo);

    // BỔ SUNG 3: Tìm danh sách chi tiết mượn theo trạng thái cụ thể để xử lý DTO phía Librarian
    List<BorrowDetail> findByStatus(String status);
}