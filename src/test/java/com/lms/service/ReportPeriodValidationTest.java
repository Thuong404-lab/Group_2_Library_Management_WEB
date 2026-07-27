package com.lms.service;

import com.lms.exception.ValidationException;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import com.lms.service.impl.ReportServiceImpl;
import com.lms.util.ReportPeriodPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ReportPeriodValidationTest {

    private ReportService reportService;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(
                mock(BorrowRepository.class),
                mock(BorrowDetailRepository.class),
                mock(TransactionRepository.class),
                mock(MemberRepository.class),
                mock(BookRepository.class),
                mock(BookItemRepository.class));
        ReflectionTestUtils.setField(
                reportService, "messages", mock(LocalizedMessageService.class));
        today = LocalDate.now(ReportPeriodPolicy.LIBRARY_ZONE);
    }

    @Test
    void rejectsWhenOnlyOneBoundaryIsProvided() {
        assertThrows(ValidationException.class,
                () -> reportService.getLibrarianRevenueReport(null, today));
        assertThrows(ValidationException.class,
                () -> reportService.getLibrarianRevenueReport(today, null));
    }

    @Test
    void rejectsFutureDates() {
        assertThrows(ValidationException.class,
                () -> reportService.getLibrarianRevenueReport(today, today.plusDays(1)));
    }

    @Test
    void rejectsReversedDateRange() {
        assertThrows(ValidationException.class,
                () -> reportService.getLibrarianRevenueReport(today, today.minusDays(1)));
    }

    @Test
    void rejectsDateRangeLongerThanBusinessLimit() {
        assertThrows(ValidationException.class,
                () -> reportService.getLibrarianRevenueReport(
                        today.minusDays(ReportPeriodPolicy.MAX_RANGE_DAYS + 1), today));
    }
}
