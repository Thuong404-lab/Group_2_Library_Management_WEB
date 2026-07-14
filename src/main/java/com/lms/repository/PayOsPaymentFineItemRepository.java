package com.lms.repository;

import com.lms.entity.PayOsPaymentFineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayOsPaymentFineItemRepository extends JpaRepository<PayOsPaymentFineItem, Long> {
    List<PayOsPaymentFineItem> findByPaymentPaymentIdOrderByFineTransactionTransactionId(Long paymentId);

    List<PayOsPaymentFineItem> findByPaymentPaymentIdIn(List<Long> paymentIds);
}
