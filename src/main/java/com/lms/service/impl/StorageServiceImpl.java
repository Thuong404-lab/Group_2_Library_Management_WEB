package com.lms.service.impl;

import com.lms.service.StorageService;

import com.lms.repository.ShelfRepository;
import com.lms.repository.BookItemRepository;
import org.springframework.stereotype.Service;

/**
 * StorageService - Xử lý Logic Kho Lưu trữ
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Service
public class StorageServiceImpl implements StorageService {
    private final ShelfRepository shelfRepository;
    private final BookItemRepository bookItemRepository;

    public StorageServiceImpl(ShelfRepository shelfRepository, BookItemRepository bookItemRepository) {
        this.shelfRepository = shelfRepository;
        this.bookItemRepository = bookItemRepository;
    }


    // UC-11.1: Cập nhật vị trí
    @Override
    public void updateStorageLocation(Integer shelfId, String shelfCode, String location) {
        // TODO: Implement
    }

    // UC-11.2: Thêm vị trí mới
    @Override
    public void addStorageLocation(String shelfCode, String location, Integer capacity) {
        // TODO: Implement
    }

    // UC-11.3: Xóa vị trí
    @Override
    public void removeStorageLocation(Integer shelfId) {
        // TODO: Implement - Kiểm tra shelf có sách không trước khi xóa
    }
}
