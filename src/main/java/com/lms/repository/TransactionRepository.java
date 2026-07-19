package com.lms.repository;

import com.lms.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// NgÆ°á»i phá»¥ trÃ¡ch: Tráº§n Ngá»c Linh Äang (CE191088)

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    Optional<Transaction> findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
            Integer borrowDetailId, String transactionType, String status);

    long countByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCase(Integer borrowDetailId, String transactionType);

    Optional<Transaction> findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDescTransactionIdDesc(
            Integer borrowDetailId, String transactionType, String status);

    @Query("select coalesce(sum(t.amount), 0) " +
            "from Transaction t " +
            "where lower(t.status) = lower(:status) " +
            "and t.transactionDate >= :fromDate " +
            "and t.transactionDate < :toDate")
    BigDecimal sumAmountByStatusAndDateRange(@Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("select t.transactionType, count(t), coalesce(sum(t.amount), 0) " +
            "from Transaction t " +
            "where lower(t.status) = lower(:status) " +
            "and t.transactionDate >= :fromDate " +
            "and t.transactionDate < :toDate " +
            "group by t.transactionType " +
            "order by coalesce(sum(t.amount), 0) desc")
    List<Object[]> summarizeByTypeAndStatusAndDateRange(@Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("select month(t.transactionDate), year(t.transactionDate), count(t), coalesce(sum(t.amount), 0) " +
            "from Transaction t " +
            "where lower(t.status) = lower(:status) " +
            "and t.transactionDate >= :fromDate " +
            "and t.transactionDate < :toDate " +
            "group by year(t.transactionDate), month(t.transactionDate) " +
            "order by year(t.transactionDate), month(t.transactionDate)")
    List<Object[]> summarizeMonthlyRevenueByStatusAndDateRange(@Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    List<Transaction> findTop5ByOrderByTransactionDateDesc();

    List<Transaction> findTop10ByTransactionTypeIgnoreCaseOrderByTransactionDateDesc(String transactionType);

    Page<Transaction> findByTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
            String transactionType,
            Pageable pageable);

    Page<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);

    @Query("""
            select t
            from Transaction t
            where upper(t.transactionType) = upper(:transactionType)
              and t.transactionDate >= :fromDate
              and t.transactionDate < :toDate
            order by t.transactionDate desc
            """)
    List<Transaction> findByTransactionTypeAndDateRange(@Param("transactionType") String transactionType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where upper(t.transactionType) = upper(:transactionType)
              and t.transactionDate >= :fromDate
              and t.transactionDate < :toDate
            """)
    BigDecimal sumAmountByTransactionTypeAndDateRange(@Param("transactionType") String transactionType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("""
            select t
            from Transaction t
            where t.wallet.member.memberId = :memberId
              and upper(t.transactionType) in :types
              and (t.status is null or lower(t.status) not in ('completed', 'paid'))
            order by t.transactionDate desc
            """)
    List<Transaction> findUnpaidFineTransactions(@Param("memberId") Integer memberId,
            @Param("types") List<String> types);

    @Query("""
            select t
            from Transaction t
            where upper(t.transactionType) in :types
              and (t.status is null or lower(t.status) not in ('completed', 'paid'))
            order by t.transactionDate asc, t.transactionId asc
            """)
    List<Transaction> findAllPendingFineTransactions(@Param("types") List<String> types);

    @Query("""
            select t
            from Transaction t
            where t.borrow.borrowId = :borrowId
              and upper(t.transactionType) in :types
              and (t.status is null or lower(t.status) not in ('completed', 'paid'))
            order by t.transactionDate asc, t.transactionId asc
            """)
    List<Transaction> findPendingFineTransactionsByBorrowId(@Param("borrowId") Integer borrowId,
            @Param("types") List<String> types);

    Optional<Transaction> findFirstByBorrowBorrowIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDesc(
            Integer borrowId,
            String transactionType,
            String status);

    @Query("""
            select case when count(t) > 0 then true else false end
            from Transaction t
            where t.wallet.member.memberId = :memberId
              and t.borrow.borrowId = :borrowId
              and upper(t.transactionType) = upper(:transactionType)
              and lower(t.status) in ('completed', 'paid')
            """)
    boolean existsCompletedTransactionForBorrow(@Param("memberId") Integer memberId,
            @Param("borrowId") Integer borrowId,
            @Param("transactionType") String transactionType);

    @Query("""
            select t
            from Transaction t
            where t.wallet.member.memberId = :memberId
              and t.borrow.borrowId = :borrowId
              and upper(t.transactionType) = upper(:transactionType)
              and lower(t.status) in ('completed', 'paid')
            order by t.transactionDate desc, t.transactionId desc
            """)
    List<Transaction> findCompletedTransactionsForBorrow(@Param("memberId") Integer memberId,
            @Param("borrowId") Integer borrowId,
            @Param("transactionType") String transactionType);

    default Optional<Transaction> findLatestCompletedBorrowFee(Integer memberId, Integer borrowId) {
        return findCompletedTransactionsForBorrow(memberId, borrowId, "BORROW_FEE").stream().findFirst();
    }

    default boolean hasCompletedBorrowFee(Integer memberId, Integer borrowId) {
        return existsCompletedTransactionForBorrow(memberId, borrowId, "BORROW_FEE");
    }

    Page<Transaction> findByWalletMemberMemberIdAndTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
            Integer memberId,
            String transactionType,
            Pageable pageable);

    Page<Transaction> findByWalletMemberMemberIdOrderByTransactionDateDesc(
            Integer memberId,
            Pageable pageable);
    List<Transaction> findByBorrow_BorrowId(Integer borrowId);

    // =========================================================================
    // PHÆ¯Æ NG THá»¨C Má»šI: Láº¥y táº¥t cáº£ giao dá»‹ch tÃ i chÃ­nh phÃ¡t sinh cá»§a Äá»™c giáº£ trong vÃ²ng 365 ngÃ y qua
    // =========================================================================
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.wallet.member.memberId = :memberId " +
            "AND t.transactionDate >= :limitDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsByMemberIdLimit365Days(@Param("memberId") Integer memberId, @Param("limitDate") LocalDateTime limitDate);

    @Query("SELECT MIN(t.transactionDate) FROM Transaction t WHERE t.wallet.member.memberId = :memberId")
    LocalDateTime findMinTransactionDateByMemberId(@Param("memberId") Integer memberId);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.wallet.member.memberId = :memberId " +
            "AND t.transactionDate >= :startDate " +
            "AND t.transactionDate <= :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsByMemberIdAndDateRange(
            @Param("memberId") Integer memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

