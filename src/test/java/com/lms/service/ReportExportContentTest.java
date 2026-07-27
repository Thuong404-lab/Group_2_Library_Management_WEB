package com.lms.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.lms.dto.response.ReportExport;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportExportContentTest {

    private final BorrowRepository borrowRepository = mock(BorrowRepository.class);
    private final BorrowDetailRepository borrowDetailRepository = mock(BorrowDetailRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final BookItemRepository bookItemRepository = mock(BookItemRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private ReportService reportService;
    private LocalDate fromDate;
    private LocalDate toDate;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        fromDate = LocalDate.of(2026, 7, 1);
        toDate = LocalDate.of(2026, 7, 28);
        reportService = new ReportServiceImpl(
                borrowRepository,
                borrowDetailRepository,
                transactionRepository,
                memberRepository,
                bookRepository,
                bookItemRepository,
                reservationRepository);
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("file:src/main/resources/messages");
        source.setDefaultEncoding("UTF-8");
        ReflectionTestUtils.setField(reportService, "messages",
                new LocalizedMessageService(source, null));

        when(transactionRepository.sumRevenueByStatusAndTypesAndDateRange(
                anyString(), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("200000"));
        when(transactionRepository.sumAbsoluteAmountByTypeAndStatusAndDateRange(
                anyString(), anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("50000"));
        when(transactionRepository.summarizeRevenueByTypeAndDateRange(
                anyString(), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[] { "BORROW_FEE", 2L, new BigDecimal("200000") }));
        when(transactionRepository.summarizeMonthlyRevenueByTypesAndDateRange(
                anyString(), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[] { 7, 2026, 2L, new BigDecimal("200000") }));

        when(borrowRepository.countByBorrowDateGreaterThanEqualAndBorrowDateLessThan(
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
        when(borrowRepository.countByStatusIgnoreCase("Active")).thenReturn(4L);
        when(borrowRepository.countMonthlyBorrows(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[] { 7, 2026, 3L }));

        when(borrowDetailRepository.countBorrowedItemsByBorrowDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5L);
        when(borrowDetailRepository.countOnTimeReturnsByDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);
        when(borrowDetailRepository.countLateReturnsByDateRange(
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1L);
        when(borrowDetailRepository.countByStatusIgnoreCase("Overdue")).thenReturn(1L);
        when(borrowDetailRepository.findTopBorrowedBooks(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[] { "Dế Mèn phiêu lưu ký", "9780000000001", 3L }));
        when(borrowDetailRepository.findTopBorrowingMembers(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[] { "Nguyễn An", "an@example.com", 5L }));

        when(memberRepository.count()).thenReturn(25L);
        when(bookRepository.countByStatusIgnoreCase("Active")).thenReturn(40L);
        when(bookItemRepository.countByStatusIgnoreCase("Available")).thenReturn(30L);
        when(reservationRepository.countByStatusIgnoreCase("PENDING")).thenReturn(2L);
        when(reservationRepository.countByStatusIgnoreCase("DEPOSIT_PAID")).thenReturn(1L);
        when(reservationRepository.countByStatusIgnoreCase("READY")).thenReturn(3L);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void librarianCsvContainsOperationalAndRevenueInformationInVietnamese() {
        ReportExport export = reportService.exportLibrarianRevenueReport(fromDate, toDate, "csv");
        String csv = new String(export.getContent(), StandardCharsets.UTF_8);

        assertAll(
                () -> assertEquals("text/csv; charset=UTF-8", export.getContentType()),
                () -> assertTrue(csv.startsWith("\uFEFF")),
                () -> assertTrue(csv.contains("Báo cáo và thống kê thủ thư")),
                () -> assertTrue(csv.contains("Trạng thái vận hành hiện tại")),
                () -> assertTrue(csv.contains("Đặt trước chờ duyệt")),
                () -> assertFalse(csv.contains("Đã thanh toán cọc")),
                () -> assertFalse(csv.contains("Trả đúng hạn")),
                () -> assertFalse(csv.contains("Trả trễ hạn")),
                () -> assertTrue(csv.contains("Dòng tiền đã hoàn")),
                () -> assertTrue(csv.contains("Doanh thu theo tháng")),
                () -> assertTrue(csv.contains("200.000")));
    }

    @Test
    void adminCsvContainsLibrarianViewAndAdministrativeInformation() {
        ReportExport export = reportService.exportAdminReport(fromDate, toDate, "csv");
        String csv = new String(export.getContent(), StandardCharsets.UTF_8);

        assertAll(
                () -> assertTrue(csv.contains("Báo cáo quản trị thư viện")),
                () -> assertTrue(csv.contains("Trạng thái vận hành hiện tại")),
                () -> assertTrue(csv.contains("Dòng tiền đã hoàn")),
                () -> assertTrue(csv.contains("Hoạt động trong kỳ đã chọn")),
                () -> assertFalse(csv.contains("Đã thanh toán cọc")),
                () -> assertFalse(csv.contains("Trả đúng hạn")),
                () -> assertFalse(csv.contains("Trả trễ hạn")),
                () -> assertTrue(csv.contains("Dế Mèn phiêu lưu ký")),
                () -> assertTrue(csv.contains("Nguyễn An")),
                () -> assertTrue(csv.contains("Doanh thu theo tháng")));
    }

    @Test
    void librarianPdfEmbedsVietnameseTextAndCanBeRead() throws Exception {
        ReportExport export = reportService.exportLibrarianRevenueReport(fromDate, toDate, "pdf");
        String text = extractPdfText(export.getContent());

        assertAll(
                () -> assertEquals("application/pdf", export.getContentType()),
                () -> assertTrue(new String(export.getContent(), 0, 4, StandardCharsets.US_ASCII).equals("%PDF")),
                () -> assertTrue(text.contains("Báo cáo và thống kê thủ thư")),
                () -> assertTrue(text.contains("Trạng thái vận hành hiện tại")),
                () -> assertFalse(text.contains("Đã thanh toán cọc")),
                () -> assertFalse(text.contains("Trả đúng hạn")),
                () -> assertFalse(text.contains("Trả trễ hạn")),
                () -> assertTrue(text.contains("Doanh thu theo loại giao dịch")));
    }

    @Test
    void adminPdfContainsCompleteAdministrativeReportAndCanBeRead() throws Exception {
        ReportExport export = reportService.exportAdminReport(fromDate, toDate, "pdf");
        String text = extractPdfText(export.getContent());

        assertAll(
                () -> assertTrue(text.contains("Báo cáo quản trị thư viện")),
                () -> assertTrue(text.contains("Trạng thái vận hành hiện tại")),
                () -> assertTrue(text.contains("Hoạt động trong kỳ đã chọn")),
                () -> assertFalse(text.contains("Đã thanh toán cọc")),
                () -> assertFalse(text.contains("Trả đúng hạn")),
                () -> assertFalse(text.contains("Trả trễ hạn")),
                () -> assertTrue(text.contains("Doanh thu theo tháng")),
                () -> assertTrue(text.contains("Dế Mèn phiêu lưu ký")),
                () -> assertTrue(text.contains("Nguyễn An")));
    }

    private String extractPdfText(byte[] content) throws Exception {
        StringBuilder text = new StringBuilder();
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(content));
                PdfDocument document = new PdfDocument(reader)) {
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(document.getPage(page))).append('\n');
            }
        }
        return text.toString();
    }
}
