package com.lms.service;

import java.util.Map;

/**
 * SystemService - Xử lý Logic Quản lý Hệ thống (Backup/Restore/Settings)
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
public interface SystemService {

    // UC-19.1: Backup data
    void backupData();

    // UC-19.2: Restore data
    void restoreData(String backupFilePath);

    // UC-19.3: Lấy System Logs
    void getSystemLogs(int page, String action);

    // UC-21.1: Cập nhật chính sách mượn/trả
    void updateBorrowingPolicies(Integer maxBorrowDays, Integer maxRenewals,
                                 Integer maxBooksPerMember, Double borrowFeePerBook);

    Map<String, String> getBorrowingPolicySettings();

}
