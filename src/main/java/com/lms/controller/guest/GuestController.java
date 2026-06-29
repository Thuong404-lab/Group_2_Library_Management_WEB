package com.lms.controller.guest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.lms.service.BookService;
import com.lms.service.MemberFavoriteService;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import com.lms.entity.Book;

/**
 * GuestController - Trang công khai cho Khách (Guest)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
public class GuestController {

    private final BookService bookService;
    private final MemberFavoriteService memberFavoriteService;

    public GuestController(BookService bookService,
                           MemberFavoriteService memberFavoriteService) {
        this.bookService = bookService;
        this.memberFavoriteService = memberFavoriteService;
    }

    // Trang chủ
    @GetMapping("/")
    public String homePage(Model model, Principal principal) {
        List<Book> books = bookService.getRecentBooks(6);
        model.addAttribute("books", books);
        addFavoriteBookIds(model, principal);
        return "index";
    }

    // UC-1: Search Books - Tìm kiếm sách (Guest có thể dùng)
    @GetMapping("/books/search")
    public String searchBooks(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Integer categoryId,
                              @RequestParam(required = false) Integer genreId,
                              Model model) {
        // TODO: Implement - Gọi BookService.search(keyword, categoryId, genreId)
        // TODO: Hỗ trợ phân trang (Pageable)
        return "guest/search";
    }

    // UC-3: View Book List - Xem danh sách sách
    @GetMapping("/books")
    public String viewBookList(@RequestParam(defaultValue = "0") int page,
                               Model model,
                               Principal principal) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 12, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "bookId"));
        org.springframework.data.domain.Page<Book> bookPage = bookService.findAllBooks(pageable);
        model.addAttribute("bookPage", bookPage);
        addFavoriteBookIds(model, principal);
        return "guest/books";
    }

    // Giới thiệu thư viện
    @GetMapping("/about")
    public String aboutPage() {
        return "guest/about";
    }

    // UC-3: View Book Detail - Xem chi tiết một quyển sách
    @GetMapping("/books/{id}")
    public String viewBookDetail(@PathVariable Integer id, Model model, Principal principal) {
        Book book = bookService.findBookById(id);
        model.addAttribute("book", book);
        addFavoriteBookIds(model, principal);
        return "guest/book-detail";
    }

    private void addFavoriteBookIds(Model model, Principal principal) {
        if (principal == null) {
            model.addAttribute("favoriteBookIds", Collections.emptySet());
            return;
        }

        try {
            model.addAttribute("favoriteBookIds", memberFavoriteService.getMyFavoriteBookIds(principal.getName()));
        } catch (RuntimeException e) {
            model.addAttribute("favoriteBookIds", Collections.emptySet());
        }
    }
}
