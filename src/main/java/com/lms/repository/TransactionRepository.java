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

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

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

    Page<Transaction> findByWalletMemberMemberIdAndTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
            Integer memberId,
            String transactionType,
            Pageable pageable);

    Page<Transaction> findByWalletMemberMemberIdOrderByTransactionDateDesc(
            Integer memberId,
            Pageable pageable);
}
