package com.lms.repository;

import com.lms.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// NgÆ°á»i phá»¥ trÃ¡ch: Tráº§n Ngá»c Linh Äang (CE191088)

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"wallet", "wallet.member", "wallet.member.user", "borrow", "borrowDetail"})
    @Query("select t from Transaction t where t.transactionId = :transactionId")
    Optional<Transaction> findByIdForUpdate(@Param("transactionId") Integer transactionId);

    boolean existsByReferenceCode(String referenceCode);

    Optional<Transaction> findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionIdDesc(
            Integer borrowDetailId, String transactionType, String status);

    long countByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCase(Integer borrowDetailId, String transactionType);

    Optional<Transaction> findFirstByBorrowDetailBorrowDetailIdAndTransactionTypeIgnoreCaseAndStatusIgnoreCaseOrderByTransactionDateDescTransactionIdDesc(
            Integer borrowDetailId, String transactionType, String status);

    @Query("""
            select coalesce(sum(abs(t.amount)), 0)
            from Transaction t
            where lower(t.status) = lower(:status)
              and upper(t.transactionType) in :revenueTypes
              and t.transactionDate >= :fromDate
              and t.transactionDate < :toDate
            """)
    BigDecimal sumRevenueByStatusAndTypesAndDateRange(@Param("status") String status,
            @Param("revenueTypes") List<String> revenueTypes,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("select t.transactionType, count(t), coalesce(sum(abs(t.amount)), 0) " +
            "from Transaction t where lower(t.status) = lower(:status) " +
            "and upper(t.transactionType) in :types and t.transactionDate >= :fromDate " +
            "and t.transactionDate < :toDate group by t.transactionType " +
            "order by coalesce(sum(abs(t.amount)), 0) desc")
    List<Object[]> summarizeRevenueByTypeAndDateRange(@Param("status") String status,
            @Param("types") List<String> types,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("select month(t.transactionDate), year(t.transactionDate), count(t), coalesce(sum(abs(t.amount)), 0) " +
            "from Transaction t where lower(t.status) = lower(:status) " +
            "and upper(t.transactionType) in :types and t.transactionDate >= :fromDate " +
            "and t.transactionDate < :toDate group by year(t.transactionDate), month(t.transactionDate) " +
            "order by year(t.transactionDate), month(t.transactionDate)")
    List<Object[]> summarizeMonthlyRevenueByTypesAndDateRange(@Param("status") String status,
            @Param("types") List<String> types,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("select coalesce(sum(abs(t.amount)), 0) from Transaction t " +
            "where lower(t.status) = lower(:status) and upper(t.transactionType) = upper(:type) " +
            "and t.transactionDate >= :fromDate and t.transactionDate < :toDate")
    BigDecimal sumAbsoluteAmountByTypeAndStatusAndDateRange(@Param("type") String type,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    List<Transaction> findTop5ByOrderByTransactionDateDesc();

    List<Transaction> findTop10ByTransactionTypeIgnoreCaseOrderByTransactionDateDesc(String transactionType);

    Page<Transaction> findByTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
            String transactionType,
            Pageable pageable);

    Page<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);

    @EntityGraph(attributePaths = {
            "wallet", "wallet.member", "wallet.member.user",
            "performedByStaff", "performedByStaff.user", "borrow"
    })
    @Query("""
            select t
            from Transaction t
            join t.wallet w
            join w.member m
            join m.user u
            where (:type is null or upper(t.transactionType) = :type)
              and (:status is null or upper(t.status) = :status)
              and (:channel is null or upper(t.channel) = :channel)
              and (:fromDate is null or t.transactionDate >= :fromDate)
              and (:toDate is null or t.transactionDate < :toDate)
              and (:query = ''
                   or lower(u.fullName) like lower(concat('%', :query, '%'))
                   or lower(u.email) like lower(concat('%', :query, '%'))
                   or lower(coalesce(u.phone, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(t.referenceCode, '')) like lower(concat('%', :query, '%'))
                   or (:transactionIdQuery is not null and t.transactionId = :transactionIdQuery)
                   or (:memberIdQuery is not null and m.memberId = :memberIdQuery))
            order by t.transactionDate desc, t.transactionId desc
            """)
    Page<Transaction> searchForLibrarian(@Param("query") String query,
            @Param("transactionIdQuery") Integer transactionIdQuery,
            @Param("memberIdQuery") Integer memberIdQuery,
            @Param("type") String type,
            @Param("status") String status,
            @Param("channel") String channel,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

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

    @EntityGraph(attributePaths = {"wallet", "wallet.member", "wallet.member.user", "performedByStaff", "performedByStaff.user"})
    @Query("""
            select t
            from Transaction t
            where upper(t.transactionType) = upper(:transactionType)
              and lower(t.status) in :statuses
              and t.transactionDate >= :fromDate
              and t.transactionDate < :toDate
            order by t.transactionDate desc, t.transactionId desc
            """)
    Page<Transaction> findCompletedTopUpsByDateRange(@Param("transactionType") String transactionType,
            @Param("statuses") List<String> statuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @EntityGraph(attributePaths = {"wallet", "wallet.member", "wallet.member.user", "performedByStaff", "performedByStaff.user"})
    @Query("""
            select t
            from Transaction t
            where upper(t.transactionType) = upper(:transactionType)
              and lower(t.status) in :statuses
            order by t.transactionDate desc, t.transactionId desc
            """)
    List<Transaction> findRecentCompletedTopUps(@Param("transactionType") String transactionType,
            @Param("statuses") List<String> statuses,
            Pageable pageable);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where upper(t.transactionType) = upper(:transactionType)
              and lower(t.status) in :statuses
              and t.transactionDate >= :fromDate
              and t.transactionDate < :toDate
            """)
    BigDecimal sumCompletedTopUpsByDateRange(@Param("transactionType") String transactionType,
            @Param("statuses") List<String> statuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("""
            select t
            from Transaction t
            where t.wallet.member.memberId = :memberId
              and upper(t.transactionType) in :types
              and lower(t.status) = 'pending'
            order by t.transactionDate desc
            """)
    List<Transaction> findUnpaidFineTransactions(@Param("memberId") Integer memberId,
            @Param("types") List<String> types);

    @EntityGraph(attributePaths = {"wallet", "wallet.member", "wallet.member.user", "borrow", "borrowDetail", "borrowDetail.book", "borrowDetail.bookItem"})
    @Query("""
            select t
            from Transaction t
            where upper(t.transactionType) in :types
              and lower(t.status) = 'pending'
            order by t.transactionDate asc, t.transactionId asc
            """)
    List<Transaction> findAllPendingFineTransactions(@Param("types") List<String> types);

    @Query("""
            select t
            from Transaction t
            where t.borrow.borrowId = :borrowId
              and upper(t.transactionType) in :types
              and lower(t.status) = 'pending'
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

    Page<Transaction> findByWalletMemberMemberIdAndTransactionDateGreaterThanEqualAndTransactionDateLessThanOrderByTransactionDateDesc(
            Integer memberId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    List<Transaction> findByBorrow_BorrowId(Integer borrowId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"performedByStaff.user"})
    List<Transaction> findByBorrow_BorrowIdInOrderByTransactionDateDesc(List<Integer> borrowIds);

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

    @Query("""
            select coalesce(sum(abs(t.amount)), 0)
            from Transaction t
            where t.wallet.member.memberId = :memberId
              and upper(t.transactionType) in ('BORROW_FEE', 'RENEWAL_FEE')
              and lower(t.status) in ('completed', 'paid')
            """)
    BigDecimal sumCompletedMembershipSpendByMemberId(@Param("memberId") Integer memberId);

    @Query("""
            select t.wallet.member.memberId, coalesce(sum(abs(t.amount)), 0)
            from Transaction t
            where upper(t.transactionType) in ('BORROW_FEE', 'RENEWAL_FEE')
              and lower(t.status) in ('completed', 'paid')
            group by t.wallet.member.memberId
            order by sum(abs(t.amount)) desc, t.wallet.member.memberId asc
            """)
    List<Object[]> findTopMembersByCompletedMembershipSpend(Pageable pageable);
}
