package com.lms.controller.librarian;
import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

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
public class InventoryController extends LocalizedControllerSupport {

    private final InventoryService inventoryService;
    private final FileUploadService fileUploadService;

    public InventoryController(InventoryService inventoryService, FileUploadService fileUploadService) {
        this.inventoryService = inventoryService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping
    public String listBooks() {
        return "redirect:/librarian/books?subsection=inventory";
    }

    @GetMapping("/add")
    public String showAddBookForm() {
        return "redirect:/librarian/books?subsection=inventory";
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
            @RequestParam(defaultValue = "0") int bookPage,
            RedirectAttributes redirectAttributes) {
        try {
            String coverImageUrl = null;
            if (coverImage != null && !coverImage.isEmpty()) {
                coverImageUrl = fileUploadService.storeFile(coverImage);
            }
            inventoryService.addNewBook(title, isbn, genreId, quantity, description, coverImageUrl, shelfId,
                    bookCondition, author);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.bookAdded"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return inventoryPageRedirect(bookPage, false);
    }

    @PostMapping(value = "/edit/{id}", consumes = "multipart/form-data")
    public String updateBook(@PathVariable Integer id,
            @RequestParam String title,
            @RequestParam String isbn,
            @RequestParam Integer genreId,
            @RequestParam(name = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(required = false) Integer shelfId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "0") int bookPage,
            RedirectAttributes redirectAttributes) {
        try {
            String coverImageUrl = null;
            if (coverImage != null && !coverImage.isEmpty()) {
                coverImageUrl = fileUploadService.storeFile(coverImage);
            }
            inventoryService.updateBook(id, title, isbn, genreId, null, coverImageUrl, shelfId,
                    description, author);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.bookUpdated"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return inventoryPageRedirect(bookPage, false);
    }

    @PostMapping("/delete/{id}")
    public String removeBook(@PathVariable Integer id,
            @RequestParam(defaultValue = "0") int bookPage,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.removeBook(id);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.bookDeleted"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return inventoryPageRedirect(bookPage, false);
    }

    @PostMapping("/status/{id}")
    public String updateBookStatus(@PathVariable Integer id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateBookStatus(id, status);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.bookStatusUpdated"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/books?subsection=inventory";
    }

    @PostMapping("/copies/add/{id}")
    public String addBookCopies(@PathVariable Integer id,
            @RequestParam Integer quantity,
            @RequestParam Integer shelfId,
            @RequestParam(required = false) String bookCondition,
            @RequestParam(defaultValue = "0") int bookPage,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.addBookCopies(id, quantity, shelfId, bookCondition);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.copiesAdded", quantity));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return inventoryPageRedirect(bookPage, true);
    }

    @PostMapping("/copies/delete/{id}")
    public String deleteBookCopies(@PathVariable Integer id,
            @RequestParam(required = false) java.util.List<Integer> itemIds,
            @RequestParam(defaultValue = "0") int bookPage,
            RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = itemIds == null ? 0 : new java.util.HashSet<>(itemIds).size();
            inventoryService.deleteBookCopies(id, itemIds);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.copiesDeleted", deletedCount));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return inventoryPageRedirect(bookPage, true);
    }

    @PostMapping("/copies/update/{bookId}/{itemId}")
    public String updateBookCopy(@PathVariable Integer bookId, @PathVariable Integer itemId,
            @RequestParam String bookCondition,
            @RequestParam(defaultValue = "0") int bookPage,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateBookCopyCondition(bookId, itemId, bookCondition);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.copyUpdated"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return inventoryPageRedirect(bookPage, true);
    }

    private String inventoryPageRedirect(int bookPage, boolean auditTab) {
        return "redirect:/librarian/books?subsection=inventory"
                + (auditTab ? "&tab=audit" : "")
                + "&bookPage=" + Math.max(0, bookPage);
    }

    @GetMapping("/categories")
    public String manageCategories() {
        return "redirect:/librarian/books?subsection=inventory&tab=categories";
    }

    @PostMapping("/categories/add")
    public String addCategory(@RequestParam String type,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        try {
            if ("genre".equals(type)) {
                inventoryService.addGenre(categoryId, name);
                redirectAttributes.addFlashAttribute("success", message("backend.inventory.genreAdded"));
            } else {
                inventoryService.addCategory(name);
                redirectAttributes.addFlashAttribute("success", message("backend.inventory.categoryAdded"));
            }
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/books?subsection=inventory&tab=categories";
    }

    @PostMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable Integer id,
            @RequestParam String name,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateCategory(id, name);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.categoryUpdated"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/books?subsection=inventory&tab=categories";
    }

    @PostMapping("/genres/edit/{id}")
    public String editGenre(@PathVariable Integer id,
            @RequestParam String name,
            @RequestParam(required = false) Integer categoryId,
            RedirectAttributes redirectAttributes) {
        try {
            inventoryService.updateGenre(id, name, categoryId);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.genreUpdated"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/books?subsection=inventory&tab=categories";
    }

    @PostMapping("/genres/delete/{id}")
    public String deleteGenre(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.deleteGenre(id);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.genreDeleted"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/books?subsection=inventory&tab=categories";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            inventoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", message("backend.inventory.categoryDeleted"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/books?subsection=inventory&tab=categories";
    }

    @GetMapping("/audit")
    public String showInventoryAudit() {
        return "redirect:/librarian/books?subsection=inventory&tab=audit";
    }

    @PostMapping("/audit")
    public String processInventoryAudit(RedirectAttributes redirectAttributes) {
        try {
            var summary = inventoryService.performInventoryAudit();
            redirectAttributes.addFlashAttribute("success",
                    message("backend.inventory.auditCompleted",
                            summary.getOrDefault("Available", 0L),
                            summary.getOrDefault("Borrowed", 0L),
                            summary.getOrDefault("Waiting_Pickup", 0L),
                            summary.getOrDefault("Unavailable", 0L)));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/books?subsection=inventory&tab=audit";
    }
}
