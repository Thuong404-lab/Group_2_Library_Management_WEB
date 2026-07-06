package com.lms.service.impl;

import com.lms.service.SystemService;

import com.lms.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SystemService - Xử lý Logic Quản lý Hệ thống (Backup/Restore/Settings)
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
@Service
public class SystemServiceImpl implements SystemService {
    private static final String MAX_BORROW_DAYS = "MAX_BORROW_DAYS";
    private static final String MAX_RENEWALS = "MAX_RENEWALS";
    private static final String MAX_BOOKS_PER_MEMBER = "MAX_BOOKS_PER_MEMBER";
    private static final String BORROW_FEE_PER_BOOK = "BORROW_FEE_PER_BOOK";

    private final SystemSettingRepository systemSettingRepository;

    public SystemServiceImpl(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }


    // UC-19.1: Backup data
    @Override
    public void backupData() {
        // TODO: Implement - Xuất database ra file
    }

    // UC-19.2: Restore data
    @Override
    public void restoreData(String backupFilePath) {
        // TODO: Implement - Khôi phục database từ file backup
    }

    // UC-19.3: Lấy System Logs
    @Override
    public void getSystemLogs(int page, String action) {
        // TODO: Implement
    }

    // UC-21.1: Cập nhật chính sách mượn/trả
    @Override
    public void updateBorrowingPolicies(Integer maxBorrowDays, Integer maxRenewals,
                                         Integer maxBooksPerMember, Double borrowFeePerBook) {
        // TODO: Hiện tại chỉ cần borrowFeePerBook cho bài toán "giá tiền mượn".
        // Các tham số khác có thể được mở rộng tương tự.

        upsertSetting(MAX_BORROW_DAYS, maxBorrowDays);
        upsertSetting(MAX_RENEWALS, maxRenewals);
        upsertSetting(MAX_BOOKS_PER_MEMBER, maxBooksPerMember);
        upsertSetting(BORROW_FEE_PER_BOOK, borrowFeePerBook);
    }

    @Override
    public Map<String, String> getBorrowingPolicySettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("maxBorrowDays", getSettingValue(MAX_BORROW_DAYS, "14"));
        settings.put("maxRenewals", getSettingValue(MAX_RENEWALS, "2"));
        settings.put("maxBooksPerMember", getSettingValue(MAX_BOOKS_PER_MEMBER, "10"));
        settings.put("borrowFeePerBook", getSettingValue(BORROW_FEE_PER_BOOK, "5000"));
        return settings;
    }

    private void upsertSetting(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }

        String settingValue = value == null ? null : String.valueOf(value);
        com.lms.entity.SystemSetting setting = systemSettingRepository
                .findBySettingKeyIgnoreCase(key)
                .orElseGet(() -> new com.lms.entity.SystemSetting());

        setting.setSettingKey(key);
        setting.setSettingValue(settingValue);
        setting.setDescription("Auto-updated by admin/settings");

        systemSettingRepository.save(setting);
    }

    private String getSettingValue(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                .map(com.lms.entity.SystemSetting::getSettingValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(defaultValue);
    }
}

