package com.lms.service;

import com.lms.dto.request.PaymentSearchCriteria;
import com.lms.dto.response.ReportExport;
import com.lms.entity.Member;
import com.lms.entity.PayOsPayment;
import com.lms.entity.User;
import com.lms.repository.PayOsPaymentAuditLogRepository;
import com.lms.repository.PayOsPaymentRepository;
import com.lms.repository.PayOsReconciliationIssueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminPaymentServiceTest {
    @Test
    void exportsFilteredOnlinePaymentsAsUtf8Csv() {
        PayOsPaymentRepository paymentRepository = mock(PayOsPaymentRepository.class);
        AdminPaymentService service = new AdminPaymentService(paymentRepository,
                mock(PayOsPaymentAuditLogRepository.class), mock(PayOsReconciliationIssueRepository.class));
        PayOsPayment payment = payment();
        when(paymentRepository.search(anyString(), any(), anyString(), anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));

        ReportExport export = service.exportPayments(
                new PaymentSearchCriteria("Nguyễn", "PAID", "TOP_UP", LocalDate.now(), LocalDate.now()), "csv");

        String csv = new String(export.getContent(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(export.getFileName()).endsWith(".csv");
        assertThat(csv).contains("Order code", "987654321", "Nguyễn Văn A", "TOP_UP", "PAID");

        ReportExport pdf = service.exportPayments(
                new PaymentSearchCriteria("", "", "", null, null), "pdf");
        assertThat(pdf.getFileName()).endsWith(".pdf");
        assertThat(new String(pdf.getContent(), 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    private PayOsPayment payment() {
        User user = new User();
        user.setFullName("Nguyễn Văn A");
        user.setEmail("member@example.com");
        Member member = new Member();
        member.setMemberId(7);
        member.setUser(user);
        PayOsPayment payment = new PayOsPayment();
        payment.setPaymentId(1L);
        payment.setOrderCode(987654321L);
        payment.setMember(member);
        payment.setPurpose("TOP_UP");
        payment.setAmount(BigDecimal.valueOf(100_000));
        payment.setStatus("PAID");
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }
}
