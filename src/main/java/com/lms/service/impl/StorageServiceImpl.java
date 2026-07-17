package com.lms.service.impl;

import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.ValidationException;
import com.lms.entity.Shelf;
import com.lms.repository.BookItemRepository;
import com.lms.repository.ShelfRepository;
import com.lms.service.StorageService;
import com.lms.service.LocalizedMessageService;
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
    private final LocalizedMessageService messages;

    public StorageServiceImpl(ShelfRepository shelfRepository, BookItemRepository bookItemRepository,
            LocalizedMessageService messages) {
        this.shelfRepository = shelfRepository;
        this.bookItemRepository = bookItemRepository;
        this.messages = messages;
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
            throw new ValidationException(messages.get("backend.storage.nameRequired"));
        }
        String normalizedShelfName = shelfName.trim();
        if (shelfRepository.existsByShelfNameIgnoreCase(normalizedShelfName)) {
            throw new ConflictException(messages.get("backend.storage.nameExists"));
        }
        Shelf shelf = new Shelf();
        shelf.setShelfName(normalizedShelfName);
        shelf.setLocation(location == null ? null : location.trim());
        shelfRepository.save(shelf);
    }

    @Override
    public void updateStorageLocation(Integer shelfId, String shelfName, String location) {
        if (shelfId == null) {
            throw new ValidationException(messages.get("backend.storage.invalidId"));
        }
        if (shelfName == null || shelfName.trim().isEmpty()) {
            throw new ValidationException(messages.get("backend.storage.nameRequired"));
        }
        Shelf shelf = shelfRepository.findById(shelfId)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.storage.notFound")));
        String normalizedShelfName = shelfName.trim();
        if (!shelf.getShelfName().equalsIgnoreCase(normalizedShelfName)
                && shelfRepository.existsByShelfNameIgnoreCase(normalizedShelfName)) {
            throw new ConflictException(messages.get("backend.storage.nameExists"));
        }
        shelf.setShelfName(normalizedShelfName);
        shelf.setLocation(location == null ? null : location.trim());
        shelfRepository.save(shelf);
    }

    @Override
    public void removeStorageLocation(Integer shelfId) {
        if (!shelfRepository.existsById(shelfId)) {
            throw new ResourceNotFoundException(messages.get("backend.storage.notFound"));
        }
        if (bookItemRepository.countByShelf_ShelfId(shelfId) > 0) {
            throw new ConflictException(messages.get("backend.storage.deleteContainsBooks"));
        }
        shelfRepository.deleteById(shelfId);
    }
}
