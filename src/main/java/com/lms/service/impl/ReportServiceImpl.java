package com.lms.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.lms.dto.response.ReportExport;
import com.lms.dto.response.ReportMetric;
import com.lms.dto.response.ReportViewData;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.ReportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ReportService - Xử lý Logic Báo cáo
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
@Service
public class ReportServiceImpl implements ReportService {
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookItemRepository bookItemRepository;

    public ReportServiceImpl(BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            TransactionRepository transactionRepository,
            MemberRepository memberRepository,
            BookRepository bookRepository,
            BookItemRepository bookItemRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.transactionRepository = transactionRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.bookItemRepository = bookItemRepository;
    }


    // UC-17.1: Tạo báo cáo tổng hợp
    @Override
    public ReportViewData getAdminConsoleReport(LocalDate fromDate, LocalDate toDate) {
        LocalDate normalizedToDate = toDate == null ? LocalDate.now() : toDate;
        LocalDate normalizedFromDate = fromDate == null ? normalizedToDate.minusDays(29) : fromDate;
        if (normalizedFromDate.isAfter(normalizedToDate)) {
            LocalDate swap = normalizedFromDate;
            normalizedFromDate = normalizedToDate;
            normalizedToDate = swap;
        }

        LocalDateTime startDate = normalizedFromDate.atStartOfDay();
        LocalDateTime endDate = normalizedToDate.plusDays(1).atStartOfDay();
        BigDecimal totalRevenue = transactionRepository.sumAmountByStatusAndDateRange("Completed", startDate, endDate);

        return new ReportViewData(
                normalizedFromDate,
                normalizedToDate,
                LocalDateTime.now(),
                borrowRepository.countByBorrowDateGreaterThanEqualAndBorrowDateLessThan(startDate, endDate),
                borrowDetailRepository.countBorrowedItemsByBorrowDateRange(startDate, endDate),
                borrowDetailRepository.countOnTimeReturnsByDateRange(startDate, endDate),
                borrowDetailRepository.countLateReturnsByDateRange(startDate, endDate),
                borrowDetailRepository.countByStatusIgnoreCase("Overdue"),
                memberRepository.count(),
                bookRepository.countByStatusIgnoreCase("Active"),
                bookItemRepository.countByStatusIgnoreCase("Available"),
                totalRevenue,
                toTransactionBreakdown(startDate, endDate),
                toTopBooks(startDate, endDate),
                toTopMembers(startDate, endDate),
                toMonthlyBorrowStats(normalizedFromDate, normalizedToDate));
    }

    @Override
    public ReportExport exportAdminReport(LocalDate fromDate, LocalDate toDate, String format) {
        ReportViewData report = getAdminConsoleReport(fromDate, toDate);
        String normalizedFormat = format == null ? "csv" : format.toLowerCase(Locale.ROOT);
        String baseName = "admin-report-" + report.getFromDate() + "-to-" + report.getToDate();

        if ("pdf".equals(normalizedFormat)) {
            return new ReportExport(baseName + ".pdf", "application/pdf", buildPdf(report));
        }

        return new ReportExport(baseName + ".csv", "text/csv; charset=UTF-8", buildCsv(report));
    }

    @Override
    public void generateReport() {
        // TODO: Implement
    }

    // UC-22.1: Báo cáo doanh thu
    @Override
    public void generateRevenueReport(String fromDate, String toDate) {
        // TODO: Implement - Tổng hợp từ Transactions
    }

    // UC-22.2: Xuất báo cáo
    @Override
    public void exportReport(String type, String format) {
        // TODO: Implement - PDF (iText) hoặc Excel (Apache POI)
    }
    private List<ReportMetric> toTransactionBreakdown(LocalDateTime startDate, LocalDateTime endDate) {
        List<ReportMetric> metrics = new ArrayList<>();
        for (Object[] row : transactionRepository
                .summarizeByTypeAndStatusAndDateRange("Completed", startDate, endDate)) {
            metrics.add(new ReportMetric(String.valueOf(row[0]), toLong(row[1]), (BigDecimal) row[2]));
        }
        return metrics;
    }

    private List<ReportMetric> toTopBooks(LocalDateTime startDate, LocalDateTime endDate) {
        List<ReportMetric> metrics = new ArrayList<>();
        for (Object[] row : borrowDetailRepository.findTopBorrowedBooks(startDate, endDate, PageRequest.of(0, 5))) {
            String isbn = row[1] == null || String.valueOf(row[1]).isBlank() ? "No ISBN" : String.valueOf(row[1]);
            metrics.add(new ReportMetric(row[0] + " (" + isbn + ")", toLong(row[2])));
        }
        return metrics;
    }

    private List<ReportMetric> toTopMembers(LocalDateTime startDate, LocalDateTime endDate) {
        List<ReportMetric> metrics = new ArrayList<>();
        for (Object[] row : borrowDetailRepository.findTopBorrowingMembers(startDate, endDate, PageRequest.of(0, 5))) {
            String email = row[1] == null || String.valueOf(row[1]).isBlank() ? "No email" : String.valueOf(row[1]);
            metrics.add(new ReportMetric(row[0] + " - " + email, toLong(row[2])));
        }
        return metrics;
    }

    private List<ReportMetric> toMonthlyBorrowStats(LocalDate fromDate, LocalDate toDate) {
        YearMonth startMonth = YearMonth.from(fromDate);
        YearMonth endMonth = YearMonth.from(toDate);
        Map<YearMonth, Long> countsByMonth = new LinkedHashMap<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            countsByMonth.put(cursor, 0L);
            cursor = cursor.plusMonths(1);
        }

        for (Object[] row : borrowRepository.countMonthlyBorrows(fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay())) {
            YearMonth month = YearMonth.of(((Number) row[1]).intValue(), ((Number) row[0]).intValue());
            countsByMonth.put(month, toLong(row[2]));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        List<ReportMetric> metrics = new ArrayList<>();
        countsByMonth.forEach((month, count) -> metrics.add(new ReportMetric(month.format(formatter), count)));
        return metrics;
    }

    private byte[] buildCsv(ReportViewData report) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Library Management Admin Report\n");
        csv.append("From,").append(report.getFromDate()).append("\n");
        csv.append("To,").append(report.getToDate()).append("\n\n");
        csv.append("Metric,Value\n");
        appendCsvRow(csv, "Borrow records", report.getTotalBorrows());
        appendCsvRow(csv, "Borrowed items", report.getTotalBorrowedItems());
        appendCsvRow(csv, "On-time returns", report.getOnTimeReturns());
        appendCsvRow(csv, "Late returns", report.getLateReturns());
        appendCsvRow(csv, "Current overdue items", report.getOverdueItems());
        appendCsvRow(csv, "Members", report.getTotalMembers());
        appendCsvRow(csv, "Active books", report.getActiveBooks());
        appendCsvRow(csv, "Available copies", report.getAvailableItems());
        appendCsvRow(csv, "Revenue", report.getTotalRevenue());
        appendMetricSection(csv, "Transaction type", "Transactions", "Amount", report.getTransactionBreakdown());
        appendMetricSection(csv, "Top book", "Borrows", null, report.getTopBooks());
        appendMetricSection(csv, "Top member", "Items", null, report.getTopMembers());
        appendMetricSection(csv, "Month", "Borrows", null, report.getMonthlyBorrowStats());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildPdf(ReportViewData report) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);

        try (Document document = new Document(pdfDocument)) {
            document.add(new Paragraph("Library Management Admin Report").setBold().setFontSize(18));
            document.add(new Paragraph("Period: " + report.getFromDate() + " to " + report.getToDate()));
            document.add(new Paragraph("Generated at: " + report.getGeneratedAt()));

            Table overview = new Table(UnitValue.createPercentArray(new float[] { 2, 1 }))
                    .useAllAvailableWidth();
            overview.addHeaderCell("Metric");
            overview.addHeaderCell("Value");
            addPdfRow(overview, "Borrow records", report.getTotalBorrows());
            addPdfRow(overview, "Borrowed items", report.getTotalBorrowedItems());
            addPdfRow(overview, "On-time returns", report.getOnTimeReturns());
            addPdfRow(overview, "Late returns", report.getLateReturns());
            addPdfRow(overview, "Current overdue items", report.getOverdueItems());
            addPdfRow(overview, "Members", report.getTotalMembers());
            addPdfRow(overview, "Active books", report.getActiveBooks());
            addPdfRow(overview, "Available copies", report.getAvailableItems());
            addPdfRow(overview, "Revenue", report.getTotalRevenue());
            document.add(overview);

            addMetricPdfSection(document, "Transaction breakdown", "Amount", report.getTransactionBreakdown(), true);
            addMetricPdfSection(document, "Top borrowed books", "Borrows", report.getTopBooks(), false);
            addMetricPdfSection(document, "Top borrowing members", "Items", report.getTopMembers(), false);
        }

        return outputStream.toByteArray();
    }

    private void appendMetricSection(StringBuilder csv,
            String labelHeader,
            String countHeader,
            String amountHeader,
            List<ReportMetric> metrics) {
        csv.append("\n").append(labelHeader).append(",").append(countHeader);
        if (amountHeader != null) {
            csv.append(",").append(amountHeader);
        }
        csv.append("\n");
        for (ReportMetric metric : metrics) {
            csv.append(escapeCsv(metric.getLabel())).append(",").append(metric.getCount());
            if (amountHeader != null) {
                csv.append(",").append(metric.getAmount());
            }
            csv.append("\n");
        }
    }

    private void appendCsvRow(StringBuilder csv, String label, Object value) {
        csv.append(escapeCsv(label)).append(",").append(value).append("\n");
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private void addPdfRow(Table table, String label, Object value) {
        table.addCell(label);
        table.addCell(String.valueOf(value));
    }

    private void addMetricPdfSection(Document document,
            String title,
            String valueHeader,
            List<ReportMetric> metrics,
            boolean includeAmount) {
        document.add(new Paragraph(title).setBold().setMarginTop(16));
        Table table = includeAmount
                ? new Table(UnitValue.createPercentArray(new float[] { 3, 1, 1 })).useAllAvailableWidth()
                : new Table(UnitValue.createPercentArray(new float[] { 3, 1 })).useAllAvailableWidth();
        table.addHeaderCell("Name");
        table.addHeaderCell(valueHeader);
        if (includeAmount) {
            table.addHeaderCell("Amount");
        }
        for (ReportMetric metric : metrics) {
            table.addCell(metric.getLabel());
            table.addCell(String.valueOf(metric.getCount()));
            if (includeAmount) {
                table.addCell(String.valueOf(metric.getAmount()));
            }
        }
        document.add(table);
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
