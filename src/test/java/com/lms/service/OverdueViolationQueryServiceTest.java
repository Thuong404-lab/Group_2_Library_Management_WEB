package com.lms.service;

import com.lms.dto.response.OverdueViolationView;
import com.lms.entity.Book;
import com.lms.entity.BookItem;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.SystemSetting;
import com.lms.entity.User;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OverdueViolationQueryServiceTest {

    @Test
    void returnsOnlyActiveOverdueLoansAndCalculatesCalendarDays() {
        BorrowDetailRepository repository = mock(BorrowDetailRepository.class);
        SystemSettingRepository settingRepository = mock(SystemSettingRepository.class);
        OverdueViolationQueryService service = new OverdueViolationQueryService(repository, settingRepository);

        SystemSetting fineSetting = new SystemSetting();
        fineSetting.setSettingValue("7000");
        when(settingRepository.findBySettingKeyIgnoreCase("Fine_Per_Day"))
                .thenReturn(Optional.of(fineSetting));

        BorrowDetail overdue = borrowDetail(
                11,
                "Borrowed",
                LocalDate.now().minusDays(3),
                false);
        BorrowDetail future = borrowDetail(
                12,
                "Borrowed",
                LocalDate.now().plusDays(1),
                false);
        BorrowDetail returned = borrowDetail(
                13,
                "Overdue",
                LocalDate.now().minusDays(5),
                true);
        BorrowDetail pending = borrowDetail(
                14,
                "Pending",
                LocalDate.now().minusDays(2),
                false);

        when(repository.findAllBorrowDetailsWithRelationships())
                .thenReturn(List.of(future, returned, overdue, pending));

        List<OverdueViolationView> result = service.getActiveOverdueViolations();

        assertEquals(1, result.size());
        assertEquals(11, result.get(0).borrowDetailId());
        assertEquals(3, result.get(0).overdueDays());
        assertEquals(0, result.get(0).finePerDay().compareTo(new java.math.BigDecimal("7000")));
        assertEquals(0, result.get(0).suggestedFineAmount().compareTo(new java.math.BigDecimal("21000")));
        assertEquals("Nguyễn Văn A", result.get(0).memberName());
        assertEquals("Sách kiểm thử", result.get(0).bookTitle());
    }

    private BorrowDetail borrowDetail(
            int detailId,
            String status,
            LocalDate dueDate,
            boolean returned) {
        User user = mock(User.class);
        when(user.getFullName()).thenReturn("Nguyễn Văn A");
        when(user.getEmail()).thenReturn("member@example.com");
        when(user.getPhone()).thenReturn("0900000000");

        Member member = mock(Member.class);
        when(member.getMemberId()).thenReturn(7);
        when(member.getUser()).thenReturn(user);

        Borrow borrow = mock(Borrow.class);
        when(borrow.getMember()).thenReturn(member);

        Book book = mock(Book.class);
        when(book.getTitle()).thenReturn("Sách kiểm thử");

        BookItem bookItem = mock(BookItem.class);
        when(bookItem.getBarcode()).thenReturn("BC-001");

        BorrowDetail detail = mock(BorrowDetail.class);
        when(detail.getBorrowDetailId()).thenReturn(detailId);
        when(detail.getBorrow()).thenReturn(borrow);
        when(detail.getBook()).thenReturn(book);
        when(detail.getBookItem()).thenReturn(bookItem);
        when(detail.getDueDate()).thenReturn(dueDate.atTime(23, 59));
        when(detail.getReturnDate()).thenReturn(returned ? LocalDate.now().atStartOfDay() : null);
        when(detail.getStatus()).thenReturn(status);
        return detail;
    }
}
