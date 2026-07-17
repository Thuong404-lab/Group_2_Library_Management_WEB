package com.lms.repository;

import com.lms.entity.PayOsPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

public interface PayOsPaymentRepository extends JpaRepository<PayOsPayment, Long> {
    Optional<PayOsPayment> findByOrderCode(Long orderCode);

    Optional<PayOsPayment> findByOrderCodeAndMemberMemberId(Long orderCode, Integer memberId);

    List<PayOsPayment> findTop10ByMemberMemberIdAndStatusOrderByPaidAtDesc(
            Integer memberId, String status);

    @Query("""
            select p.orderCode from PayOsPayment p
            where p.transaction is null
              and upper(p.status) in :statuses
              and p.createdAt >= :createdAfter
            order by p.createdAt
            """)
    List<Long> findRecoverableOrderCodes(@Param("statuses") List<String> statuses,
                                         @Param("createdAfter") LocalDateTime createdAfter);

    Optional<PayOsPayment> findFirstByMemberMemberIdAndPurposeAndReferenceIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            Integer memberId, String purpose, Integer referenceId, String status, LocalDateTime createdAfter);

    Optional<PayOsPayment> findFirstByMemberMemberIdAndPurposeAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            Integer memberId, String purpose, String status, LocalDateTime createdAfter);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PayOsPayment p where p.orderCode = :orderCode")
    Optional<PayOsPayment> findByOrderCodeForUpdate(@Param("orderCode") Long orderCode);

    @EntityGraph(attributePaths = {"member", "member.user", "transaction"})
    @Query("""
            select p from PayOsPayment p
            join p.member m
            join m.user u
            where (:keyword = ''
                or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.paymentLinkId, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.bankReference, '')) like lower(concat('%', :keyword, '%'))
                or (:orderCode is not null and p.orderCode = :orderCode))
              and (:status = '' or upper(p.status) = upper(:status))
              and (:purpose = '' or upper(p.purpose) = upper(:purpose))
              and (:fromDate is null or p.createdAt >= :fromDate)
              and (:toDate is null or p.createdAt < :toDate)
            order by p.createdAt desc
            """)
    Page<PayOsPayment> search(@Param("keyword") String keyword,
                              @Param("orderCode") Long orderCode,
                              @Param("status") String status,
                              @Param("purpose") String purpose,
                              @Param("fromDate") LocalDateTime fromDate,
                              @Param("toDate") LocalDateTime toDate,
                              Pageable pageable);
}
