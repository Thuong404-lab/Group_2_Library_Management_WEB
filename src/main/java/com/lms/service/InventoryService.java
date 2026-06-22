package com.lms.service;

/**
 * InventoryService - Xử lý Logic Quản lý Kho Sách
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
public interface InventoryService {

    // UC-12.1: Kiểm kê sách
    void performInventoryAudit();

    // UC-12.2: Cập nhật trạng thái sách
    void updateBookStatus(Integer bookItemId, String status);

    // UC-12.3: Thêm sách mới
    void addNewBook(String title, String isbn, Integer categoryId, Integer quantity);

    // UC-12.4: Cập nhật sách
    void updateBook(Integer bookId, String title, String isbn);

    // UC-12.5: Xóa sách
    void removeBook(Integer bookId);

    // UC-12.6: Quản lý danh mục
    void addCategory(String name);

    void addGenre(String name);

}
