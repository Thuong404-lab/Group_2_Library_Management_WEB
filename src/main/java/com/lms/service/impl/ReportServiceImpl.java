package com.lms.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.lms.dto.response.LibrarianRevenueReportData;
import com.lms.dto.response.ReportExport;
import com.lms.dto.response.ReportMetric;
import com.lms.dto.response.ReportViewData;
import com.lms.exception.ValidationException;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.ReportService;
import com.lms.service.LocalizedMessageService;
import com.lms.util.FinancialTransactionPolicy;
import com.lms.util.ReportPeriodPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ReportService - Xu ly logic bao cao
 * Nguoi phu trach: Tran Nguyen Quoc Anh (CE191655)
 */
@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookItemRepository bookItemRepository;
    private final ReservationRepository reservationRepository;

    public ReportServiceImpl(BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            TransactionRepository transactionRepository,
            MemberRepository memberRepository,
            BookRepository bookRepository,
            BookItemRepository bookItemRepository,
            ReservationRepository reservationRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.transactionRepository = transactionRepository;
        this.memberRepository = memberRepository;
        this.bookRepository = bookRepository;
        this.bookItemRepository = bookItemRepository;
        this.reservationRepository = reservationRepository;
    }


    // UC-17.1: Tao bao cao tong hop
    @Override
    public ReportViewData getAdminConsoleReport(LocalDate fromDate, LocalDate toDate) {
        LocalDate[] range = normalizeDateRange(fromDate, toDate);
        LocalDate normalizedFromDate = range[0];
        LocalDate normalizedToDate = range[1];

        LocalDateTime startDate = normalizedFromDate.atStartOfDay();
        LocalDateTime endDate = normalizedToDate.plusDays(1).atStartOfDay();
        BigDecimal totalRevenue = transactionRepository.sumRevenueByStatusAndTypesAndDateRange(
                FinancialTransactionPolicy.COMPLETED_STATUS,
                FinancialTransactionPolicy.REVENUE_TYPES,
                startDate,
                endDate);
        List<ReportMetric> transactionBreakdown = toRevenueTransactionBreakdown(startDate, endDate);
        long totalTransactions = transactionBreakdown.stream().mapToLong(ReportMetric::getCount).sum();
        BigDecimal totalRefunds = completedRefunds(startDate, endDate);
        BigDecimal averageTransaction = averageTransaction(totalRevenue, totalTransactions);

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
                totalRefunds,
                totalTransactions,
                averageTransaction,
                transactionBreakdown,
                toMonthlyRevenueStats(normalizedFromDate, normalizedToDate),
                toTopBooks(startDate, endDate),
                toTopMembers(startDate, endDate),
                toMonthlyBorrowStats(normalizedFromDate, normalizedToDate),
                borrowRepository.countByStatusIgnoreCase("Active"),
                reservationRepository.countByStatusIgnoreCase("PENDING"),
                reservationRepository.countByStatusIgnoreCase("DEPOSIT_PAID"),
                reservationRepository.countByStatusIgnoreCase("READY"));
    }

    @Override
    public ReportExport exportAdminReport(LocalDate fromDate, LocalDate toDate, String format) {
        ReportViewData report = getAdminConsoleReport(fromDate, toDate);
        String normalizedFormat = normalizeExportFormat(format);
        String baseName = "admin-report-" + report.getFromDate() + "-to-" + report.getToDate();

        if ("pdf".equals(normalizedFormat)) {
            return new ReportExport(baseName + ".pdf", "application/pdf", buildPdf(report));
        }

        return new ReportExport(baseName + ".csv", "text/csv; charset=UTF-8", buildCsv(report));
    }

    @Override
    @Transactional(readOnly = true)
    public LibrarianRevenueReportData getLibrarianRevenueReport(LocalDate fromDate, LocalDate toDate) {
        LocalDate[] range = normalizeDateRange(fromDate, toDate);
        LocalDate normalizedFromDate = range[0];
        LocalDate normalizedToDate = range[1];
        LocalDateTime startDate = normalizedFromDate.atStartOfDay();
        LocalDateTime endDate = normalizedToDate.plusDays(1).atStartOfDay();

        List<ReportMetric> transactionBreakdown = toRevenueTransactionBreakdown(startDate, endDate);
        long totalTransactions = transactionBreakdown.stream()
                .mapToLong(ReportMetric::getCount)
                .sum();
        BigDecimal totalRevenue = transactionRepository.sumRevenueByStatusAndTypesAndDateRange(
                FinancialTransactionPolicy.COMPLETED_STATUS,
                FinancialTransactionPolicy.REVENUE_TYPES,
                startDate,
                endDate);
        BigDecimal totalRefunds = completedRefunds(startDate, endDate);
        BigDecimal averageTransaction = averageTransaction(totalRevenue, totalTransactions);

        return new LibrarianRevenueReportData(
                normalizedFromDate,
                normalizedToDate,
                LocalDateTime.now(),
                totalRevenue,
                totalRefunds,
                totalTransactions,
                averageTransaction,
                transactionBreakdown,
                toMonthlyRevenueStats(normalizedFromDate, normalizedToDate),
                borrowRepository.countByStatusIgnoreCase("Active"),
                reservationRepository.countByStatusIgnoreCase("PENDING"),
                reservationRepository.countByStatusIgnoreCase("DEPOSIT_PAID"),
                reservationRepository.countByStatusIgnoreCase("READY"),
                borrowDetailRepository.countByStatusIgnoreCase("Overdue"),
                memberRepository.count());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportExport exportLibrarianRevenueReport(LocalDate fromDate, LocalDate toDate, String format) {
        String normalizedFormat = normalizeExportFormat(format);
        LibrarianRevenueReportData report = getLibrarianRevenueReport(fromDate, toDate);
        String baseName = "librarian-report-" + report.getFromDate() + "-to-" + report.getToDate();

        if ("pdf".equals(normalizedFormat)) {
            return new ReportExport(baseName + ".pdf", "application/pdf", buildLibrarianRevenuePdf(report));
        }

        return new ReportExport(baseName + ".csv", "text/csv; charset=UTF-8", buildLibrarianRevenueCsv(report));
    }

    private List<ReportMetric> toRevenueTransactionBreakdown(LocalDateTime startDate, LocalDateTime endDate) {
        List<ReportMetric> metrics = new ArrayList<>();
        for (Object[] row : transactionRepository.summarizeRevenueByTypeAndDateRange(
                FinancialTransactionPolicy.COMPLETED_STATUS,
                FinancialTransactionPolicy.REVENUE_TYPES,
                startDate,
                endDate)) {
            metrics.add(new ReportMetric(displayTransactionType(String.valueOf(row[0])),
                    toLong(row[1]), (BigDecimal) row[2]));
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

    private List<ReportMetric> toMonthlyRevenueStats(LocalDate fromDate, LocalDate toDate) {
        YearMonth startMonth = YearMonth.from(fromDate);
        YearMonth endMonth = YearMonth.from(toDate);
        Map<YearMonth, ReportMetric> metricsByMonth = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            metricsByMonth.put(cursor, new ReportMetric(cursor.format(formatter), 0, BigDecimal.ZERO));
            cursor = cursor.plusMonths(1);
        }

        for (Object[] row : transactionRepository.summarizeMonthlyRevenueByTypesAndDateRange(
                FinancialTransactionPolicy.COMPLETED_STATUS,
                FinancialTransactionPolicy.REVENUE_TYPES,
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay())) {
            YearMonth month = YearMonth.of(((Number) row[1]).intValue(), ((Number) row[0]).intValue());
            metricsByMonth.put(month, new ReportMetric(month.format(formatter), toLong(row[2]), (BigDecimal) row[3]));
        }

        return new ArrayList<>(metricsByMonth.values());
    }

    private byte[] buildCsv(ReportViewData report) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(escapeCsv(messages.get("admin.reports.exportTitle"))).append("\n");
        appendCsvRow(csv, messages.get("librarian.report.exportFrom"), formatDate(report.getFromDate()));
        appendCsvRow(csv, messages.get("librarian.report.exportTo"), formatDate(report.getToDate()));
        appendCsvRow(csv, messages.get("librarian.report.generatedAt"), formatDateTime(report.getGeneratedAt()));

        appendSectionHeading(csv, messages.get("librarian.report.currentActivity"));
        appendCsvRow(csv, messages.get("admin.dashboard.activeLoans"), report.getActiveBorrows());
        appendCsvRow(csv, messages.get("librarian.report.pendingReservations"), report.getPendingReservationRequests());
        appendCsvRow(csv, messages.get("librarian.report.readyReservations"), report.getReadyReservations());
        appendCsvRow(csv, messages.get("loan.status.overdue"), report.getOverdueItems());
        appendCsvRow(csv, messages.get("librarian.report.registeredMembers"), report.getTotalMembers());

        appendSectionHeading(csv, messages.get("admin.reports.periodActivity"));
        appendCsvRow(csv, messages.get("admin.reports.loans"), report.getTotalBorrows());
        appendCsvRow(csv, messages.get("admin.reports.borrowedCopies"), report.getTotalBorrowedItems());

        appendSectionHeading(csv, messages.get("admin.reports.inventoryStatus"));
        appendCsvRow(csv, messages.get("admin.reports.activeTitles"), report.getActiveBooks());
        appendCsvRow(csv, messages.get("admin.reports.availableCopies"), report.getAvailableItems());

        appendSectionHeading(csv, messages.get("librarian.report.revenueMetrics"));
        appendCsvRow(csv, messages.get("librarian.report.totalRevenue"), formatMoney(report.getTotalRevenue()));
        appendCsvRow(csv, messages.get("librarian.report.totalRefunds"), formatMoney(report.getTotalRefunds()));
        appendCsvRow(csv, messages.get("librarian.report.exportCompletedTransactions"), report.getTotalTransactions());
        appendCsvRow(csv, messages.get("librarian.report.averageTransaction"),
                formatMoney(report.getAverageTransaction()));

        appendCsvTableHeading(csv, messages.get("librarian.report.byType"));
        appendMetricSection(csv, messages.get("librarian.report.exportTransactionType"),
                messages.get("librarian.report.transactionCount"),
                messages.get("librarian.report.exportAmount"), report.getTransactionBreakdown(), true);
        appendCsvTableHeading(csv, messages.get("librarian.report.monthly"));
        appendMetricSection(csv, messages.get("librarian.report.month"),
                messages.get("librarian.report.transactionCount"),
                messages.get("librarian.report.exportAmount"), report.getMonthlyRevenueStats(), true);
        appendCsvTableHeading(csv, messages.get("admin.reports.monthlyLoans"));
        appendMetricSection(csv, messages.get("librarian.report.month"),
                messages.get("admin.reports.loans"), null, report.getMonthlyBorrowStats(), false);
        appendCsvTableHeading(csv, messages.get("admin.reports.topBooks"));
        appendMetricSection(csv, messages.get("admin.reports.topBooks"),
                messages.get("admin.reports.loans"), null, report.getTopBooks(), false);
        appendCsvTableHeading(csv, messages.get("admin.reports.topMembers"));
        appendMetricSection(csv, messages.get("admin.reports.topMembers"),
                messages.get("admin.reports.borrowedCopies"), null, report.getTopMembers(), false);
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildPdf(ReportViewData report) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);

        try (Document document = createPdfDocument(pdfDocument, PageSize.A4.rotate(),
                messages.get("admin.reports.exportTitle"), report.getFromDate(), report.getToDate(),
                report.getGeneratedAt())) {
            addSummaryPdfSection(document, messages.get("librarian.report.currentActivity"), List.of(
                    metric(messages.get("admin.dashboard.activeLoans"), report.getActiveBorrows()),
                    metric(messages.get("librarian.report.pendingReservations"), report.getPendingReservationRequests()),
                    metric(messages.get("librarian.report.readyReservations"), report.getReadyReservations()),
                    metric(messages.get("loan.status.overdue"), report.getOverdueItems()),
                    metric(messages.get("librarian.report.registeredMembers"), report.getTotalMembers())));
            addSummaryPdfSection(document, messages.get("admin.reports.periodActivity"), List.of(
                    metric(messages.get("admin.reports.loans"), report.getTotalBorrows()),
                    metric(messages.get("admin.reports.borrowedCopies"), report.getTotalBorrowedItems())));
            addSummaryPdfSection(document, messages.get("admin.reports.inventoryStatus"), List.of(
                    metric(messages.get("admin.reports.activeTitles"), report.getActiveBooks()),
                    metric(messages.get("admin.reports.availableCopies"), report.getAvailableItems())));
            addSummaryPdfSection(document, messages.get("librarian.report.revenueMetrics"), List.of(
                    metric(messages.get("librarian.report.totalRevenue"), formatMoney(report.getTotalRevenue())),
                    metric(messages.get("librarian.report.totalRefunds"), formatMoney(report.getTotalRefunds())),
                    metric(messages.get("librarian.report.exportCompletedTransactions"), report.getTotalTransactions()),
                    metric(messages.get("librarian.report.averageTransaction"),
                            formatMoney(report.getAverageTransaction()))));
            addMetricPdfSection(document, messages.get("librarian.report.byType"),
                    messages.get("librarian.report.transactionCount"), report.getTransactionBreakdown(), true);
            addMetricPdfSection(document, messages.get("librarian.report.monthly"),
                    messages.get("librarian.report.transactionCount"), report.getMonthlyRevenueStats(), true);
            addMetricPdfSection(document, messages.get("admin.reports.monthlyLoans"),
                    messages.get("admin.reports.loans"), report.getMonthlyBorrowStats(), false);
            addMetricPdfSection(document, messages.get("admin.reports.topBooks"),
                    messages.get("admin.reports.loans"), report.getTopBooks(), false);
            addMetricPdfSection(document, messages.get("admin.reports.topMembers"),
                    messages.get("admin.reports.borrowedCopies"), report.getTopMembers(), false);
        } catch (IOException exception) {
            throw new IllegalStateException(messages.get("backend.report.pdfFontUnavailable"), exception);
        }

        return outputStream.toByteArray();
    }

    private byte[] buildLibrarianRevenueCsv(LibrarianRevenueReportData report) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(escapeCsv(messages.get("librarian.report.exportTitle"))).append("\n");
        appendCsvRow(csv, messages.get("librarian.report.exportFrom"), formatDate(report.getFromDate()));
        appendCsvRow(csv, messages.get("librarian.report.exportTo"), formatDate(report.getToDate()));
        appendCsvRow(csv, messages.get("librarian.report.generatedAt"), formatDateTime(report.getGeneratedAt()));

        appendSectionHeading(csv, messages.get("librarian.report.currentActivity"));
        appendCsvRow(csv, messages.get("admin.dashboard.activeLoans"), report.getActiveBorrows());
        appendCsvRow(csv, messages.get("librarian.report.pendingReservations"), report.getPendingReservationRequests());
        appendCsvRow(csv, messages.get("librarian.report.readyReservations"), report.getReadyReservations());
        appendCsvRow(csv, messages.get("loan.status.overdue"), report.getOverdueItems());
        appendCsvRow(csv, messages.get("librarian.report.registeredMembers"), report.getTotalMembers());

        appendSectionHeading(csv, messages.get("librarian.report.revenueMetrics"));
        appendCsvRow(csv, messages.get("librarian.report.totalRevenue"), formatMoney(report.getTotalRevenue()));
        appendCsvRow(csv, messages.get("librarian.report.totalRefunds"), formatMoney(report.getTotalRefunds()));
        appendCsvRow(csv, messages.get("librarian.report.exportCompletedTransactions"), report.getTotalTransactions());
        appendCsvRow(csv, messages.get("librarian.report.averageTransaction"),
                formatMoney(report.getAverageTransaction()));
        appendCsvTableHeading(csv, messages.get("librarian.report.byType"));
        appendMetricSection(csv, messages.get("librarian.report.exportTransactionType"),
                messages.get("librarian.report.transactionCount"),
                messages.get("librarian.report.exportAmount"), report.getTransactionBreakdown(), true);
        appendCsvTableHeading(csv, messages.get("librarian.report.monthly"));
        appendMetricSection(csv, messages.get("librarian.report.month"),
                messages.get("librarian.report.transactionCount"),
                messages.get("librarian.report.exportAmount"), report.getMonthlyRevenueStats(), true);
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildLibrarianRevenuePdf(LibrarianRevenueReportData report) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);

        try (Document document = createPdfDocument(pdfDocument, PageSize.A4,
                messages.get("librarian.report.exportTitle"), report.getFromDate(), report.getToDate(),
                report.getGeneratedAt())) {
            addSummaryPdfSection(document, messages.get("librarian.report.currentActivity"), List.of(
                    metric(messages.get("admin.dashboard.activeLoans"), report.getActiveBorrows()),
                    metric(messages.get("librarian.report.pendingReservations"), report.getPendingReservationRequests()),
                    metric(messages.get("librarian.report.readyReservations"), report.getReadyReservations()),
                    metric(messages.get("loan.status.overdue"), report.getOverdueItems()),
                    metric(messages.get("librarian.report.registeredMembers"), report.getTotalMembers())));
            addSummaryPdfSection(document, messages.get("librarian.report.revenueMetrics"), List.of(
                    metric(messages.get("librarian.report.totalRevenue"), formatMoney(report.getTotalRevenue())),
                    metric(messages.get("librarian.report.totalRefunds"), formatMoney(report.getTotalRefunds())),
                    metric(messages.get("librarian.report.exportCompletedTransactions"), report.getTotalTransactions()),
                    metric(messages.get("librarian.report.averageTransaction"),
                            formatMoney(report.getAverageTransaction()))));
            addMetricPdfSection(document, messages.get("librarian.report.byType"),
                    messages.get("librarian.report.transactionCount"),
                    report.getTransactionBreakdown(), true);
            addMetricPdfSection(document, messages.get("librarian.report.monthly"),
                    messages.get("librarian.report.transactionCount"),
                    report.getMonthlyRevenueStats(), true);
        } catch (IOException exception) {
            throw new IllegalStateException(messages.get("backend.report.pdfFontUnavailable"), exception);
        }

        return outputStream.toByteArray();
    }

    private void appendMetricSection(StringBuilder csv,
            String labelHeader,
            String countHeader,
            String amountHeader,
            List<ReportMetric> metrics,
            boolean amountIsMoney) {
        csv.append("\n").append(escapeCsv(labelHeader)).append(",").append(escapeCsv(countHeader));
        if (amountHeader != null) {
            csv.append(",").append(escapeCsv(amountHeader + (amountIsMoney ? " (VND)" : "")));
        }
        csv.append("\n");
        if (metrics.isEmpty()) {
            csv.append(escapeCsv(messages.get("admin.reports.noData"))).append("\n");
            return;
        }
        for (ReportMetric metric : metrics) {
            csv.append(escapeCsv(metric.getLabel())).append(",").append(escapeCsv(String.valueOf(metric.getCount())));
            if (amountHeader != null) {
                Object amount = amountIsMoney ? formatMoney(metric.getAmount()) : metric.getAmount();
                csv.append(",").append(escapeCsv(String.valueOf(amount)));
            }
            csv.append("\n");
        }
    }

    private void appendCsvRow(StringBuilder csv, String label, Object value) {
        csv.append(escapeCsv(label)).append(",").append(escapeCsv(String.valueOf(value))).append("\n");
    }

    private void appendSectionHeading(StringBuilder csv, String heading) {
        csv.append("\n").append(escapeCsv(heading)).append("\n")
                .append(escapeCsv(messages.get("librarian.report.exportMetric"))).append(",")
                .append(escapeCsv(messages.get("librarian.report.exportValue"))).append("\n");
    }

    private void appendCsvTableHeading(StringBuilder csv, String heading) {
        csv.append("\n").append(escapeCsv(heading)).append("\n");
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private void addMetricPdfSection(Document document,
            String title,
            String valueHeader,
            List<ReportMetric> metrics,
            boolean includeAmount) {
        document.add(sectionTitle(title));
        Table table = includeAmount
                ? new Table(UnitValue.createPercentArray(new float[] { 3, 1, 1 })).useAllAvailableWidth()
                : new Table(UnitValue.createPercentArray(new float[] { 3, 1 })).useAllAvailableWidth();
        table.addHeaderCell(headerCell(messages.get("librarian.report.exportName")));
        table.addHeaderCell(headerCell(valueHeader));
        if (includeAmount) {
            table.addHeaderCell(headerCell(messages.get("librarian.report.exportAmount") + " (VND)"));
        }
        for (ReportMetric metric : metrics) {
            table.addCell(bodyCell(metric.getLabel(), false));
            table.addCell(bodyCell(String.valueOf(metric.getCount()), true));
            if (includeAmount) {
                table.addCell(bodyCell(formatMoney(metric.getAmount()), true));
            }
        }
        if (metrics.isEmpty()) {
            table.addCell(new Cell(1, includeAmount ? 3 : 2)
                    .setPadding(10).setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(pdfBorder(), .6f))
                    .add(new Paragraph(messages.get("admin.reports.noData"))));
        }
        document.add(table);
    }

    private BigDecimal completedRefunds(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.sumAbsoluteAmountByTypeAndStatusAndDateRange(
                FinancialTransactionPolicy.REFUND_TYPE,
                FinancialTransactionPolicy.COMPLETED_STATUS,
                startDate,
                endDate);
    }

    private BigDecimal averageTransaction(BigDecimal totalRevenue, long totalTransactions) {
        BigDecimal safeRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        return totalTransactions == 0
                ? BigDecimal.ZERO
                : safeRevenue.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);
    }

    private Document createPdfDocument(PdfDocument pdfDocument,
            PageSize pageSize,
            String title,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime generatedAt) throws IOException {
        pdfDocument.setDefaultPageSize(pageSize);
        Document document = new Document(pdfDocument);
        document.setMargins(28, 28, 28, 28);
        document.setFont(reportFont());

        Color brandDark = new DeviceRgb(83, 59, 47);
        Color brand = new DeviceRgb(150, 96, 43);
        Table heading = new Table(UnitValue.createPercentArray(new float[] { 3, 2 })).useAllAvailableWidth();
        heading.addCell(new Cell().setPadding(14).setBorder(null).setBackgroundColor(brandDark)
                .add(new Paragraph(title).setBold().setFontSize(18)
                        .setFontColor(new DeviceRgb(255, 255, 255))));
        heading.addCell(new Cell().setPadding(14).setBorder(null).setBackgroundColor(brand)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(messages.get("librarian.report.exportPeriod") + ": "
                        + formatDate(fromDate) + " - " + formatDate(toDate))
                        .setFontSize(9).setFontColor(new DeviceRgb(255, 255, 255)))
                .add(new Paragraph(messages.get("librarian.report.generatedAt") + ": "
                        + formatDateTime(generatedAt))
                        .setFontSize(8).setFontColor(new DeviceRgb(255, 255, 255))));
        document.add(heading);
        return document;
    }

    private void addSummaryPdfSection(Document document, String title, List<SummaryMetric> metrics) {
        document.add(sectionTitle(title));
        Table table = new Table(UnitValue.createPercentArray(new float[] { 3, 1 }))
                .useAllAvailableWidth();
        for (SummaryMetric metric : metrics) {
            table.addCell(bodyCell(metric.label(), false));
            table.addCell(bodyCell(metric.value(), true));
        }
        document.add(table);
    }

    private Paragraph sectionTitle(String title) {
        return new Paragraph(title)
                .setBold()
                .setFontSize(12)
                .setFontColor(new DeviceRgb(83, 50, 21))
                .setMarginTop(14)
                .setMarginBottom(6);
    }

    private Cell headerCell(String value) {
        return new Cell()
                .setPadding(7)
                .setBackgroundColor(new DeviceRgb(252, 238, 231))
                .setBorder(new SolidBorder(pdfBorder(), .6f))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(value).setBold().setFontSize(8).setMargin(0));
    }

    private Cell bodyCell(String value, boolean alignRight) {
        Cell cell = new Cell()
                .setPadding(7)
                .setBorder(new SolidBorder(pdfBorder(), .6f))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(value == null ? "" : value).setFontSize(8).setMargin(0));
        if (alignRight) {
            cell.setTextAlignment(TextAlignment.RIGHT);
        }
        return cell;
    }

    private Color pdfBorder() {
        return new DeviceRgb(225, 211, 197);
    }

    private PdfFont reportFont() throws IOException {
        try (var resource = ReportServiceImpl.class.getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            if (resource != null) {
                return PdfFontFactory.createFont(resource.readAllBytes(), PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
        for (String candidate : List.of(
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/tahoma.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
                "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf")) {
            if (Files.isRegularFile(Path.of(candidate))) {
                return PdfFontFactory.createFont(candidate, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
        throw new IOException(messages.get("backend.report.pdfFontUnavailable"));
    }

    private SummaryMetric metric(String label, Object value) {
        return new SummaryMetric(label, String.valueOf(value));
    }

    private String formatDate(LocalDate value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String formatMoney(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(LocaleContextHolder.getLocale());
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(value == null ? BigDecimal.ZERO : value) + " " + messages.get("currency.vnd");
    }

    private record SummaryMetric(String label, String value) {
    }

    private long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private LocalDate[] normalizeDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now(ReportPeriodPolicy.LIBRARY_ZONE);
        if ((fromDate == null) != (toDate == null)) {
            throw new ValidationException(messages.get("backend.report.dateRangeRequired"));
        }
        if ((fromDate != null && fromDate.isAfter(today))
                || (toDate != null && toDate.isAfter(today))) {
            throw new ValidationException(messages.get("backend.report.futureDate"));
        }
        LocalDate normalizedToDate = toDate == null ? today : toDate;
        LocalDate normalizedFromDate = fromDate == null
                ? normalizedToDate.minusDays(ReportPeriodPolicy.DEFAULT_LOOKBACK_DAYS)
                : fromDate;
        if (normalizedFromDate.isAfter(normalizedToDate)) {
            throw new ValidationException(messages.get("backend.report.invalidRange"));
        }
        if (ChronoUnit.DAYS.between(normalizedFromDate, normalizedToDate)
                > ReportPeriodPolicy.MAX_RANGE_DAYS) {
            throw new ValidationException(messages.get(
                    "backend.report.rangeTooLarge", ReportPeriodPolicy.MAX_RANGE_DAYS));
        }
        return new LocalDate[] { normalizedFromDate, normalizedToDate };
    }

    private String normalizeExportFormat(String format) {
        String normalized = format == null ? "csv" : format.trim().toLowerCase(Locale.ROOT);
        if (!"csv".equals(normalized) && !"pdf".equals(normalized)) {
            throw new ValidationException(messages.get("backend.report.invalidExportFormat"));
        }
        return normalized;
    }

    private String displayTransactionType(String transactionType) {
        if (transactionType == null) {
            return messages.get("transaction.type.other");
        }
        switch (transactionType) {
            case "TOP_UP":
                return messages.get("transaction.type.topUp");
            case "BORROW_FEE":
                return messages.get("transaction.type.borrowFee");
            case "RENEWAL_FEE":
                return messages.get("transaction.type.renewalFee");
            case "DEPOSIT":
                return messages.get("transaction.type.deposit");
            case "FINE":
                return messages.get("transaction.type.fine");
            case "DAMAGE_FEE":
                return messages.get("transaction.type.damageFee");
            case "REFUND":
                return messages.get("transaction.type.refund");
            case "PAYMENT":
                return messages.get("transaction.type.payment");
            case "OVERDUE_FINE":
                return messages.get("transaction.type.overdueFine");
            case "FEE":
                return messages.get("transaction.type.fee");
            default:
                return transactionType;
        }
    }
}

