package com.lms.repository.payos;

import com.lms.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PayOsTransactionRepository extends JpaRepository<Transaction, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaction t where t.transactionId = :transactionId")
    Optional<Transaction> findByIdForUpdate(@Param("transactionId") Integer transactionId);

    @Query("""
            select case when count(t) > 0 then true else false end
            from Transaction t
            where t.wallet.member.memberId = :memberId
              and t.borrow.borrowId = :borrowId
              and upper(t.transactionType) = 'BORROW_FEE'
              and lower(t.status) in ('completed', 'paid')
            """)
    boolean hasCompletedBorrowFee(@Param("memberId") Integer memberId,
                                  @Param("borrowId") Integer borrowId);
}
