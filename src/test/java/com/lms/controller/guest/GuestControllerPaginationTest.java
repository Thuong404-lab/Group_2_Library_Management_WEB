package com.lms.controller.guest;

import com.lms.repository.BookItemRepository;
import com.lms.repository.GenreRepository;
import com.lms.service.BookService;
import com.lms.service.CartService;
import com.lms.service.MemberFavoriteService;
import com.lms.service.MemberReviewService;
import com.lms.service.MembershipService;
import com.lms.service.SystemService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestControllerPaginationTest {

    @Test
    void negativeBookPageIsClampedBeforeBuildingPageRequest() {
        BookService bookService = mock(BookService.class);
        GuestController controller = controllerWithEmptyBookPage(bookService);

        String view = controller.viewBookList(null, null, null, "newest", -1,
                new ExtendedModelMap(), null);

        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).findAllBooks(pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals("guest/books", view);
    }

    @Test
    void excessiveBookPageIsCapped() {
        BookService bookService = mock(BookService.class);
        GuestController controller = controllerWithEmptyBookPage(bookService);

        controller.viewBookList(null, null, null, "newest", Integer.MAX_VALUE,
                new ExtendedModelMap(), null);

        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).findAllBooks(pageable.capture());
        assertEquals(10_000, pageable.getValue().getPageNumber());
    }

    private GuestController controllerWithEmptyBookPage(BookService bookService) {
        GenreRepository genreRepository = mock(GenreRepository.class);
        when(bookService.findAllBooks(any(Pageable.class))).thenReturn(Page.empty());
        when(genreRepository.findAll()).thenReturn(java.util.List.of());
        return new GuestController(
                bookService,
                mock(CartService.class),
                genreRepository,
                mock(MemberFavoriteService.class),
                mock(MemberReviewService.class),
                mock(MembershipService.class),
                mock(BookItemRepository.class),
                mock(SystemService.class));
    }
}
