package com.lms.service;

import com.lms.entity.SystemLog;
import com.lms.entity.SystemSetting;
import com.lms.entity.MembershipTier;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

/**
 * SystemService - Xử lý Logic Quản lý Hệ thống (Backup/Restore/Settings)
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
public interface SystemService {

    void restoreData(String backupFilePath);

    Page<SystemLog> getSystemLogs(int page, String action, String keyword);

    List<SystemSetting> getAllSettings();

    Map<String, String> getSettingMap();

    List<MembershipTier> getMembershipTiers();

    void updateBorrowingPolicies(Integer maxBorrowDays,
                                 Integer maxRenewals,
                                 Map<Integer, Integer> tierBorrowLimits,
                                 Double borrowFeePerBook,
                                 Double finePerDay,
                                 Double damageCompensationAmount,
                                 Integer damageCompensationThreshold,
                                 Integer overdueViolationLockLimit,
                                 Integer bookDisposalConditionThreshold,
                                 Double loyalUpgradeSpendingThreshold,
                                 Integer standardTierId,
                                 Integer loyalTierId,
                                 Double depositAmount);
}
