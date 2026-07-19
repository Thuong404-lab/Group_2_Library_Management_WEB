package com.lms.service;

import com.lms.entity.Shelf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * StorageService - Xử lý Logic Kho Lưu trữ
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
public interface StorageService {

    List<Shelf> getAllStorageLocations();

    Page<Shelf> getStorageLocations(Pageable pageable);

    Optional<Shelf> getStorageLocationById(Integer shelfId);

    void updateStorageLocation(Integer shelfId, String shelfName, String location);

    void addStorageLocation(String shelfName, String location);

    void removeStorageLocation(Integer shelfId);

}
