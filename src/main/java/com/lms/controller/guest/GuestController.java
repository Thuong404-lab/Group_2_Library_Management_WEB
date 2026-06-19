package com.lms.controller.guest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * GuestController - Trang công khai cho Khách (Guest)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
public class GuestController {

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
                              Model model) {
        // TODO: Implement - Gọi BookService.search(keyword, categoryId, genreId)
        // TODO: Hỗ trợ phân trang (Pageable)
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
