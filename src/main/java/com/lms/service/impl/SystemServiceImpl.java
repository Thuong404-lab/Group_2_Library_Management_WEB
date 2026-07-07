package com.lms.service.impl;

import com.lms.entity.SystemLog;
import com.lms.entity.SystemSetting;
import com.lms.entity.MembershipTier;
import com.lms.enums.ActionType;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.MemberAccountRepository;
import com.lms.repository.StaffAccountRepository;
import com.lms.repository.SystemLogRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.service.SystemService;
import com.lms.service.AuditLogService;
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
    public Page<SystemLog> getSystemLogs(int page, String action, String keyword) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), 10);

        String trimmedAction = action == null ? "" : action.trim();
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        String searchKeyword = (trimmedAction + " " + trimmedKeyword).trim();

        Page<SystemLog> logs = !searchKeyword.isEmpty()
                ? systemLogRepository.searchLogs(searchKeyword, pageRequest)
                : systemLogRepository.findAllByOrderByCreatedAtDesc(pageRequest);

        logs.forEach(this::populateActorUsername);
        return logs;
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

        validatePositive(maxBorrowDays, "Số ngày mượn tối đa phải lớn hơn 0.");
        validatePositive(maxRenewalDays, "Số ngày gia hạn tối đa phải lớn hơn 0.");
        validateAndUpdateTiers(tierBorrowLimits, tierSpendingConditions);
        validateZeroOrPositive(borrowFeePerBook, "Phí mượn sách không được âm.");
        validateZeroOrPositive(finePerDay, "Phí phạt quá hạn không được âm.");
        validateZeroOrPositive(damageCompensationAmount, "Phí bồi thường không được âm.");
        validatePercentage(damageCompensationThreshold, "Ngưỡng hư hỏng phải nằm trong khoảng 0-100%.");
        validateZeroOrPositive(overdueViolationLockLimit, "Số lần quá hạn trước khi khóa không được âm.");
        validatePercentage(bookDisposalConditionThreshold, "Ngưỡng thanh lý sách phải nằm trong khoảng 0-100%.");
        validateZeroOrPositive(depositAmount, "Tiền cọc không được âm.");

        saveOrUpdateSetting("Max_Borrow_Days",
                String.valueOf(maxBorrowDays),
                "Số ngày mượn sách tối đa tiêu chuẩn");

        saveOrUpdateSetting("Max_Renewal_Days",
                String.valueOf(maxRenewalDays),
                "Số ngày gia hạn tối đa");

        saveOrUpdateSetting("Max_Books_Per_Member",
                String.valueOf(tierBorrowLimits.values().stream().mapToInt(Integer::intValue).max().orElse(1)),
                "Giới hạn mượn lớn nhất trong các hạng thành viên");

        saveOrUpdateSetting("Borrow_Fee_Per_Book",
                borrowFeePerBook.toPlainString(),
                "Phí mượn tiêu chuẩn cho mỗi quyển sách mỗi ngày");

        saveOrUpdateSetting("Fine_Per_Day",
                finePerDay.toPlainString(),
                "Phí phạt trễ hạn tính theo ngày cho mỗi cuốn sách");

        saveOrUpdateSetting("Damage_Compensation_Amount",
                damageCompensationAmount.toPlainString(),
                "Phí bồi thường khi sách hư hỏng nặng");

        saveOrUpdateSetting("Damage_Compensation_Threshold",
                String.valueOf(damageCompensationThreshold),
                "Ngưỡng phần trăm hư hỏng để tính phí bồi thường");

        saveOrUpdateSetting("Overdue_Violation_Lock_Limit",
                String.valueOf(overdueViolationLockLimit),
                "Số lần quá hạn tối đa trước khi khóa tài khoản thành viên");

        saveOrUpdateSetting("Book_Disposal_Condition_Threshold",
                String.valueOf(bookDisposalConditionThreshold),
                "Ngưỡng tình trạng sách cho phép thanh lý");

        saveOrUpdateSetting("Deposit_Amount",
                depositAmount.toPlainString(),
                "Tiền cọc đặt trước một quyển sách");

        auditLogService.log(
                ActionType.UPDATE_SETTINGS,
                "Cập nhật chính sách mượn/trả, phí phạt và cấu hình hạng thành viên.");
    }

    private void validateAndUpdateTiers(Map<Integer, Integer> tierBorrowLimits,
                                        Map<Integer, BigDecimal> tierSpendingConditions) {
        List<MembershipTier> tiers = getMembershipTiers();
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("Chưa có hạng thành viên để cấu hình.");
        }

        for (MembershipTier tier : tiers) {
            Integer borrowLimit = tierBorrowLimits == null ? null : tierBorrowLimits.get(tier.getTierId());
            validatePositive(borrowLimit,
                    "Số sách tối đa của hạng " + tier.getTierName() + " phải lớn hơn 0.");
            BigDecimal spendingCondition = tierSpendingConditions == null
                    ? null
                    : tierSpendingConditions.get(tier.getTierId());
            validateZeroOrPositive(spendingCondition,
                    "Mức chi tiêu của hạng " + tier.getTierName() + " không được âm.");
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
            throw new IllegalArgumentException(message);
        }
    }

    private void validateZeroOrPositive(Integer value, String message) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePercentage(Integer value, String message) {
        if (value == null || value < 0 || value > 100) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateZeroOrPositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
