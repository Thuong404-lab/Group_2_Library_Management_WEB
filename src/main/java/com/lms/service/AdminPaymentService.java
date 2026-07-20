package com.lms.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.lms.dto.request.PaymentSearchCriteria;
import com.lms.exception.DataProcessingException;
import com.lms.exception.ResourceNotFoundException;
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
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;

@Service
public class AdminPaymentService {
    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();
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
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.payment.notFound")));
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
    public long countOpenIssues() {
        return issueRepository.countByStatus(PayOsReconciliationIssue.OPEN);
    }

    @Transactional(readOnly = true)
    public ReportExport exportPayments(PaymentSearchCriteria criteria, String format) {
        List<PayOsPayment> rows = paymentRepository.search(criteria.normalizedKeyword(), criteria.orderCode(),
                criteria.normalizedStatus(), criteria.normalizedPurpose(), criteria.fromDateTime(),
                criteria.toDateTimeExclusive(), Pageable.unpaged()).getContent();
        String extension = "pdf".equalsIgnoreCase(format) ? "pdf" : "csv";
        byte[] content = "pdf".equals(extension) ? paymentPdf(rows, criteria) : paymentCsv(rows);
        return new ReportExport("online-payments-" + LocalDate.now() + "." + extension,
                "pdf".equals(extension) ? "application/pdf" : "text/csv; charset=UTF-8", content);
    }

    @Transactional(readOnly = true)
    public ReportExport exportAudits(String keyword, String eventType, LocalDate fromDate,
            LocalDate toDate, String format) {
        List<PayOsPaymentAuditLog> rows = auditRepository.search(normalize(keyword), parseLong(keyword),
                normalize(eventType), start(fromDate), endExclusive(toDate), Pageable.unpaged()).getContent();
        String extension = "pdf".equalsIgnoreCase(format) ? "pdf" : "csv";
        byte[] content = "pdf".equals(extension) ? auditPdf(rows, fromDate, toDate) : auditCsv(rows);
        return new ReportExport("payment-audit-logs-" + LocalDate.now() + "." + extension,
                "pdf".equals(extension) ? "application/pdf" : "text/csv; charset=UTF-8", content);
    }

    private byte[] paymentCsv(List<PayOsPayment> rows) {
        StringBuilder csv = new StringBuilder("\uFEFF")
                .append(csvRow(messages.get("report.payment.orderCode"), messages.get("report.payment.member"),
                        messages.get("report.payment.email"), messages.get("report.payment.purpose"),
                        messages.get("report.payment.amount"), messages.get("report.payment.status"),
                        messages.get("report.payment.bankReference"), messages.get("report.payment.createdAt"),
                        messages.get("report.payment.paidAt"), messages.get("report.payment.transactionId")));
        for (PayOsPayment p : rows) {
            csv.append(csvRow(p.getOrderCode(), memberName(p), memberEmail(p), localizedPurpose(p.getPurpose()),
                    p.getAmount(), localizedStatus(p.getStatus()), p.getBankReference(), format(p.getCreatedAt()),
                    format(p.getPaidAt()), p.getTransaction() == null ? null : p.getTransaction().getTransactionId()));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] auditCsv(List<PayOsPaymentAuditLog> rows) {
        StringBuilder csv = new StringBuilder("\uFEFF")
                .append(csvRow(messages.get("report.audit.time"), messages.get("report.payment.orderCode"),
                        messages.get("report.payment.member"), messages.get("report.audit.event"),
                        messages.get("report.audit.source"), messages.get("report.audit.oldStatus"),
                        messages.get("report.audit.newStatus"), messages.get("report.audit.result"),
                        messages.get("report.audit.actor"), messages.get("report.audit.content")));
        for (PayOsPaymentAuditLog a : rows) {
            csv.append(csvRow(format(a.getCreatedAt()), a.getPayment().getOrderCode(), memberName(a.getPayment()),
                    localizedEvent(a.getEventType()), localizedSource(a.getSource()), localizedStatus(a.getOldStatus()),
                    localizedStatus(a.getNewStatus()), localizedResult(a.isSuccessful()),
                    a.getActor() == null ? messages.get("payment.admin.source.system") : a.getActor().getFullName(),
                    localizedAuditContent(a.getEventType())));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] paymentPdf(List<PayOsPayment> rows, PaymentSearchCriteria criteria) {
        BigDecimal total = rows.stream().map(PayOsPayment::getAmount).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paid = rows.stream().filter(p -> "PAID".equals(p.getStatus())).count();
        long pending = rows.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
        List<SummaryItem> summary = List.of(
                new SummaryItem(messages.get("report.summary.transactions"), String.valueOf(rows.size())),
                new SummaryItem(messages.get("report.summary.totalAmount"), money(total)),
                new SummaryItem(messages.get("report.summary.paid"), String.valueOf(paid)),
                new SummaryItem(messages.get("report.summary.pending"), String.valueOf(pending)));
        List<ReportRow> reportRows = rows.stream().map(p -> new ReportRow(new String[] {
                String.valueOf(p.getOrderCode()), memberName(p), localizedPurpose(p.getPurpose()), money(p.getAmount()),
                localizedStatus(p.getStatus()), format(p.getCreatedAt()) }, p.getStatus())).toList();
        return pdf(messages.get("report.payment.title"), paymentScope(criteria), summary,
                new String[] { messages.get("report.payment.orderCode"), messages.get("report.payment.member"),
                        messages.get("report.payment.purpose"), messages.get("report.payment.amount"),
                        messages.get("report.payment.status"), messages.get("report.payment.createdAt") },
                new float[] { 1.3f, 2.2f, 2f, 1.5f, 1.5f, 1.8f }, reportRows, 3, 4);
    }

    private byte[] auditPdf(List<PayOsPaymentAuditLog> rows, LocalDate fromDate, LocalDate toDate) {
        long successful = rows.stream().filter(PayOsPaymentAuditLog::isSuccessful).count();
        List<SummaryItem> summary = List.of(
                new SummaryItem(messages.get("report.summary.auditEntries"), String.valueOf(rows.size())),
                new SummaryItem(messages.get("report.summary.successful"), String.valueOf(successful)),
                new SummaryItem(messages.get("report.summary.failed"), String.valueOf(rows.size() - successful)));
        List<ReportRow> reportRows = rows.stream().map(a -> new ReportRow(new String[] {
                format(a.getCreatedAt()), String.valueOf(a.getPayment().getOrderCode()),
                localizedEvent(a.getEventType()),
                localizedSource(a.getSource()), localizedStatus(a.getNewStatus()), localizedResult(a.isSuccessful()) },
                a.isSuccessful() ? "PAID" : "FAILED")).toList();
        return pdf(messages.get("report.audit.title"), dateScope(fromDate, toDate), summary,
                new String[] { messages.get("report.audit.time"), messages.get("report.payment.orderCode"),
                        messages.get("report.audit.event"), messages.get("report.audit.source"),
                        messages.get("report.audit.newStatus"), messages.get("report.audit.result") },
                new float[] { 1.8f, 1.4f, 2.4f, 1.8f, 1.5f, 1.4f }, reportRows, -1, 5);
    }

    private byte[] pdf(String title, String scope, List<SummaryItem> summary, String[] headers,
            float[] widths, List<ReportRow> rows, int amountColumn, int statusColumn) {
        Color brand = new DeviceRgb(148, 96, 42);
        Color brandDark = new DeviceRgb(91, 57, 31);
        Color cream = new DeviceRgb(248, 244, 238);
        Color border = new DeviceRgb(223, 211, 197);
        Color muted = new DeviceRgb(104, 93, 84);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                PdfWriter writer = new PdfWriter(out);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf, PageSize.A4.rotate())) {
            PdfFont font = reportFont();
            document.setFont(font).setMargins(28, 28, 28, 28);

            Table heading = new Table(UnitValue.createPercentArray(new float[] { 3.2f, 1.2f })).useAllAvailableWidth();
            heading.addCell(new Cell().setPadding(16).setBorder(null).setBackgroundColor(brandDark)
                    .add(new Paragraph(title).setBold().setFontSize(18).setFontColor(new DeviceRgb(255, 255, 255)))
                    .add(new Paragraph(scope).setFontSize(9).setFontColor(new DeviceRgb(238, 226, 214))));
            heading.addCell(new Cell().setPadding(16).setBorder(null).setBackgroundColor(brand)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph("KQPay").setBold().setFontSize(17).setFontColor(new DeviceRgb(255, 255, 255)))
                    .add(new Paragraph(messages.get("report.generatedAt", format(LocalDateTime.now())))
                            .setFontSize(8).setFontColor(new DeviceRgb(255, 255, 255))));
            document.add(heading);

            float[] summaryWidths = new float[summary.size()];
            Arrays.fill(summaryWidths, 1f);
            Table summaryTable = new Table(UnitValue.createPercentArray(summaryWidths)).useAllAvailableWidth()
                    .setMarginTop(14).setMarginBottom(16);
            for (SummaryItem item : summary) {
                summaryTable.addCell(new Cell().setPadding(10).setBackgroundColor(cream)
                        .setBorder(new SolidBorder(border, 0.7f))
                        .add(new Paragraph(item.label()).setFontSize(8).setFontColor(muted))
                        .add(new Paragraph(item.value()).setBold().setFontSize(12).setFontColor(brandDark)));
            }
            document.add(summaryTable);

            Table table = new Table(UnitValue.createPercentArray(widths)).useAllAvailableWidth();
            for (String header : headers) {
                table.addHeaderCell(new Cell().setPadding(8).setBackgroundColor(brand)
                        .setBorder(new SolidBorder(brandDark, 0.5f)).setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .add(new Paragraph(header).setBold().setFontSize(8)
                                .setFontColor(new DeviceRgb(255, 255, 255))));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                ReportRow row = rows.get(rowIndex);
                for (int column = 0; column < row.values().length; column++) {
                    Cell cell = new Cell().setPadding(7).setFontSize(8)
                            .setBorder(new SolidBorder(border, 0.45f))
                            .setVerticalAlignment(VerticalAlignment.MIDDLE);
                    if (rowIndex % 2 == 1)
                        cell.setBackgroundColor(cream);
                    if (column == amountColumn)
                        cell.setTextAlignment(TextAlignment.RIGHT).setBold();
                    if (column == statusColumn) {
                        cell.setBackgroundColor(statusBackground(row.tone())).setTextAlignment(TextAlignment.CENTER)
                                .setBold().setFontColor(statusText(row.tone()));
                    }
                    cell.add(new Paragraph(nullSafe(row.values()[column])).setMargin(0));
                    table.addCell(cell);
                }
            }
            if (rows.isEmpty()) {
                table.addCell(new Cell(1, headers.length).setPadding(18).setTextAlignment(TextAlignment.CENTER)
                        .setBorder(new SolidBorder(border, 0.5f)).setFontColor(muted)
                        .add(new Paragraph(messages.get("report.empty"))));
            }
            document.add(table);
            document.add(new Paragraph(messages.get("report.footer"))
                    .setFontSize(8).setFontColor(muted).setTextAlignment(TextAlignment.CENTER).setMarginTop(12));
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new DataProcessingException(messages.get("backend.payment.pdfFailed"), e);
        }
    }

    private PdfFont reportFont() throws Exception {
        for (String candidate : List.of("C:/Windows/Fonts/arial.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf")) {
            if (Files.isRegularFile(Path.of(candidate))) {
                return PdfFontFactory.createFont(candidate, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
        return PdfFontFactory.createFont(StandardFonts.HELVETICA);
    }

    private Color statusBackground(String status) {
        return switch (nullSafe(status)) {
            case "PAID" -> new DeviceRgb(224, 241, 225);
            case "PENDING" -> new DeviceRgb(255, 241, 194);
            default -> new DeviceRgb(250, 224, 224);
        };
    }

    private Color statusText(String status) {
        return switch (nullSafe(status)) {
            case "PAID" -> new DeviceRgb(40, 103, 52);
            case "PENDING" -> new DeviceRgb(130, 91, 0);
            default -> new DeviceRgb(155, 45, 45);
        };
    }

    private String memberName(PayOsPayment p) {
        return p.getMember() == null || p.getMember().getUser() == null ? "" : p.getMember().getUser().getFullName();
    }

    private String memberEmail(PayOsPayment p) {
        return p.getMember() == null || p.getMember().getUser() == null ? "" : p.getMember().getUser().getEmail();
    }

    private String localizedStatus(String value) {
        return localizedCode("payment.admin.status.", value);
    }

    private String localizedPurpose(String value) {
        return localizedCode("payment.admin.purpose.", value);
    }

    private String localizedEvent(String value) {
        return localizedCode("payment.admin.event.", value);
    }

    private String localizedSource(String value) {
        return localizedCode("payment.admin.source.", value);
    }

    private String localizedResult(boolean successful) {
        return messages.get(successful ? "payment.admin.result.success" : "payment.admin.result.failure");
    }

    private String localizedAuditContent(String eventType) {
        String normalized = normalize(eventType);
        if (normalized.isEmpty())
            return "";
        try {
            return messages.get("payment.admin.content." + normalized.toLowerCase(Locale.ROOT));
        } catch (NoSuchMessageException ignored) {
            return localizedEvent(normalized);
        }
    }

    private String localizedCode(String prefix, String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty())
            return "";
        try {
            return messages.get(prefix + normalized.toLowerCase(Locale.ROOT));
        } catch (NoSuchMessageException ignored) {
            String text = normalized.toLowerCase(Locale.ROOT).replace('_', ' ');
            return Character.toUpperCase(text.charAt(0)) + text.substring(1);
        }
    }

    private String money(BigDecimal value) {
        NumberFormat number = NumberFormat.getIntegerInstance(LocaleContextHolder.getLocale());
        return number.format(value == null ? BigDecimal.ZERO : value) + " " + messages.get("currency.vnd");
    }

    private String paymentScope(PaymentSearchCriteria criteria) {
        if (criteria == null)
            return messages.get("report.scope.all");
        return dateScope(criteria.fromDate(), criteria.toDate());
    }

    private String dateScope(LocalDate fromDate, LocalDate toDate) {
        DateTimeFormatter date = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (fromDate != null && toDate != null)
            return messages.get("report.scope.dateRange", date.format(fromDate), date.format(toDate));
        if (fromDate != null)
            return messages.get("report.scope.fromDate", date.format(fromDate));
        if (toDate != null)
            return messages.get("report.scope.toDate", date.format(toDate));
        return messages.get("report.scope.all");
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private Long parseLong(String value) {
        try {
            return normalize(value).isEmpty() ? null : Long.valueOf(normalize(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime start(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime endExclusive(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private String csvRow(Object... values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                row.append(',');
            row.append(csv(values[i]));
        }
        return row.append("\r\n").toString();
    }

    private record SummaryItem(String label, String value) {
    }

    private record ReportRow(String[] values, String tone) {
    }
}
