package com.lms.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.lms.dto.request.PaymentSearchCriteria;
import com.lms.dto.response.ReportExport;
import com.lms.entity.PayOsPayment;
import com.lms.entity.PayOsPaymentAuditLog;
import com.lms.entity.PayOsReconciliationIssue;
import com.lms.repository.PayOsPaymentAuditLogRepository;
import com.lms.repository.PayOsPaymentRepository;
import com.lms.repository.PayOsReconciliationIssueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminPaymentService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final PayOsPaymentRepository paymentRepository;
    private final PayOsPaymentAuditLogRepository auditRepository;
    private final PayOsReconciliationIssueRepository issueRepository;

    public AdminPaymentService(PayOsPaymentRepository paymentRepository,
                               PayOsPaymentAuditLogRepository auditRepository,
                               PayOsReconciliationIssueRepository issueRepository) {
        this.paymentRepository = paymentRepository;
        this.auditRepository = auditRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public Page<PayOsPayment> searchPayments(PaymentSearchCriteria criteria, int page) {
        return paymentRepository.search(criteria.normalizedKeyword(), criteria.orderCode(),
                criteria.normalizedStatus(), criteria.normalizedPurpose(), criteria.fromDateTime(),
                criteria.toDateTimeExclusive(), PageRequest.of(Math.max(page, 0), 15));
    }

    @Transactional(readOnly = true)
    public PayOsPayment getPayment(Long orderCode) {
        return paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch thanh toán."));
    }

    @Transactional(readOnly = true)
    public Page<PayOsPaymentAuditLog> getPaymentAudits(Long paymentId, int page) {
        return auditRepository.findByPaymentPaymentIdOrderByCreatedAtDesc(
                paymentId, PageRequest.of(Math.max(page, 0), 20));
    }

    @Transactional(readOnly = true)
    public Page<PayOsPaymentAuditLog> searchAudits(String keyword, String eventType,
                                                   LocalDate fromDate, LocalDate toDate, int page) {
        return auditRepository.search(normalize(keyword), parseLong(keyword), normalize(eventType),
                start(fromDate), endExclusive(toDate), PageRequest.of(Math.max(page, 0), 20));
    }

    @Transactional(readOnly = true)
    public List<PayOsReconciliationIssue> getPaymentIssues(Long paymentId) {
        return issueRepository.findByPaymentPaymentIdOrderByLastAttemptAtDesc(paymentId);
    }

    @Transactional(readOnly = true)
    public Page<PayOsReconciliationIssue> getOpenIssues(int page) {
        return issueRepository.findByStatusOrderByLastAttemptAtDesc(
                PayOsReconciliationIssue.OPEN, PageRequest.of(Math.max(page, 0), 10));
    }

    @Transactional(readOnly = true)
    public long countOpenIssues() { return issueRepository.countByStatus(PayOsReconciliationIssue.OPEN); }

    @Transactional(readOnly = true)
    public ReportExport exportPayments(PaymentSearchCriteria criteria, String format) {
        List<PayOsPayment> rows = paymentRepository.search(criteria.normalizedKeyword(), criteria.orderCode(),
                criteria.normalizedStatus(), criteria.normalizedPurpose(), criteria.fromDateTime(),
                criteria.toDateTimeExclusive(), Pageable.unpaged()).getContent();
        String extension = "pdf".equalsIgnoreCase(format) ? "pdf" : "csv";
        byte[] content = "pdf".equals(extension) ? paymentPdf(rows) : paymentCsv(rows);
        return new ReportExport("online-payments-" + LocalDate.now() + "." + extension,
                "pdf".equals(extension) ? "application/pdf" : "text/csv; charset=UTF-8", content);
    }

    @Transactional(readOnly = true)
    public ReportExport exportAudits(String keyword, String eventType, LocalDate fromDate,
                                     LocalDate toDate, String format) {
        List<PayOsPaymentAuditLog> rows = auditRepository.search(normalize(keyword), parseLong(keyword),
                normalize(eventType), start(fromDate), endExclusive(toDate), Pageable.unpaged()).getContent();
        String extension = "pdf".equalsIgnoreCase(format) ? "pdf" : "csv";
        byte[] content = "pdf".equals(extension) ? auditPdf(rows) : auditCsv(rows);
        return new ReportExport("payment-audit-logs-" + LocalDate.now() + "." + extension,
                "pdf".equals(extension) ? "application/pdf" : "text/csv; charset=UTF-8", content);
    }

    private byte[] paymentCsv(List<PayOsPayment> rows) {
        StringBuilder csv = new StringBuilder("\uFEFFOrder code,Member,Email,Purpose,Amount,Status,Bank reference,Created at,Paid at,Transaction ID\r\n");
        for (PayOsPayment p : rows) {
            csv.append(csv(p.getOrderCode())).append(',').append(csv(memberName(p))).append(',')
                    .append(csv(memberEmail(p))).append(',').append(csv(p.getPurpose())).append(',')
                    .append(csv(p.getAmount())).append(',').append(csv(p.getStatus())).append(',')
                    .append(csv(p.getBankReference())).append(',').append(csv(format(p.getCreatedAt()))).append(',')
                    .append(csv(format(p.getPaidAt()))).append(',')
                    .append(csv(p.getTransaction() == null ? null : p.getTransaction().getTransactionId())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] auditCsv(List<PayOsPaymentAuditLog> rows) {
        StringBuilder csv = new StringBuilder("\uFEFFTime,Order code,Member,Event,Source,Old status,New status,Successful,Actor,Message\r\n");
        for (PayOsPaymentAuditLog a : rows) {
            csv.append(csv(format(a.getCreatedAt()))).append(',').append(csv(a.getPayment().getOrderCode())).append(',')
                    .append(csv(memberName(a.getPayment()))).append(',').append(csv(a.getEventType())).append(',')
                    .append(csv(a.getSource())).append(',').append(csv(a.getOldStatus())).append(',')
                    .append(csv(a.getNewStatus())).append(',').append(csv(a.isSuccessful())).append(',')
                    .append(csv(a.getActor() == null ? "SYSTEM" : a.getActor().getFullName())).append(',')
                    .append(csv(a.getMessage())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] paymentPdf(List<PayOsPayment> rows) { return pdf("ONLINE PAYMENT REPORT",
            new String[]{"Order", "Member", "Purpose", "Amount", "Status", "Created"},
            rows.stream().map(p -> new String[]{String.valueOf(p.getOrderCode()), memberName(p), p.getPurpose(),
                    String.valueOf(p.getAmount()), p.getStatus(), format(p.getCreatedAt())}).toList()); }

    private byte[] auditPdf(List<PayOsPaymentAuditLog> rows) { return pdf("PAYMENT AUDIT LOGS",
            new String[]{"Time", "Order", "Event", "Source", "Status", "Result"},
            rows.stream().map(a -> new String[]{format(a.getCreatedAt()), String.valueOf(a.getPayment().getOrderCode()),
                    a.getEventType(), a.getSource(), nullSafe(a.getNewStatus()), a.isSuccessful() ? "OK" : "FAILED"}).toList()); }

    private byte[] pdf(String title, String[] headers, List<String[]> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(out); PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {
            document.add(new Paragraph(title).setBold().setFontSize(16));
            document.add(new Paragraph("Generated: " + format(LocalDateTime.now())));
            Table table = new Table(UnitValue.createPercentArray(headers.length)).useAllAvailableWidth();
            for (String header : headers) table.addHeaderCell(header);
            for (String[] row : rows) for (String value : row) table.addCell(nullSafe(value));
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo file PDF thanh toán.", e);
        }
    }

    private String memberName(PayOsPayment p) { return p.getMember() == null || p.getMember().getUser() == null ? "" : p.getMember().getUser().getFullName(); }
    private String memberEmail(PayOsPayment p) { return p.getMember() == null || p.getMember().getUser() == null ? "" : p.getMember().getUser().getEmail(); }
    private String format(LocalDateTime value) { return value == null ? "" : DATE_TIME.format(value); }
    private String normalize(String value) { return value == null ? "" : value.trim(); }
    private Long parseLong(String value) { try { return normalize(value).isEmpty() ? null : Long.valueOf(normalize(value)); } catch (NumberFormatException e) { return null; } }
    private LocalDateTime start(LocalDate date) { return date == null ? null : date.atStartOfDay(); }
    private LocalDateTime endExclusive(LocalDate date) { return date == null ? null : date.plusDays(1).atStartOfDay(); }
    private String nullSafe(String value) { return value == null ? "" : value; }
    private String csv(Object value) { String text = value == null ? "" : String.valueOf(value); return '"' + text.replace("\"", "\"\"") + '"'; }
}
