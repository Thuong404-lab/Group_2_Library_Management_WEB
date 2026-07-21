package com.lms.controller.admin;
import com.lms.exception.ApplicationException;

import com.lms.config.CustomUserDetails;
import com.lms.controller.LocalizedControllerSupport;
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
public class AdminBookController extends LocalizedControllerSupport {

    private static final String BOOKS_REDIRECT = "redirect:/admin/books";
    private static final String CATEGORIES_REDIRECT = BOOKS_REDIRECT + "?subsection=inventory&tab=categories";
    private static final String AUDIT_REDIRECT = BOOKS_REDIRECT + "?subsection=inventory&tab=audit";
    private static final String STORAGE_REDIRECT = BOOKS_REDIRECT + "?subsection=storage";

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
            @RequestParam(defaultValue = "0") int shelfPage,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAllAttributes(dashboardService.getDashboardData(
                Math.max(0, bookPage), Math.max(0, shelfPage), 0, 0, keyword));
        model.addAttribute("keyword", keyword);
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
            success(redirectAttributes, message("backend.inventory.bookAdded"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping(value = "/inventory/edit/{id}", consumes = "multipart/form-data")
    public String editBook(@PathVariable Integer id, @RequestParam String title,
            @RequestParam String isbn, @RequestParam Integer genreId,
            @RequestParam(name = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(required = false) Integer shelfId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String author,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateBook(id, title, isbn, genreId, null, storeCover(coverImage), shelfId,
                    description, author);
            success(redirectAttributes, message("backend.inventory.bookUpdated"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return BOOKS_REDIRECT;
    }

    @PostMapping("/inventory/delete/{id}")
    public String deleteBook(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.removeBook(id);
            success(redirectAttributes, message("backend.inventory.bookDeleted"));
        } catch (ApplicationException ex) {
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
                success(redirectAttributes, message("backend.inventory.genreAdded"));
            } else {
                inventoryService.addCategory(name);
                success(redirectAttributes, message("backend.inventory.categoryAdded"));
            }
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return CATEGORIES_REDIRECT;
    }

    @PostMapping("/inventory/categories/edit/{id}")
    public String editCategory(@PathVariable Integer id, @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateCategory(id, name);
            success(redirectAttributes, message("backend.inventory.categoryUpdated"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return CATEGORIES_REDIRECT;
    }

    @PostMapping("/inventory/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.deleteCategory(id);
            success(redirectAttributes, message("backend.inventory.categoryDeleted"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return CATEGORIES_REDIRECT;
    }

    @PostMapping("/inventory/genres/edit/{id}")
    public String editGenre(@PathVariable Integer id, @RequestParam String name,
            @RequestParam(required = false) Integer categoryId, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateGenre(id, name, categoryId);
            success(redirectAttributes, message("backend.inventory.genreUpdated"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return CATEGORIES_REDIRECT;
    }

    @PostMapping("/inventory/genres/delete/{id}")
    public String deleteGenre(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.deleteGenre(id);
            success(redirectAttributes, message("backend.inventory.genreDeleted"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return CATEGORIES_REDIRECT;
    }

    @PostMapping("/inventory/audit")
    public String audit(RedirectAttributes redirectAttributes) {
        try {
            var summary = inventoryService.performInventoryAudit();
            success(redirectAttributes, message("backend.inventory.auditCompleted",
                    summary.getOrDefault("Available", 0L), summary.getOrDefault("Borrowed", 0L),
                    summary.getOrDefault("Lost", 0L), summary.getOrDefault("Damaged", 0L),
                    summary.getOrDefault("MinorDamaged", 0L)));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return AUDIT_REDIRECT;
    }

    @PostMapping("/inventory/copies/add/{id}")
    public String addBookCopies(@PathVariable Integer id,
            @RequestParam Integer quantity,
            @RequestParam Integer shelfId,
            @RequestParam String bookCondition,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.addBookCopies(id, quantity, shelfId, bookCondition);
            success(redirectAttributes, message("backend.inventory.copiesAdded", quantity));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return AUDIT_REDIRECT;
    }

    @PostMapping("/inventory/copies/delete/{id}")
    public String deleteBookCopies(@PathVariable Integer id,
            @RequestParam(required = false) java.util.List<Integer> itemIds,
            RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = itemIds == null ? 0 : new java.util.HashSet<>(itemIds).size();
            inventoryService.deleteBookCopies(id, itemIds);
            success(redirectAttributes, message("backend.inventory.copiesDeleted", deletedCount));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return AUDIT_REDIRECT;
    }

    @PostMapping("/inventory/copies/update/{bookId}/{itemId}")
    public String updateBookCopy(@PathVariable Integer bookId, @PathVariable Integer itemId,
            @RequestParam String bookCondition, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateBookCopyCondition(bookId, itemId, bookCondition);
            success(redirectAttributes, message("backend.inventory.copyUpdated"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return AUDIT_REDIRECT;
    }

    @PostMapping("/storage/add")
    public String addShelf(@RequestParam String shelfName,
            @RequestParam(required = false) String location, RedirectAttributes redirectAttributes) {
        try {
            storageService.addStorageLocation(shelfName, location);
            success(redirectAttributes, message("backend.storage.added"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return STORAGE_REDIRECT;
    }

    @PostMapping("/storage/update/{id}")
    public String updateShelf(@PathVariable Integer id, @RequestParam String shelfName,
            @RequestParam(required = false) String location, RedirectAttributes redirectAttributes) {
        try {
            storageService.updateStorageLocation(id, shelfName, location);
            success(redirectAttributes, message("backend.storage.updated"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return STORAGE_REDIRECT;
    }

    @PostMapping("/storage/delete/{id}")
    public String deleteShelf(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            storageService.removeStorageLocation(id);
            success(redirectAttributes, message("backend.storage.deleted"));
        } catch (ApplicationException ex) {
            error(redirectAttributes, ex);
        }
        return STORAGE_REDIRECT;
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
