package com.lms.controller.librarian;
import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.entity.Shelf;
import com.lms.service.StorageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * StorageController - Quản lý Kho & Vị trí Lưu trữ Sách
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Controller
@RequestMapping("/librarian/storage")
public class StorageController extends LocalizedControllerSupport {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public String listStorageLocations() {
        return "redirect:/librarian/dashboard?section=books&subsection=storage";
    }

    @GetMapping("/add")
    public String showAddStorageLocation() {
        return "librarian/storage-form";
    }

    @PostMapping("/add")
    public String addStorageLocation(@RequestParam String shelfName,
            @RequestParam(required = false) String location,
            RedirectAttributes redirectAttributes) {
        try {
            storageService.addStorageLocation(shelfName, location);
            redirectAttributes.addFlashAttribute("success", message("backend.storage.added"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=storage";
    }

    @GetMapping("/edit/{id}")
    public String showEditStorageLocation(@PathVariable Integer id, RedirectAttributes redirectAttributes,
            Model model) {
        return storageService.getStorageLocationById(id)
                .map(shelf -> {
                    model.addAttribute("shelf", shelf);
                    return "librarian/storage-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", message("backend.storage.notFound"));
                    return "redirect:/librarian/dashboard?section=books&subsection=storage";
                });
    }

    @PostMapping("/update/{id}")
    public String updateStorageLocation(@PathVariable Integer id,
            @RequestParam String shelfName,
            @RequestParam(required = false) String location,
            RedirectAttributes redirectAttributes) {
        try {
            storageService.updateStorageLocation(id, shelfName, location);
            redirectAttributes.addFlashAttribute("success", message("backend.storage.updated"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=storage";
    }

    @PostMapping("/delete/{id}")
    public String removeStorageLocation(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            storageService.removeStorageLocation(id);
            redirectAttributes.addFlashAttribute("success", message("backend.storage.deleted"));
        } catch (ApplicationException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/librarian/dashboard?section=books&subsection=storage";
    }
}
