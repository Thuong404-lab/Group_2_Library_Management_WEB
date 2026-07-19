package com.lms.repository;

import com.lms.entity.BorrowDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowDetailRepository extends JpaRepository<BorrowDetail, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bd from BorrowDetail bd where bd.borrowDetailId = :id")
    Optional<BorrowDetail> findByIdForUpdate(@Param("id") Integer id);
    long countByStatusIgnoreCase(String status);

    long countByBorrow_Member_MemberIdAndStatusIgnoreCase(Integer memberId, String status);

    long countByDueDateGreaterThanEqualAndDueDateLessThan(
            LocalDateTime startDate, LocalDateTime endDate);

    @Query(value = """
            select count(*)
            from dbo.BorrowDetails bd
            join dbo.Borrows b on b.borrow_id = bd.borrow_id
            where b.member_id = :memberId
              and bd.book_id = :bookId
              and upper(ltrim(rtrim(bd.status))) in (
                    'BORROWED',
                    'OVERDUE',
                    'RETURN_PENDING',
                    'RENEW_PENDING',
                    'RETURNED'
              )
            """, nativeQuery = true)
    long countEligibleReviewBorrows(@Param("memberId") Integer memberId,
                                    @Param("bookId") Integer bookId);

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

    @Query("SELECT COUNT(bd) FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.status IN ('Payment_Pending', 'Pending', 'Waiting_Pickup', 'Borrowed', 'Overdue', 'Return_Pending', 'Renew_Pending')")
    long countActiveBorrowedBooks(@Param("memberId") Integer memberId);

    @Query("SELECT bd FROM BorrowDetail bd JOIN MemberAccount ma ON bd.borrow.member = ma.member WHERE ma.username = :username AND bd.status IN ('Borrowed', 'Overdue')")
    List<BorrowDetail> findActiveBorrowDetailsByUsername(@Param("username") String username);

    @Query("SELECT bd FROM BorrowDetail bd JOIN MemberAccount ma ON bd.borrow.member = ma.member WHERE ma.username = :username AND bd.status = 'Returned'")
    List<BorrowDetail> findReturnedBorrowDetailsByUsername(@Param("username") String username);

    // Current loan workflow includes requests awaiting approval and approved copies awaiting pickup.
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.status IN ('Pending', 'Waiting_Pickup', 'Borrowed', 'Overdue', 'Return_Pending', 'Renew_Pending') ORDER BY bd.dueDate ASC")
    List<BorrowDetail> findCurrentBorrowsByMemberId(@Param("memberId") Integer memberId);

    // Bá»” SUNG 2: Láº¥y lá»‹ch sá»­ mÆ°á»£n tráº£ trong vÃ²ng 1 thÃ¡ng gáº§n Ä‘Ã¢y (Hiá»ƒn thá»‹ tab Lá»‹ch sá»­)
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.borrow.borrowDate >= :oneMonthAgo ORDER BY bd.borrow.borrowDate DESC")
    List<BorrowDetail> findBorrowHistoryInOneMonth(@Param("memberId") Integer memberId, @Param("oneMonthAgo") LocalDateTime oneMonthAgo);
    List<BorrowDetail> findByStatus(String status);
    List<BorrowDetail> findByStatusIgnoreCaseAndDueDateLessThanEqual(String status, LocalDateTime dueDate);

    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.bookItem.barcode = :barcode " +
            "AND bd.status IN ('Borrowed', 'Overdue', 'Return_Pending')")
    List<BorrowDetail> findActiveLoansByBarcode(@Param("barcode") String barcode);

    // THÃŠM QUERY 2: Láº¥y danh sÃ¡ch sÃ¡ch Ä‘Ã£ Ä‘Æ°á»£c tráº£ thÃ nh cÃ´ng trong ngÃ y hÃ´m nay
    @Query("SELECT bd FROM BorrowDetail bd WHERE bd.status = 'Returned' " +
            "AND bd.returnDate >= :startOfDay AND bd.returnDate <= :endOfDay ORDER BY bd.returnDate DESC")
    List<BorrowDetail> findReturnedBooksToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @EntityGraph(attributePaths = {"borrow.member.user", "book"})
    @Query("SELECT bd FROM BorrowDetail bd ORDER BY bd.borrow.borrowDate DESC")
    List<BorrowDetail> findRecentActivities(Pageable pageable);

    @Query("SELECT bd FROM BorrowDetail bd " +
           "JOIN FETCH bd.borrow b " +
           "JOIN FETCH b.member m " +
           "JOIN FETCH m.user mu " +
           "LEFT JOIN FETCH b.staff s " +
           "LEFT JOIN FETCH s.user su " +
           "LEFT JOIN FETCH bd.book bk " +
           "LEFT JOIN FETCH bd.bookItem bi " +
           "ORDER BY bd.borrowDetailId DESC")
    List<BorrowDetail> findAllBorrowDetailsWithRelationships();

    @Query("SELECT bd FROM BorrowDetail bd " +
            "WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.borrow.borrowDate >= :limitDate " +
            "ORDER BY bd.borrow.borrowDate DESC")
    List<BorrowDetail> findBorrowHistoryLimit365Days(@Param("memberId") Integer memberId, @Param("limitDate") java.time.LocalDateTime limitDate);

    @Query("SELECT bd FROM BorrowDetail bd " +
            "WHERE bd.borrow.member.memberId = :memberId " +
            "AND bd.borrow.borrowDate >= :startDate " +
            "AND bd.borrow.borrowDate <= :endDate " +
            "ORDER BY bd.borrow.borrowDate DESC")
    List<BorrowDetail> findBorrowHistoryByDateRange(
            @Param("memberId") Integer memberId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("""
            SELECT COUNT(bd)
            FROM BorrowDetail bd
            WHERE bd.borrow.member.memberId = :memberId
              AND bd.book.bookId = :bookId
              AND UPPER(TRIM(bd.status)) IN (
                    'PENDING',
                    'PAYMENT_PENDING',
                    'WAITING_PICKUP',
                    'BORROWED',
                    'OVERDUE',
                    'RETURN_PENDING',
                    'RENEW_PENDING'
              )
            """)
    long countActiveOrPendingRequestsByMemberAndBook(@Param("memberId") Integer memberId, @Param("bookId") Integer bookId);
}

