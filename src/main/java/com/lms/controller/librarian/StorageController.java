package com.lms.controller.librarian;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * StorageController - Quản lý Kho & Vị trí Lưu trữ Sách
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Controller
@RequestMapping("/librarian/storage")
public class StorageController {

    // UC-11: Hiển thị danh sách vị trí lưu trữ (Shelves)
    @GetMapping
    public String listStorageLocations(Model model) {
        // TODO: Implement - Gọi ShelfRepository.findAll()
        return "librarian/storage";
    }

    // UC-11.2: Add Storage Location
    @PostMapping("/add")
    public String addStorageLocation(Model model) {
        // TODO: Implement - Validate và lưu Shelf mới
        // TODO: Kiểm tra trùng lặp shelfCode
        return "redirect:/librarian/storage?added";
    }

    // UC-11.1: Update Storage Location
    @PostMapping("/update/{id}")
    public String updateStorageLocation(@PathVariable Integer id, Model model) {
        // TODO: Implement - Tìm Shelf theo ID, cập nhật thông tin
        return "redirect:/librarian/storage?updated";
    }

    // UC-11.3: Remove Storage Location
    @PostMapping("/delete/{id}")
    public String removeStorageLocation(@PathVariable Integer id, Model model) {
        // TODO: Implement - Kiểm tra Shelf có chứa sách không trước khi xóa
        // TODO: Nếu còn sách → báo lỗi, không cho xóa
        return "redirect:/librarian/storage?deleted";
    }
}
