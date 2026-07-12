package com.lms.controller.admin;

import com.lms.config.CustomUserDetails;
import com.lms.service.FileUploadService;
import com.lms.service.InventoryService;
import com.lms.service.LibrarianDashboardService;
import com.lms.service.StorageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin copy of librarian book and shelf management. */
@Controller
@RequestMapping("/admin")
public class AdminBookController {

    private static final String BOOKS_REDIRECT = "redirect:/admin/books";

    private final LibrarianDashboardService dashboardService;
    private final InventoryService inventoryService;
    private final FileUploadService fileUploadService;
    private final StorageService storageService;

    public AdminBookController(LibrarianDashboardService dashboardService,
            InventoryService inventoryService,
            FileUploadService fileUploadService,
            StorageService storageService) {
        this.dashboardService = dashboardService;
        this.inventoryService = inventoryService;
        this.fileUploadService = fileUploadService;
        this.storageService = storageService;
    }

    @GetMapping("/books")
    public String viewBooks(@RequestParam(defaultValue = "0") int bookPage,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAllAttributes(dashboardService.getDashboardData(Math.max(0, bookPage), 0, 0));
        if (userDetails != null && userDetails.getUser() != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
        return "admin/books";
    }

    @PostMapping(value = "/inventory/add", consumes = "multipart/form-data")
    public String addBook(@RequestParam String title, @RequestParam String isbn,
            @RequestParam Integer genreId, @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String description,
            @RequestParam(name = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(required = false) Integer shelfId,
            @RequestParam(defaultValue = "Mới") String bookCondition,
            @RequestParam(required = false) String author,
            RedirectAttributes redirectAttributes) {
        try {
            String coverImageUrl = storeCover(coverImage);
            inventoryService.addNewBook(title, isbn, genreId, quantity, description, coverImageUrl, shelfId,
                    bookCondition, author);
            success(redirectAttributes, "Thêm sách mới thành công.");
        } catch (IllegalArgumentException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping(value = "/inventory/edit/{id}", consumes = "multipart/form-data")
    public String editBook(@PathVariable Integer id, @RequestParam String title,
            @RequestParam String isbn, @RequestParam Integer genreId, @RequestParam String status,
            @RequestParam(name = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(required = false) Integer shelfId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String author,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateBook(id, title, isbn, genreId, status, storeCover(coverImage), shelfId,
                    description, author);
            success(redirectAttributes, "Cập nhật sách thành công.");
        } catch (IllegalArgumentException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping("/inventory/delete/{id}")
    public String deleteBook(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.removeBook(id);
            success(redirectAttributes, "Xóa sách thành công.");
        } catch (IllegalArgumentException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping("/inventory/categories/add")
    public String addCategory(@RequestParam String type,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam String name, RedirectAttributes redirectAttributes) {
        try {
            if ("genre".equals(type)) {
                inventoryService.addGenre(categoryId, name);
                success(redirectAttributes, "Thêm thể loại thành công.");
            } else {
                inventoryService.addCategory(name);
                success(redirectAttributes, "Thêm danh mục thành công.");
            }
        } catch (IllegalArgumentException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping("/inventory/audit")
    public String audit(RedirectAttributes redirectAttributes) {
        try {
            var summary = inventoryService.performInventoryAudit();
            success(redirectAttributes, String.format(
                    "Kiểm kê hoàn tất: Available=%d, Borrowed=%d, Lost=%d, Damaged=%d, Disposed=%d.",
                    summary.getOrDefault("Available", 0L), summary.getOrDefault("Borrowed", 0L),
                    summary.getOrDefault("Lost", 0L), summary.getOrDefault("Damaged", 0L),
                    summary.getOrDefault("Disposed", 0L)));
        } catch (IllegalArgumentException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping("/storage/add")
    public String addShelf(@RequestParam String shelfName,
            @RequestParam(required = false) String location, RedirectAttributes redirectAttributes) {
        try {
            storageService.addStorageLocation(shelfName, location);
            success(redirectAttributes, "Thêm vị trí lưu trữ thành công.");
        } catch (IllegalArgumentException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping("/storage/update/{id}")
    public String updateShelf(@PathVariable Integer id, @RequestParam String shelfName,
            @RequestParam(required = false) String location, RedirectAttributes redirectAttributes) {
        try {
            storageService.updateStorageLocation(id, shelfName, location);
            success(redirectAttributes, "Cập nhật vị trí lưu trữ thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping("/storage/delete/{id}")
    public String deleteShelf(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            storageService.removeStorageLocation(id);
            success(redirectAttributes, "Xóa vị trí lưu trữ thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    private String storeCover(MultipartFile coverImage) {
        return coverImage == null || coverImage.isEmpty() ? null : fileUploadService.storeFile(coverImage);
    }

    private void success(RedirectAttributes attributes, String message) {
        attributes.addFlashAttribute("success", message);
    }

    private void error(RedirectAttributes attributes, RuntimeException exception) {
        attributes.addFlashAttribute("error", exception.getMessage());
    }
}
