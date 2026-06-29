package com.lms.controller.librarian;

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
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping
    public String listStorageLocations(Model model) {
        model.addAttribute("shelves", storageService.getAllStorageLocations());
        return "librarian/storage";
    }

    @GetMapping("/add")
    public String showAddStorageLocation(Model model) {
        model.addAttribute("shelf", null);
        return "librarian/storage-form";
    }

    @PostMapping("/add")
    public String addStorageLocation(@RequestParam String shelfName,
                                     @RequestParam(required = false) String location,
                                     RedirectAttributes redirectAttributes) {
        try {
            storageService.addStorageLocation(shelfName, location);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm vị trí lưu trữ thành công.");
            return "redirect:/librarian/storage";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/librarian/storage/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditStorageLocation(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return storageService.getStorageLocationById(id)
                .map(shelf -> {
                    model.addAttribute("shelf", shelf);
                    return "librarian/storage-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy vị trí lưu trữ.");
                    return "redirect:/librarian/storage";
                });
    }

    @PostMapping("/update/{id}")
    public String updateStorageLocation(@PathVariable Integer id,
                                        @RequestParam String shelfName,
                                        @RequestParam(required = false) String location,
                                        RedirectAttributes redirectAttributes) {
        try {
            storageService.updateStorageLocation(id, shelfName, location);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vị trí lưu trữ thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/librarian/storage";
    }

    @PostMapping("/delete/{id}")
    public String removeStorageLocation(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            storageService.removeStorageLocation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa vị trí lưu trữ thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/librarian/storage";
    }
}
