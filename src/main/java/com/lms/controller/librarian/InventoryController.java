package com.lms.controller.librarian;

import com.lms.service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * InventoryController - Quản lý Kho Sách & Danh mục
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Controller
@RequestMapping("/librarian/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String listBooks() {
        return "redirect:/librarian/dashboard?section=books";
    }

    @GetMapping("/add")
    public String showAddBookForm() {
        return "redirect:/librarian/dashboard?section=books";
    }

    @PostMapping("/add")
    public String addNewBook(@RequestParam String title,
                             @RequestParam String isbn,
                             @RequestParam Integer genreId,
                             @RequestParam(defaultValue = "1") Integer quantity,
                             RedirectAttributes redirectAttributes) {
        try {
            inventoryService.addNewBook(title, isbn, genreId, quantity);
            redirectAttributes.addFlashAttribute("success", "Thêm sách mới thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books";
    }

    @PostMapping("/edit/{id}")
    public String updateBook(@PathVariable Integer id,
                             @RequestParam String title,
                             @RequestParam String isbn,
                             @RequestParam Integer genreId,
                             @RequestParam String status,
                             RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateBook(id, title, isbn, genreId, status);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sách thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books";
    }

    @PostMapping("/delete/{id}")
    public String removeBook(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.removeBook(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sách thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books";
    }

    @PostMapping("/status/{id}")
    public String updateBookStatus(@PathVariable Integer id,
                                   @RequestParam String status,
                                   RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateBookStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái sách thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books";
    }

    @GetMapping("/categories")
    public String manageCategories() {
        return "redirect:/librarian/dashboard?section=books";
    }

    @PostMapping("/categories/add")
    public String addCategory(@RequestParam String type,
                              @RequestParam(required = false) Integer categoryId,
                              @RequestParam String name,
                              RedirectAttributes redirectAttributes) {
        try {
            if ("genre".equals(type)) {
                inventoryService.addGenre(categoryId, name);
                redirectAttributes.addFlashAttribute("success", "Thêm thể loại thành công.");
            } else {
                inventoryService.addCategory(name);
                redirectAttributes.addFlashAttribute("success", "Thêm danh mục thành công.");
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books";
    }

    @GetMapping("/audit")
    public String showInventoryAudit() {
        return "redirect:/librarian/dashboard?section=books";
    }

    @PostMapping("/audit")
    public String processInventoryAudit(RedirectAttributes redirectAttributes) {
        try {
            var summary = inventoryService.performInventoryAudit();
            redirectAttributes.addFlashAttribute("success",
                    String.format("Kiểm kê hoàn tất: Available=%d, Borrowed=%d, Lost=%d, Damaged=%d, Disposed=%d.",
                            summary.getOrDefault("Available", 0L),
                            summary.getOrDefault("Borrowed", 0L),
                            summary.getOrDefault("Lost", 0L),
                            summary.getOrDefault("Damaged", 0L),
                            summary.getOrDefault("Disposed", 0L)));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books";
    }
}
