package com.lms.service.impl;

import com.lms.entity.SystemLog;
import com.lms.entity.SystemSetting;
import com.lms.exception.ValidationException;
import com.lms.enums.ActionType;
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
    private final MemberAccountRepository memberAccountRepository;
    private final StaffAccountRepository staffAccountRepository;
    private final AuditLogService auditLogService;

    public SystemServiceImpl(SystemSettingRepository systemSettingRepository,
            SystemLogRepository systemLogRepository,
            MemberAccountRepository memberAccountRepository,
            StaffAccountRepository staffAccountRepository,
            AuditLogService auditLogService) {
        this.systemSettingRepository = systemSettingRepository;
        this.systemLogRepository = systemLogRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.staffAccountRepository = staffAccountRepository;
        this.auditLogService = auditLogService;
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
    @Transactional
    public void updateBorrowingPolicies(Integer maxBorrowDays,
            Integer maxRenewalDays,
            Integer maxRenewalRequests,
            Integer renewalRejectionCooldownHours,
            Integer renewalApprovalTimeoutHours,
            BigDecimal borrowFeePerBook,
            BigDecimal finePerDay,
            BigDecimal damageCompensationAmount,
            Integer damageCompensationThreshold,
            Integer overdueViolationLockLimit,
            BigDecimal depositAmount) {

        validatePositive(maxBorrowDays, messages.get("backend.settings.maxBorrowDaysPositive"));
        validatePositive(maxRenewalDays, messages.get("backend.settings.maxRenewalDaysPositive"));
        validatePositive(maxRenewalRequests, messages.get("backend.settings.maxRenewalRequestsPositive"));
        validatePositive(renewalRejectionCooldownHours, messages.get("backend.settings.renewalCooldownPositive"));
        validatePositive(renewalApprovalTimeoutHours, messages.get("backend.settings.renewalCooldownPositive"));
        validateZeroOrPositive(borrowFeePerBook, messages.get("backend.settings.borrowFeeNonNegative"));
        validateZeroOrPositive(finePerDay, messages.get("backend.settings.fineNonNegative"));
        validateZeroOrPositive(damageCompensationAmount, messages.get("backend.settings.compensationNonNegative"));
        validateDamageThreshold(damageCompensationThreshold, messages.get("backend.settings.damageThresholdRange"));
        validateZeroOrPositive(overdueViolationLockLimit, messages.get("backend.settings.overdueLimitNonNegative"));
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

        saveOrUpdateSetting("RENEWAL_APPROVAL_TIMEOUT_HOURS",
                String.valueOf(renewalApprovalTimeoutHours),
                "Maximum hours to process a renewal request");

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

    private void validateDamageThreshold(Integer value, String message) {
        if (value == null || value < 2 || value > 4) {
            throw new ValidationException(message);
        }
    }

    private void validateZeroOrPositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(message);
        }
    }
}
