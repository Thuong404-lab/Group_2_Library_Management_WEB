package com.lms.service;

import com.lms.entity.SystemLog;
import com.lms.entity.SystemSetting;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * SystemService - Xử lý Logic Quản lý Hệ thống (Backup/Restore/Settings)
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
public interface SystemService {

    void restoreData(String backupFilePath);

    Page<SystemLog> getSystemLogs(int page, String action, String keyword, String actionType);

    List<SystemSetting> getAllSettings();

    Map<String, String> getSettingMap();

    int getIntSetting(String settingKey, int defaultValue);

    void updateBorrowingPolicies(Integer maxBorrowDays,
                                 Integer maxRenewalDays,
                                 BigDecimal borrowFeePerBook,
                                 BigDecimal finePerDay,
                                 BigDecimal damageCompensationAmount,
                                 Integer damageCompensationThreshold,
                                 Integer overdueViolationLockLimit,
                                 Integer bookDisposalConditionThreshold,
                                 BigDecimal depositAmount);
}
