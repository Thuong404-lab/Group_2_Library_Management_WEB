package com.lms.controller.guest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.lms.service.BookService;
import com.lms.repository.GenreRepository;
import java.util.List;
import com.lms.entity.Book;

/**
 * GuestController - Trang công khai cho Khách (Guest)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
public class GuestController {

    private final BookService bookService;
    private final GenreRepository genreRepository;

    public GuestController(BookService bookService, GenreRepository genreRepository) {
        this.bookService = bookService;
        this.genreRepository = genreRepository;
    }

    // Trang chủ
    @GetMapping("/")
    public String homePage(Model model) {
        List<Book> books = bookService.getRecentBooks(6);
        model.addAttribute("books", books);
        return "index";
    }

    // UC-1 & UC-3: Xem danh sách sách và Tìm kiếm
    @GetMapping("/books")
    public String viewBookList(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer genreId,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false, defaultValue = "newest") String sort,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        
        org.springframework.data.domain.Sort sorting;
        if ("title_asc".equals(sort)) sorting = org.springframework.data.domain.Sort.by("title").ascending();
        else if ("title_desc".equals(sort)) sorting = org.springframework.data.domain.Sort.by("title").descending();
        else if ("oldest".equals(sort)) sorting = org.springframework.data.domain.Sort.by("bookId").ascending();
        else sorting = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "bookId");

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 12, sorting);
        
        org.springframework.data.domain.Page<Book> bookPage;
        if ((keyword != null && !keyword.trim().isEmpty()) || genreId != null || (status != null && !status.trim().isEmpty())) {
            bookPage = bookService.searchBooks(keyword, genreId, status, pageable);
        } else {
            bookPage = bookService.findAllBooks(pageable);
        }
        
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("genres", genreRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedGenreId", genreId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sort", sort);
        
        return "guest/books";
    }

    // Giới thiệu thư viện
    @GetMapping("/about")
    public String aboutPage() {
        return "guest/about";
    }

    // UC-3: View Book Detail - Xem chi tiết một quyển sách
    @GetMapping("/books/{id}")
    public String viewBookDetail(@PathVariable Integer id, Model model) {
        Book book = bookService.findBookById(id);
        model.addAttribute("book", book);
        return "guest/book-detail";
    }
}
