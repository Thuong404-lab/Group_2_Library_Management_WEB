package com.lms.service;

import org.springframework.stereotype.Service;

/**
 * StorageService - Xử lý Logic Kho Lưu trữ
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Service
public class StorageService {

    // UC-11.1: Cập nhật vị trí
    public void updateStorageLocation(Integer shelfId, String shelfCode, String location) {
        // TODO: Implement
    }

    // UC-11.2: Thêm vị trí mới
    public void addStorageLocation(String shelfCode, String location, Integer capacity) {
        // TODO: Implement
    }

    // UC-11.3: Xóa vị trí
    public void removeStorageLocation(Integer shelfId) {
        // TODO: Implement - Kiểm tra shelf có sách không trước khi xóa
    }
}
