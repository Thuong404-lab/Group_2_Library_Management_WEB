package com.lms.service;

/**
 * StorageService - Xử lý Logic Kho Lưu trữ
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
public interface StorageService {

    // UC-11.1: Cập nhật vị trí
    void updateStorageLocation(Integer shelfId, String shelfCode, String location);

    // UC-11.2: Thêm vị trí mới
    void addStorageLocation(String shelfCode, String location, Integer capacity);

    // UC-11.3: Xóa vị trí
    void removeStorageLocation(Integer shelfId);

}
