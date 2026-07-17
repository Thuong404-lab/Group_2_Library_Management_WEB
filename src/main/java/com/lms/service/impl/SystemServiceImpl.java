package com.lms.service.impl;

import com.lms.entity.SystemLog;
import com.lms.entity.SystemSetting;
import com.lms.exception.ValidationException;
import com.lms.entity.MembershipTier;
import com.lms.enums.ActionType;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.MemberAccountRepository;
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
    private final MembershipTierRepository membershipTierRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final AuditLogService auditLogService;

    public SystemServiceImpl(SystemSettingRepository systemSettingRepository,
                             SystemLogRepository systemLogRepository,
                             MembershipTierRepository membershipTierRepository,
                             MemberAccountRepository memberAccountRepository,
                             StaffAccountRepository staffAccountRepository,
                             AuditLogService auditLogService) {
        this.systemSettingRepository = systemSettingRepository;
        this.systemLogRepository = systemLogRepository;
        this.membershipTierRepository = membershipTierRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public void restoreData(String backupFilePath) {
        // TODO: Implement restore sau nếu cần
    }

    @Override
    public Page<SystemLog> getSystemLogs(int page, String action, String keyword, String actionType) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), 10);
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        String section = normalizeLogSection(action);

        Page<SystemLog> logs = systemLogRepository.searchLogsBySection(
                section, trimmedKeyword, actionType == null ? "" : actionType.trim(), pageRequest);

        logs.forEach(this::populateActorUsername);
        return logs;
    }

    private String normalizeLogSection(String section) {
        if ("operations".equalsIgnoreCase(section)) {
            return "operations";
        }
        if ("circulation".equalsIgnoreCase(section)) {
            return "circulation";
        }
        return "auth";
    }

    private void populateActorUsername(SystemLog log) {
        if (log.getUser() == null || log.getUser().getId() == null) {
            log.setActorUsername("System");
            return;
        }

        Integer userId = log.getUser().getId();
        String username = memberAccountRepository.findByMember_User_Id(userId)
                .map(account -> account.getUsername())
                .orElseGet(() -> staffAccountRepository.findByStaff_User_Id(userId)
                        .map(account -> account.getUsername())
                        .orElse("System"));
        log.setActorUsername(username);
    }

    @Override
    public List<SystemSetting> getAllSettings() {
        return systemSettingRepository.findAll(Sort.by("settingKey").ascending());
    }

    @Override
    public Map<String, String> getSettingMap() {
        Map<String, String> settingMap = new LinkedHashMap<>();

        for (SystemSetting setting : getAllSettings()) {
            settingMap.put(setting.getSettingKey(), setting.getSettingValue());
        }

        return settingMap;
    }

    @Override
    public int getIntSetting(String settingKey, int defaultValue) {
        return systemSettingRepository.findBySettingKeyIgnoreCase(settingKey)
                .map(SystemSetting::getSettingValue)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    @Override
    public List<MembershipTier> getMembershipTiers() {
        return membershipTierRepository.findAll(Sort.by("tierId").ascending());
    }

    @Override
    @Transactional
    public void updateBorrowingPolicies(Integer maxBorrowDays,
                                        Integer maxRenewalDays,
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

        saveOrUpdateSetting("Max_Books_Per_Member",
                String.valueOf(tierBorrowLimits.values().stream().mapToInt(Integer::intValue).max().orElse(1)),
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

    private void validateAndUpdateTiers(Map<Integer, Integer> tierBorrowLimits,
                                        Map<Integer, BigDecimal> tierSpendingConditions) {
        List<MembershipTier> tiers = getMembershipTiers();
        if (tiers.isEmpty()) {
            throw new ValidationException(messages.get("backend.settings.noTiers"));
        }

        for (MembershipTier tier : tiers) {
            Integer borrowLimit = tierBorrowLimits == null ? null : tierBorrowLimits.get(tier.getTierId());
            validatePositive(borrowLimit,
                    messages.get("backend.settings.tierBorrowLimitPositive", tier.getTierName()));
            BigDecimal spendingCondition = tierSpendingConditions == null
                    ? null
                    : tierSpendingConditions.get(tier.getTierId());
            validateZeroOrPositive(spendingCondition,
                    messages.get("backend.settings.tierSpendingNonNegative", tier.getTierName()));
            tier.setBorrowLimit(borrowLimit);
            tier.setCondition(spendingCondition);
        }

        membershipTierRepository.saveAll(tiers);
    }

    private void saveOrUpdateSetting(String key, String value, String description) {
        SystemSetting setting = systemSettingRepository.findBySettingKeyIgnoreCase(key)
                .orElseGet(SystemSetting::new);

        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setDescription(description);

        systemSettingRepository.save(setting);
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
