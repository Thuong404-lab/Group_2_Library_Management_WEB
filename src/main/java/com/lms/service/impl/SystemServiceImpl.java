package com.lms.service.impl;

import com.lms.entity.MembershipTier;
import com.lms.entity.SystemLog;
import com.lms.entity.SystemSetting;
import com.lms.exception.ValidationException;
import com.lms.enums.ActionType;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.SystemLogRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.service.SystemService;
import com.lms.service.AuditLogService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SystemService - Xử lý Logic Quản lý Hệ thống (Backup/Restore/Settings)
 * Người phụ trách: Trần Ngọc Linh Đang (CE191088)
 */
@Service
public class SystemServiceImpl implements SystemService {

    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();

    private final SystemSettingRepository systemSettingRepository;
    private final SystemLogRepository systemLogRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final AuditLogService auditLogService;
    private final MembershipTierRepository membershipTierRepository;

    public SystemServiceImpl(SystemSettingRepository systemSettingRepository,
                             SystemLogRepository systemLogRepository,
                             MemberAccountRepository memberAccountRepository,
                             StaffAccountRepository staffAccountRepository,
                             AuditLogService auditLogService,
                             MembershipTierRepository membershipTierRepository) {
        this.systemSettingRepository = systemSettingRepository;
        this.systemLogRepository = systemLogRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.auditLogService = auditLogService;
        this.membershipTierRepository = membershipTierRepository;
    }

    @Override
    public void restoreData(String backupFilePath) {
        // TODO: Implement restore sau nếu cần
    }

    @Override
    public Page<SystemLog> getSystemLogs(int page, String action, String keyword, String actionType) {
        PageRequest pageable = PageRequest.of(page, 10);

        String kw = keyword != null ? keyword.trim() : "";
        String actType = actionType != null ? actionType.trim() : "";

        if (action != null && !action.trim().isEmpty()) {
            return systemLogRepository.searchLogsBySection(action.trim(), kw, actType, pageable);
        }

        if (!kw.isEmpty()) {
            return systemLogRepository.searchLogs(kw, pageable);
        }

        return systemLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAll();
    }

    @Override
    public Map<String, String> getSettingMap() {
        List<SystemSetting> settings = getAllSettings();
        Map<String, String> map = new LinkedHashMap<>();
        for (SystemSetting setting : settings) {
            map.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return map;
    }

    @Override
    public int getIntSetting(String settingKey, int defaultValue) {
        return systemSettingRepository.findBySettingKeyIgnoreCase(settingKey)
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    @Transactional
    public void updateBorrowingPolicies(Integer maxBorrowDays,
                                        Integer maxRenewalDays,
                                        Integer maxRenewalRequests,
                                        Integer renewalRejectionCooldownHours,
                                        Map<Integer, Integer> tierBorrowLimits,
                                        Map<Integer, BigDecimal> tierSpendingConditions,
                                        BigDecimal borrowFeePerBook,
                                        BigDecimal finePerDay,
                                        BigDecimal damageCompensationAmount,
                                        Integer damageCompensationThreshold,
                                        Integer overdueViolationLockLimit,
                                        Integer bookDisposalConditionThreshold,
                                        BigDecimal depositAmount) {

        validatePositive(maxBorrowDays, messages.get("backend.settings.maxBorrowDaysPositive"));
        validatePositive(maxRenewalDays, messages.get("backend.settings.maxRenewalDaysPositive"));
        validatePositive(maxRenewalRequests, messages.get("backend.settings.maxRenewalRequestsPositive"));
        validatePositive(renewalRejectionCooldownHours, messages.get("backend.settings.renewalCooldownPositive"));
        validateAndUpdateTiers(tierBorrowLimits, tierSpendingConditions);
        validateZeroOrPositive(borrowFeePerBook, messages.get("backend.settings.borrowFeeNonNegative"));
        validateZeroOrPositive(finePerDay, messages.get("backend.settings.fineNonNegative"));
        validateZeroOrPositive(damageCompensationAmount, messages.get("backend.settings.compensationNonNegative"));
        validatePercentage(damageCompensationThreshold, messages.get("backend.settings.damageThresholdRange"));
        validateZeroOrPositive(overdueViolationLockLimit, messages.get("backend.settings.overdueLimitNonNegative"));
        validatePercentage(bookDisposalConditionThreshold, messages.get("backend.settings.disposalThresholdRange"));
        validateZeroOrPositive(depositAmount, messages.get("backend.settings.depositNonNegative"));

        saveOrUpdateSetting("Max_Borrow_Days",
                String.valueOf(maxBorrowDays),
                messages.get("backend.settings.description.maxBorrowDays"));

        saveOrUpdateSetting("Max_Renewal_Days",
                String.valueOf(maxRenewalDays),
                messages.get("backend.settings.description.maxRenewalDays"));

        saveOrUpdateSetting("MAX_RENEWAL_REQUESTS_PER_LOAN",
                String.valueOf(maxRenewalRequests),
                messages.get("backend.settings.description.maxRenewalRequests"));

        saveOrUpdateSetting("RENEWAL_REJECTION_COOLDOWN_HOURS",
                String.valueOf(renewalRejectionCooldownHours),
                messages.get("backend.settings.description.renewalCooldown"));


        saveOrUpdateSetting("Max_Books_Per_Member",
                String.valueOf(tierBorrowLimits != null ? tierBorrowLimits.values().stream().mapToInt(Integer::intValue).max().orElse(1) : 1),
                messages.get("backend.settings.description.maxBooks"));

        saveOrUpdateSetting("Borrow_Fee_Per_Book",
                borrowFeePerBook.toPlainString(),
                messages.get("backend.settings.description.borrowFee"));

        saveOrUpdateSetting("Fine_Per_Day",
                finePerDay.toPlainString(),
                messages.get("backend.settings.description.finePerDay"));

        saveOrUpdateSetting("Damage_Compensation_Amount",
                damageCompensationAmount.toPlainString(),
                messages.get("backend.settings.description.compensation"));

        saveOrUpdateSetting("Damage_Compensation_Threshold",
                String.valueOf(damageCompensationThreshold),
                messages.get("backend.settings.description.damageThreshold"));

        saveOrUpdateSetting("Overdue_Violation_Lock_Limit",
                String.valueOf(overdueViolationLockLimit),
                messages.get("backend.settings.description.overdueLockLimit"));

        saveOrUpdateSetting("Book_Disposal_Condition_Threshold",
                String.valueOf(bookDisposalConditionThreshold),
                messages.get("backend.settings.description.disposalThreshold"));

        saveOrUpdateSetting("Deposit_Amount",
                depositAmount.toPlainString(),
                messages.get("backend.settings.description.deposit"));

        auditLogService.log(
                ActionType.UPDATE_SETTINGS,
                messages.get("backend.settings.audit.updated"));
    }

    private void saveOrUpdateSetting(String key, String value, String description) {
        SystemSetting setting = systemSettingRepository.findBySettingKeyIgnoreCase(key)
                .orElseGet(SystemSetting::new);

        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);

        systemSettingRepository.save(setting);
    }

    private void validateAndUpdateTiers(Map<Integer, Integer> tierBorrowLimits, Map<Integer, BigDecimal> tierSpendingConditions) {
        if (tierBorrowLimits != null && !tierBorrowLimits.isEmpty()) {
            tierBorrowLimits.forEach((tierId, limit) -> {
                if (tierId != null && limit != null) {
                    validateZeroOrPositive(limit, messages.get("backend.settings.tierBorrowLimitNonNegative"));
                    membershipTierRepository.findById(tierId).ifPresent(tier -> {
                        tier.setBorrowLimit(limit);
                        membershipTierRepository.save(tier);
                    });
                }
            });
        }
        if (tierSpendingConditions != null && !tierSpendingConditions.isEmpty()) {
            tierSpendingConditions.forEach((tierId, condition) -> {
                if (tierId != null && condition != null) {
                    validateZeroOrPositive(condition, messages.get("backend.settings.tierConditionNonNegative"));
                    membershipTierRepository.findById(tierId).ifPresent(tier -> {
                        tier.setCondition(condition);
                        membershipTierRepository.save(tier);
                    });
                }
            });
        }
    }

    private void validatePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new ValidationException(message);
        }
    }

    private void validateZeroOrPositive(Integer value, String message) {
        if (value == null || value < 0) {
            throw new ValidationException(message);
        }
    }

    private void validatePercentage(Integer value, String message) {
        if (value == null || value < 0 || value > 100) {
            throw new ValidationException(message);
        }
    }

    private void validateZeroOrPositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(message);
        }
    }
}
