package com.lms.controller.librarian;
import com.lms.exception.ApplicationException;

import com.lms.service.FileUploadService;
import com.lms.service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * InventoryController - Quản lý Kho Sách & Danh mục
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Controller
@RequestMapping("/librarian/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final FileUploadService fileUploadService;

    public InventoryController(InventoryService inventoryService, FileUploadService fileUploadService) {
        this.inventoryService = inventoryService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping
    public String listBooks() {
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
    }

    @GetMapping("/add")
    public String showAddBookForm() {
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
    }

    @PostMapping(value = "/add", consumes = "multipart/form-data")
    public String addNewBook(@RequestParam String title,
            @RequestParam String isbn,
            @RequestParam Integer genreId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String description,
            @RequestParam(name = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(required = false) Integer shelfId,
            @RequestParam(defaultValue = "Mới") String bookCondition,
            @RequestParam String author,
            RedirectAttributes redirectAttributes) {
        try {
            String coverImageUrl = null;
            if (coverImage != null && !coverImage.isEmpty()) {
                coverImageUrl = fileUploadService.storeFile(coverImage);
            }
            inventoryService.addNewBook(title, isbn, genreId, quantity, description, coverImageUrl, shelfId,
                    bookCondition, author);
            redirectAttributes.addFlashAttribute("success", "Thêm sách mới thành công.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
    }

    @PostMapping(value = "/edit/{id}", consumes = "multipart/form-data")
    public String updateBook(@PathVariable Integer id,
            @RequestParam String title,
            @RequestParam String isbn,
            @RequestParam Integer genreId,
            @RequestParam String status,
            @RequestParam(name = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(required = false) Integer shelfId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String author,
            RedirectAttributes redirectAttributes) {
        try {
            String coverImageUrl = null;
            if (coverImage != null && !coverImage.isEmpty()) {
                coverImageUrl = fileUploadService.storeFile(coverImage);
            }
            inventoryService.updateBook(id, title, isbn, genreId, status, coverImageUrl, shelfId,
                    description, author);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sách thành công.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
    }

    @PostMapping("/delete/{id}")
    public String removeBook(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.removeBook(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sách thành công.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
    }

    @PostMapping("/status/{id}")
    public String updateBookStatus(@PathVariable Integer id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateBookStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái sách thành công.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
    }

    @GetMapping("/categories")
    public String manageCategories() {
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
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
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory&tab=categories";
    }

    @PostMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable Integer id,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateCategory(id, name);
            redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục thành công.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory&tab=categories";
    }

    @PostMapping("/genres/edit/{id}")
    public String editGenre(@PathVariable Integer id,
            @RequestParam String name,
            @RequestParam(required = false) Integer categoryId,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateGenre(id, name, categoryId);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thể loại thành công.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory&tab=categories";
    }

    @PostMapping("/genres/delete/{id}")
    public String deleteGenre(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.deleteGenre(id);
            redirectAttributes.addFlashAttribute("success", "Xóa thể loại thành công.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory&tab=categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Xóa danh mục thành công.");
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory&tab=categories";
    }

    @GetMapping("/audit")
    public String showInventoryAudit() {
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
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
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=inventory";
    }
}
