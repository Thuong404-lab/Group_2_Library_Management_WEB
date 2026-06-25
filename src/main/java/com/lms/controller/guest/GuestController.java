package com.lms.controller.guest;

import com.lms.entity.Book;
import com.lms.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GuestController - Trang công khai cho Khách (Guest)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
public class GuestController {

    private final BookService bookService;

    public GuestController(BookService bookService) {
        this.bookService = bookService;
    }

    // Trang chủ
    @GetMapping("/")
    public String homePage(Model model) {
        // TODO: Implement - Lấy danh sách sách mới nhất, truyền vào model

        return "index";
    }

    // UC-1: Search Books - Tìm kiếm sách (Guest có thể dùng)
    @GetMapping("/books/search")
    public String searchBooks(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) Integer categoryId,
                              @RequestParam(required = false) Integer genreId,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {

        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        Page<Book> bookPage = bookService.searchBooks(keyword, categoryId, genreId, page);

        model.addAttribute("bookPage", bookPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("genreId", genreId);
        return "guest/search";
    }

    // UC-3: View Book List - Xem danh sách sách
    @GetMapping("/books")
    public String viewBookList(@RequestParam(defaultValue = "0") int page,
                               Model model) {
        // TODO: Implement - Gọi BookService.findAll(Pageable)
        // TODO: Truyền danh sách sách + thông tin phân trang vào model
        return "guest/books";
    }

    // UC-3: View Book Detail - Xem chi tiết một quyển sách
    @GetMapping("/books/{id}")
    public String viewBookDetail(@PathVariable Integer id, Model model) {
        // TODO: Implement - Gọi BookService.findById(id)
        // TODO: Lấy thêm danh sách Review/Rating của sách
        return "guest/book-detail";
    }
}
