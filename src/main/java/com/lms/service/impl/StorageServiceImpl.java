package com.lms.service.impl;

import com.lms.entity.Shelf;
import com.lms.repository.BookItemRepository;
import com.lms.repository.ShelfRepository;
import com.lms.service.StorageService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * StorageService - Xử lý Logic Kho Lưu trữ
 * Người phụ trách: La Tấn Khanh (CE191640)
 */
@Service
@Transactional
public class StorageServiceImpl implements StorageService {
    private final ShelfRepository shelfRepository;
    private final BookItemRepository bookItemRepository;

    public StorageServiceImpl(ShelfRepository shelfRepository, BookItemRepository bookItemRepository) {
        this.shelfRepository = shelfRepository;
        this.bookItemRepository = bookItemRepository;
    }

    @Override
    public List<Shelf> getAllStorageLocations() {
        return shelfRepository.findAll(Sort.by(Sort.Direction.ASC, "shelfId"));
    }

    @Override
    public Optional<Shelf> getStorageLocationById(Integer shelfId) {
        return shelfRepository.findById(shelfId);
    }

    @Override
    public void addStorageLocation(String shelfName, String location) {
        if (shelfName == null || shelfName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên vị trí không được để trống.");
        }
        String normalizedShelfName = shelfName.trim();
        if (shelfRepository.existsByShelfNameIgnoreCase(normalizedShelfName)) {
            throw new IllegalArgumentException("Tên vị trí đã tồn tại.");
        }
        Shelf shelf = new Shelf();
        shelf.setShelfName(normalizedShelfName);
        shelf.setLocation(location == null ? null : location.trim());
        shelfRepository.save(shelf);
    }

    @Override
    public void updateStorageLocation(Integer shelfId, String shelfName, String location) {
        if (shelfId == null) {
            throw new IllegalArgumentException("Id vị trí không hợp lệ.");
        }
        if (shelfName == null || shelfName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên vị trí không được để trống.");
        }
        Shelf shelf = shelfRepository.findById(shelfId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vị trí lưu trữ."));
        String normalizedShelfName = shelfName.trim();
        if (!shelf.getShelfName().equalsIgnoreCase(normalizedShelfName)
                && shelfRepository.existsByShelfNameIgnoreCase(normalizedShelfName)) {
            throw new IllegalArgumentException("Tên vị trí đã tồn tại.");
        }
        shelf.setShelfName(normalizedShelfName);
        shelf.setLocation(location == null ? null : location.trim());
        shelfRepository.save(shelf);
    }

    @Override
    public void removeStorageLocation(Integer shelfId) {
        if (!shelfRepository.existsById(shelfId)) {
            throw new IllegalArgumentException("Vị trí lưu trữ không tồn tại.");
        }
        if (bookItemRepository.countByShelf_ShelfId(shelfId) > 0) {
            throw new IllegalStateException("Không thể xóa vị trí đang chứa sách.");
        }
        shelfRepository.deleteById(shelfId);
    }
}
