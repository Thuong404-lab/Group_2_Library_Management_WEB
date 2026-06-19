package com.lms.controller.librarian;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * InventoryController - Quản lý Kho Sách & Danh mục
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Controller
@RequestMapping("/librarian/inventory")
public class InventoryController {

    // UC-12: Hiển thị danh sách sách trong kho
    @GetMapping
    public String listBooks(@RequestParam(defaultValue = "0") int page, Model model) {
        // TODO: Implement - Gọi BookRepository.findAll(Pageable)
        // TODO: Hỗ trợ tìm kiếm và lọc theo Category, Genre
        return "admin/books";
    }

    // UC-12.3: Add New Books - Hiển thị form thêm sách
    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        // TODO: Implement - Truyền danh sách Category, Genre, Author vào model
        return "librarian/add-book";
    }

    // UC-12.3: Add New Books - Xử lý thêm sách mới
    @PostMapping("/add")
    public String addNewBook(Model model) {
        // TODO: Implement - Validate input
        // TODO: Tạo Book + BookItem (bản vật lý)
        // TODO: Upload ảnh bìa sách (nếu có) lên Cloudinary
        return "redirect:/librarian/inventory?added";
    }

    // UC-12.4: Update Book
    @GetMapping("/edit/{id}")
    public String showEditBookForm(@PathVariable Integer id, Model model) {
        // TODO: Implement - Lấy Book theo ID, truyền vào form
        return "librarian/edit-book";
    }

    @PostMapping("/edit/{id}")
    public String updateBook(@PathVariable Integer id, Model model) {
        // TODO: Implement - Validate và cập nhật Book
        return "redirect:/librarian/inventory?updated";
    }

    // UC-12.5: Remove Books
    @PostMapping("/delete/{id}")
    public String removeBook(@PathVariable Integer id, Model model) {
        // TODO: Implement - Soft delete hoặc chuyển trạng thái sang "Disposed"
        // TODO: Kiểm tra sách có đang được mượn không
        return "redirect:/librarian/inventory?deleted";
    }

    // UC-12.2: Update Book Status
    @PostMapping("/status/{id}")
    public String updateBookStatus(@PathVariable Integer id,
                                    @RequestParam String status, Model model) {
        // TODO: Implement - Cập nhật trạng thái BookItem (Available, Damaged, Lost)
        return "redirect:/librarian/inventory?statusUpdated";
    }

    // UC-12.6: Manage Categories & Genres
    @GetMapping("/categories")
    public String manageCategories(Model model) {
        // TODO: Implement - Hiển thị danh sách Category và Genre
        return "admin/categories";
    }

    @PostMapping("/categories/add")
    public String addCategory(Model model) {
        // TODO: Implement - Thêm Category hoặc Genre mới
        return "redirect:/librarian/inventory/categories?added";
    }

    // UC-12.1: Perform Periodic Inventory Audit
    @GetMapping("/audit")
    public String showInventoryAudit(Model model) {
        // TODO: Implement - Hiển thị form kiểm kê
        // TODO: So sánh số lượng sách thực tế vs hệ thống
        return "librarian/inventory-audit";
    }

    @PostMapping("/audit")
    public String processInventoryAudit(Model model) {
        // TODO: Implement - Ghi nhận kết quả kiểm kê
        // TODO: Cập nhật trạng thái sách bị thiếu/hư hỏng
        return "redirect:/librarian/inventory/audit?completed";
    }
}
