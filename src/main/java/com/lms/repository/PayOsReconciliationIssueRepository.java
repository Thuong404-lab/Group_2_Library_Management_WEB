package com.lms.repository;

import com.lms.entity.PayOsReconciliationIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayOsReconciliationIssueRepository extends JpaRepository<PayOsReconciliationIssue, Long> {
    Optional<PayOsReconciliationIssue> findFirstByPaymentPaymentIdAndStatusOrderByLastAttemptAtDesc(
            Long paymentId, String status);

    @EntityGraph(attributePaths = {"payment", "payment.member", "payment.member.user"})
    Page<PayOsReconciliationIssue> findByStatusOrderByLastAttemptAtDesc(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"payment", "payment.member", "payment.member.user"})
    List<PayOsReconciliationIssue> findByPaymentPaymentIdOrderByLastAttemptAtDesc(Long paymentId);

    long countByStatus(String status);
}
