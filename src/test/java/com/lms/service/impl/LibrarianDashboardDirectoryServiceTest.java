package com.lms.service.impl;

import com.lms.dto.response.LibrarianListViewData;
import com.lms.entity.Staff;
import com.lms.entity.StaffAccount;
import com.lms.entity.User;
import com.lms.enums.UserStatus;
import com.lms.exception.ValidationException;
import com.lms.repository.BookAcquisitionRequestRepository;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.FeedbackRepository;
import com.lms.repository.GenreRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.StaffRepository;
import com.lms.service.LibrarianInteractionService;
import com.lms.service.LocalizedMessageService;
import com.lms.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibrarianDashboardDirectoryServiceTest {

    private final BorrowRepository borrowRepository = mock(BorrowRepository.class);
    private final BorrowDetailRepository borrowDetailRepository = mock(BorrowDetailRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final BookItemRepository bookItemRepository = mock(BookItemRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final BookAcquisitionRequestRepository acquisitionRepository = mock(BookAcquisitionRequestRepository.class);
    private final FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final GenreRepository genreRepository = mock(GenreRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final StaffRepository staffRepository = mock(StaffRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final LibrarianInteractionService interactionService = mock(LibrarianInteractionService.class);
    private final StaffAccountRepository staffAccountRepository = mock(StaffAccountRepository.class);

    private LibrarianDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LibrarianDashboardServiceImpl(
                borrowRepository,
                borrowDetailRepository,
                reservationRepository,
                bookItemRepository,
                bookRepository,
                acquisitionRepository,
                feedbackRepository,
                categoryRepository,
                genreRepository,
                memberRepository,
                staffRepository,
                storageService,
                interactionService,
                staffAccountRepository,
                LocalizedMessageService.fallback());
    }

    @Test
    void directoryUsesCanonicalAccountStatusAndReturnsPrivacySafeDto() {
        StaffAccount account = librarianAccount("librarian01", "Inactive");
        PageRequest pageRequest = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("id").ascending());
        when(staffAccountRepository.searchDirectory("Librarian", "Minh", "Inactive", pageRequest))
                .thenReturn(new PageImpl<>(List.of(account), pageRequest, 1));
        when(staffAccountRepository.countDirectoryByStatus("Librarian"))
                .thenReturn(List.<Object[]>of(new Object[]{"Active", 2L}, new Object[]{"Inactive", 1L}));

        LibrarianListViewData result = service.getLibrarianList(0, "  Minh  ", "inactive");

        assertThat(result.keyword()).isEqualTo("Minh");
        assertThat(result.selectedStatus()).isEqualTo("Inactive");
        assertThat(result.staffPage().getContent()).singleElement().satisfies(item -> {
            assertThat(item.username()).isEqualTo("librarian01");
            assertThat(item.fullName()).isEqualTo("Thủ thư Minh Anh");
            assertThat(item.status()).isEqualTo(UserStatus.Inactive);
        });
        assertThat(result.summaryCounts()).containsEntry("total", 3L)
                .containsEntry("active", 2L)
                .containsEntry("inactive", 1L)
                .containsEntry("blocked", 0L);
    }

    @Test
    void directoryRejectsInvalidStatusInsteadOfShowingAllAccounts() {
        assertThatThrownBy(() -> service.getLibrarianList(0, "", "unknown"))
                .isInstanceOf(ValidationException.class);

        verify(staffAccountRepository, never()).searchDirectory(any(), any(), any(), any());
    }

    @Test
    void directoryRejectsInvalidPageAndOversizedKeyword() {
        assertThatThrownBy(() -> service.getLibrarianList(-1, "", ""))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.getLibrarianList(0, "x".repeat(101), ""))
                .isInstanceOf(ValidationException.class);

        verify(staffAccountRepository, never()).searchDirectory(any(), any(), any(), any());
    }

    private StaffAccount librarianAccount(String username, String status) {
        User user = new User(2, "Thủ thư Minh Anh", "private@library.test", "0900000002", UserStatus.Active);
        Staff staff = new Staff(2, user, "Librarian");
        return new StaffAccount(2, staff, username, "hash", status);
    }
}
