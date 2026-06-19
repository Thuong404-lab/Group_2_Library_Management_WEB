package com.lms.service;

import org.springframework.stereotype.Service;

/**
 * InventoryService - Xử lý Logic Quản lý Kho Sách
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Service
public class InventoryService {

    // UC-12.1: Kiểm kê sách
    public void performInventoryAudit() {
        // TODO: Implement - So sánh số lượng thực tế vs hệ thống
    }

    // UC-12.2: Cập nhật trạng thái sách
    public void updateBookStatus(Integer bookItemId, String status) {
        // TODO: Implement
    }

    // UC-12.3: Thêm sách mới
    public void addNewBook(String title, String isbn, Integer categoryId, Integer quantity) {
        // TODO: Implement - Tạo Book + BookItems
    }

    // UC-12.4: Cập nhật sách
    public void updateBook(Integer bookId, String title, String isbn) {
        // TODO: Implement
    }

    // UC-12.5: Xóa sách
    public void removeBook(Integer bookId) {
        // TODO: Implement - Soft delete
    }

    // UC-12.6: Quản lý danh mục
    public void addCategory(String name) {
        // TODO: Implement
    }

    public void addGenre(String name) {
        // TODO: Implement
    }
}
