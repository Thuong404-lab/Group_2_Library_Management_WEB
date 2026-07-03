package com.lms.service.impl;

import com.lms.entity.SystemLog;
import com.lms.entity.SystemSetting;
import com.lms.entity.MembershipTier;
import com.lms.repository.MembershipTierRepository;
import com.lms.repository.SystemLogRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.service.SystemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public SystemServiceImpl(SystemSettingRepository systemSettingRepository,
                             SystemLogRepository systemLogRepository,
                             MembershipTierRepository membershipTierRepository) {
        this.systemSettingRepository = systemSettingRepository;
        this.systemLogRepository = systemLogRepository;
        this.membershipTierRepository = membershipTierRepository;
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

        if (!trimmedAction.isEmpty() || !trimmedKeyword.isEmpty()) {
            return systemLogRepository.searchLogs(trimmedAction, trimmedKeyword, pageRequest);
        }

        return systemLogRepository.findAllByOrderByCreatedAtDesc(pageRequest);
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
    public List<MembershipTier> getMembershipTiers() {
        return membershipTierRepository.findAll(Sort.by("tierId").ascending());
    }

    @Override
    @Transactional
    public void updateBorrowingPolicies(Integer maxBorrowDays,
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
                                        Double depositAmount) {

        validatePositive(maxBorrowDays, "Số ngày mượn tối đa phải lớn hơn 0.");
        validateZeroOrPositive(maxRenewals, "Số lần gia hạn tối đa không được âm.");
        validateAndUpdateTierBorrowLimits(tierBorrowLimits);
        validateZeroOrPositive(borrowFeePerBook, "Phí mượn sách không được âm.");
        validateZeroOrPositive(finePerDay, "Phí phạt quá hạn không được âm.");
        validateZeroOrPositive(damageCompensationAmount, "Phí bồi thường không được âm.");
        validateZeroOrPositive(damageCompensationThreshold, "Ngưỡng hư hỏng không được âm.");
        validateZeroOrPositive(overdueViolationLockLimit, "Số lần quá hạn trước khi khóa không được âm.");
        validateZeroOrPositive(bookDisposalConditionThreshold, "Ngưỡng thanh lý sách không được âm.");
        validateZeroOrPositive(loyalUpgradeSpendingThreshold, "Điều kiện nâng hạng không được âm.");
        validatePositive(standardTierId, "Mã hạng Standard phải lớn hơn 0.");
        validatePositive(loyalTierId, "Mã hạng Loyal phải lớn hơn 0.");
        validateZeroOrPositive(depositAmount, "Tiền cọc không được âm.");

        saveOrUpdateSetting("Max_Borrow_Days",
                String.valueOf(maxBorrowDays),
                "Số ngày mượn sách tối đa tiêu chuẩn");

        saveOrUpdateSetting("Max_Renewals",
                String.valueOf(maxRenewals),
                "Số lần gia hạn tối đa");

        saveOrUpdateSetting("Max_Books_Per_Member",
                String.valueOf(tierBorrowLimits.values().stream().mapToInt(Integer::intValue).max().orElse(1)),
                "Giới hạn mượn lớn nhất trong các hạng thành viên");

        saveOrUpdateSetting("Borrow_Fee_Per_Book",
                String.valueOf(borrowFeePerBook),
                "Phí mượn tiêu chuẩn cho mỗi quyển sách mỗi ngày");

        saveOrUpdateSetting("Fine_Per_Day",
                String.valueOf(finePerDay),
                "Phí phạt trễ hạn tính theo ngày cho mỗi cuốn sách");

        saveOrUpdateSetting("Damage_Compensation_Amount",
                String.valueOf(damageCompensationAmount),
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

        saveOrUpdateSetting("Loyal_Upgrade_Spending_Threshold",
                String.valueOf(loyalUpgradeSpendingThreshold),
                "Tổng chi tiêu tối thiểu để nâng hạng Loyal Member");

        saveOrUpdateSetting("Standard_Tier_Id",
                String.valueOf(standardTierId),
                "Hạng Regular được dùng tạm như Standard Member");

        saveOrUpdateSetting("Loyal_Tier_Id",
                String.valueOf(loyalTierId),
                "Hạng Diamond được dùng tạm như Loyal Member");

        saveOrUpdateSetting("Deposit_Amount",
                String.valueOf(depositAmount),
                "Tiền cọc đặt trước một quyển sách");
    }

    private void validateAndUpdateTierBorrowLimits(Map<Integer, Integer> tierBorrowLimits) {
        List<MembershipTier> tiers = getMembershipTiers();
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("Chưa có hạng thành viên để cấu hình.");
        }

        for (MembershipTier tier : tiers) {
            Integer borrowLimit = tierBorrowLimits == null ? null : tierBorrowLimits.get(tier.getTierId());
            validatePositive(borrowLimit,
                    "Số sách tối đa của hạng " + tier.getTierName() + " phải lớn hơn 0.");
            tier.setBorrowLimit(borrowLimit);
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

    private void validateZeroOrPositive(Double value, String message) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
