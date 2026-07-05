package com.lms.repository;

import com.lms.entity.BorrowDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BorrowDetailRepository extends JpaRepository<BorrowDetail, Integer> {
    long countByStatusIgnoreCase(String status);

    @Query("select count(bd) " +
            "from BorrowDetail bd " +
            "where bd.borrow.borrowDate >= :startDate and bd.borrow.borrowDate < :endDate")
    long countBorrowedItemsByBorrowDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("select count(bd) " +
            "from BorrowDetail bd " +
            "where bd.returnDate is not null " +
            "and bd.returnDate >= :startDate and bd.returnDate < :endDate " +
            "and bd.returnDate <= bd.dueDate")
    long countOnTimeReturnsByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("select count(bd) " +
            "from BorrowDetail bd " +
            "where bd.returnDate is not null " +
            "and bd.returnDate >= :startDate and bd.returnDate < :endDate " +
            "and bd.returnDate > bd.dueDate")
    long countLateReturnsByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("select bd.book.title, coalesce(bd.book.isbn, ''), count(bd) " +
            "from BorrowDetail bd " +
            "where bd.borrow.borrowDate >= :startDate and bd.borrow.borrowDate < :endDate " +
            "group by bd.book.bookId, bd.book.title, bd.book.isbn " +
            "order by count(bd) desc")
    List<Object[]> findTopBorrowedBooks(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("select bd.borrow.member.user.fullName, bd.borrow.member.user.email, count(bd) " +
            "from BorrowDetail bd " +
            "where bd.borrow.borrowDate >= :startDate and bd.borrow.borrowDate < :endDate " +
            "group by bd.borrow.member.memberId, bd.borrow.member.user.fullName, bd.borrow.member.user.email " +
            "order by count(bd) desc")
    List<Object[]> findTopBorrowingMembers(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    List<BorrowDetail> findTop5ByStatusIgnoreCaseAndDueDateBetweenOrderByDueDateAsc(
            String status, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.borrowId = :borrowId")
    List<BorrowDetail> findByBorrowId(@Param("borrowId") Integer borrowId);

    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId AND bd.status IN ('Borrowed', 'Overdue')")
    long countActiveBorrowedBooks(@Param("memberId") Integer memberId);

    // BỔ SUNG 1: Lấy danh sách sách ĐANG mượn của độc giả (Hiển thị tab Đang mượn)
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId AND bd.status IN ('Borrowed', 'Overdue') ORDER BY bd.dueDate ASC")
    List<BorrowDetail> findCurrentBorrowsByMemberId(@Param("memberId") Integer memberId);

    // BỔ SUNG 2: Lấy lịch sử mượn trả trong vòng 1 tháng gần đây (Hiển thị tab Lịch sử)
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.borrow.borrowDate >= :oneMonthAgo ORDER BY bd.borrow.borrowDate DESC")
    List<BorrowDetail> findBorrowHistoryInOneMonth(@Param("memberId") Integer memberId, @Param("oneMonthAgo") LocalDateTime oneMonthAgo);
}
