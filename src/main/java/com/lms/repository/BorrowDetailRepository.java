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

    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId AND bd.status IN ('Borrowed', 'Overdue', 'Return_Pending')")
    long countActiveBorrowedBooks(@Param("memberId") Integer memberId);

    @Query("SELECT bd FROM BorrowDetail bd JOIN MemberAccount ma ON bd.borrow.member = ma.member WHERE ma.username = :username AND bd.status IN ('Borrowed', 'Overdue')")
    List<BorrowDetail> findActiveBorrowDetailsByUsername(@Param("username") String username);

    @Query("SELECT bd FROM BorrowDetail bd JOIN MemberAccount ma ON bd.borrow.member = ma.member WHERE ma.username = :username AND bd.status = 'Returned'")
    List<BorrowDetail> findReturnedBorrowDetailsByUsername(@Param("username") String username);

    // BỔ SUNG & CẬP NHẬT 1: Lấy danh sách sách hiện tại bao gồm cả Pending và Return_Pending (Vấn đề 7)
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.status IN ('Pending', 'Borrowed', 'Overdue', 'Return_Pending') ORDER BY bd.dueDate ASC")
    List<BorrowDetail> findCurrentBorrowsByMemberId(@Param("memberId") Integer memberId);

    // BỔ SUNG 2: Lấy lịch sử mượn trả trong vòng 1 tháng gần đây (Hiển thị tab Lịch sử)
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.borrow.borrowDate >= :oneMonthAgo ORDER BY bd.borrow.borrowDate DESC")
    List<BorrowDetail> findBorrowHistoryInOneMonth(@Param("memberId") Integer memberId, @Param("oneMonthAgo") LocalDateTime oneMonthAgo);
    List<BorrowDetail> findByStatus(String status);

    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.bookItem.barcode = :barcode " +
            "AND bd.status IN ('Borrowed', 'Overdue', 'Return_Pending')")
    List<BorrowDetail> findActiveLoansByBarcode(@Param("barcode") String barcode);

    // THÊM QUERY 2: Lấy danh sách sách đã được trả thành công trong ngày hôm nay
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.status = 'Returned' " +
            "AND bd.returnDate >= :startOfDay AND bd.returnDate <= :endOfDay ORDER BY bd.returnDate DESC")
    List<BorrowDetail> findReturnedBooksToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

}
