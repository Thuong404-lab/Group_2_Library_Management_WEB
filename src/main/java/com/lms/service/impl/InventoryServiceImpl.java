package com.lms.service.impl;

import com.lms.service.InventoryService;

import com.lms.repository.BookItemRepository;
import com.lms.repository.BookDisposalRepository;
import org.springframework.stereotype.Service;

/**
 * InventoryService - Xử lý Logic Quản lý Kho Sách
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Service
public class InventoryServiceImpl implements InventoryService {
    private final BookItemRepository bookItemRepository;
    private final BookDisposalRepository bookDisposalRepository;

    public InventoryServiceImpl(BookItemRepository bookItemRepository, BookDisposalRepository bookDisposalRepository) {
        this.bookItemRepository = bookItemRepository;
        this.bookDisposalRepository = bookDisposalRepository;
    }


    // UC-12.1: Kiểm kê sách
    @Override
    public void performInventoryAudit() {
        // TODO: Implement - So sánh số lượng thực tế vs hệ thống
    }

    // UC-12.2: Cập nhật trạng thái sách
    @Override
    public void updateBookStatus(Integer bookItemId, String status) {
        // TODO: Implement
    }

    // UC-12.3: Thêm sách mới
    @Override
    public void addNewBook(String title, String isbn, Integer categoryId, Integer quantity) {
        // TODO: Implement - Tạo Book + BookItems
    }

    // UC-12.4: Cập nhật sách
    @Override
    public void updateBook(Integer bookId, String title, String isbn) {
        // TODO: Implement
    }

    // UC-12.5: Xóa sách
    @Override
    public void removeBook(Integer bookId) {
        // TODO: Implement - Soft delete
    }

    // UC-12.6: Quản lý danh mục
    @Override
    public void addCategory(String name) {
        // TODO: Implement
    }

    @Override
    public void addGenre(String name) {
        // TODO: Implement
    }
}
