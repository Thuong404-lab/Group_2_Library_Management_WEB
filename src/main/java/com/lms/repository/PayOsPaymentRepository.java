package com.lms.repository;

import com.lms.entity.PayOsPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;

public interface PayOsPaymentRepository extends JpaRepository<PayOsPayment, Long> {
    Optional<PayOsPayment> findByOrderCode(Long orderCode);

    Optional<PayOsPayment> findByOrderCodeAndMemberMemberId(Long orderCode, Integer memberId);

    Optional<PayOsPayment> findFirstByMemberMemberIdAndPurposeAndReferenceIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            Integer memberId, String purpose, Integer referenceId, String status, LocalDateTime createdAfter);

    Optional<PayOsPayment> findFirstByMemberMemberIdAndPurposeAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            Integer memberId, String purpose, String status, LocalDateTime createdAfter);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PayOsPayment p where p.orderCode = :orderCode")
    Optional<PayOsPayment> findByOrderCodeForUpdate(@Param("orderCode") Long orderCode);
}
