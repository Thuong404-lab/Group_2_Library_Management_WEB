package com.lms.repository;

import com.lms.entity.PayOsPaymentAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PayOsPaymentAuditLogRepository extends JpaRepository<PayOsPaymentAuditLog, Long> {
    @EntityGraph(attributePaths = {"actor", "payment", "payment.member", "payment.member.user"})
    Page<PayOsPaymentAuditLog> findByPaymentPaymentIdOrderByCreatedAtDesc(Long paymentId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "payment", "payment.member", "payment.member.user"})
    @Query("""
            select a from PayOsPaymentAuditLog a
            join a.payment p
            join p.member m
            join m.user u
            where (:keyword = ''
                or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%'))
                or lower(a.eventType) like lower(concat('%', :keyword, '%'))
                or lower(a.source) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(a.message, '')) like lower(concat('%', :keyword, '%'))
                or (:orderCode is not null and p.orderCode = :orderCode))
              and (:eventType = '' or upper(a.eventType) = upper(:eventType))
              and (:fromDate is null or a.createdAt >= :fromDate)
              and (:toDate is null or a.createdAt < :toDate)
            order by a.createdAt desc
            """)
    Page<PayOsPaymentAuditLog> search(@Param("keyword") String keyword,
                                      @Param("orderCode") Long orderCode,
                                      @Param("eventType") String eventType,
                                      @Param("fromDate") LocalDateTime fromDate,
                                      @Param("toDate") LocalDateTime toDate,
                                      Pageable pageable);
}
