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

        logs.forEach(log -> {
            populateActorUsername(log);
            localizeLogDescription(log);
            convertCreatedAtToLocalTime(log);
        });
        return logs;
    }

    private static final java.time.ZoneId VIETNAM_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    private void convertCreatedAtToLocalTime(SystemLog log) {
        if (log != null && log.getCreatedAt() != null) {
            log.setCreatedAt(log.getCreatedAt()
                    .atZone(java.time.ZoneOffset.UTC)
                    .withZoneSameInstant(VIETNAM_ZONE)
                    .toLocalDateTime());
        }
    }

    private static final java.util.regex.Pattern PATTERN_CREATED_ACCOUNT_EN =
            java.util.regex.Pattern.compile("^Created account (.+) with account type (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_CREATED_ACCOUNT_VI =
            java.util.regex.Pattern.compile("^Tạo tài khoản (.+) với loại (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_CREATED_MEMBER_EN =
            java.util.regex.Pattern.compile("^Created member account (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_CREATED_MEMBER_VI =
            java.util.regex.Pattern.compile("^Tạo tài khoản thành viên (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_DEACTIVATED_STAFF_EN =
            java.util.regex.Pattern.compile("^Deactivated staff account (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_DEACTIVATED_STAFF_VI =
            java.util.regex.Pattern.compile("^Vô hiệu hóa tài khoản nhân sự (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_DEACTIVATED_MEMBER_EN =
            java.util.regex.Pattern.compile("^Deactivated member account (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_DEACTIVATED_MEMBER_VI =
            java.util.regex.Pattern.compile("^Vô hiệu hóa tài khoản thành viên (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_DELETED_ACCOUNT_EN =
            java.util.regex.Pattern.compile("^Deleted member account:?\\s*(.+)$");
    private static final java.util.regex.Pattern PATTERN_DELETED_ACCOUNT_VI =
            java.util.regex.Pattern.compile("^Đã xóa tài khoản thành viên:?\\s*(.+)$");

    private static final java.util.regex.Pattern PATTERN_UPDATED_MEMBER_EN =
            java.util.regex.Pattern.compile("^Updated member account (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_UPDATED_MEMBER_VI =
            java.util.regex.Pattern.compile("^Cập nhật tài khoản thành viên (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_STATUS_CHANGED_EN =
            java.util.regex.Pattern.compile("^Changed member account (\\S+) status from (.+?) to (.+?)\\.?$");
    private static final java.util.regex.Pattern PATTERN_STATUS_CHANGED_VI =
            java.util.regex.Pattern.compile("^Đã đổi trạng thái tài khoản thành viên (\\S+) từ (.+?) sang (.+?)\\.?$");

    private static final java.util.regex.Pattern PATTERN_UPDATED_STAFF_EN =
            java.util.regex.Pattern.compile("^Updated staff account (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_UPDATED_STAFF_VI =
            java.util.regex.Pattern.compile("^Cập nhật tài khoản nhân sự (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_UPDATED_TIER_EN =
            java.util.regex.Pattern.compile("^Updated membership tier (.+); synchronized (\\d+) member\\(s\\)\\.?$");
    private static final java.util.regex.Pattern PATTERN_UPDATED_TIER_VI =
            java.util.regex.Pattern.compile("^Đã cập nhật hạng (.+); đồng bộ (\\d+) thành viên\\.?$");

    private static final java.util.regex.Pattern PATTERN_MULTI_BORROW_EN =
            java.util.regex.Pattern.compile("^Member (.+) requested (\\d+) books: (.+), for (\\d+) days\\. Awaiting librarian approval\\.?$");
    private static final java.util.regex.Pattern PATTERN_MULTI_BORROW_VI =
            java.util.regex.Pattern.compile("^Độc giả (.+) đã đăng ký mượn (\\d+) cuốn sách: (.+) trong (\\d+) ngày\\. Đang chờ thủ thư phê duyệt\\.?$");

    private static final java.util.regex.Pattern PATTERN_SINGLE_BORROW_EN =
            java.util.regex.Pattern.compile("^Member (.+) requested book #(\\d+) - (.+) for (\\d+) days\\.?$");
    private static final java.util.regex.Pattern PATTERN_SINGLE_BORROW_VI =
            java.util.regex.Pattern.compile("^Thành viên (.+) gửi yêu cầu mượn sách #(\\d+) - (.+) trong (\\d+) ngày\\.?$");

    private static final java.util.regex.Pattern PATTERN_RESERVE_BOOK_EN =
            java.util.regex.Pattern.compile("^Member (.+) requested a reservation for book #(\\d+) - (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_RESERVE_BOOK_VI =
            java.util.regex.Pattern.compile("^Thành viên (.+) yêu cầu đặt trước sách #(\\d+) - (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_REQUEST_RENEWAL_EN =
            java.util.regex.Pattern.compile("^Member (.+) requested (\\d+) renewal days for detail #(\\d+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_REQUEST_RENEWAL_VI =
            java.util.regex.Pattern.compile("^Thành viên (.+) gửi yêu cầu gia hạn (\\d+) ngày cho bản ghi #(\\d+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_CANCEL_RENEWAL_EN =
            java.util.regex.Pattern.compile("^Member (.+) cancelled renewal request for detail #(\\d+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_CANCEL_RENEWAL_VI =
            java.util.regex.Pattern.compile("^Thành viên (.+) đã hủy yêu cầu gia hạn cho bản ghi #(\\d+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_REQUEST_RETURN_EN =
            java.util.regex.Pattern.compile("^Member (.+) submitted a return request for loan #(\\d+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_REQUEST_RETURN_VI =
            java.util.regex.Pattern.compile("^Thành viên (.+) gửi yêu cầu trả sách cho hợp đồng mượn #(\\d+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_CREATED_BOOK_EN =
            java.util.regex.Pattern.compile("^Added new book: (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_CREATED_BOOK_VI =
            java.util.regex.Pattern.compile("^Thêm sách mới: (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_UPDATED_BOOK_EN =
            java.util.regex.Pattern.compile("^Updated book details: (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_UPDATED_BOOK_VI =
            java.util.regex.Pattern.compile("^Cập nhật thông tin sách: (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_DELETED_BOOK_EN =
            java.util.regex.Pattern.compile("^Deleted book: (.+)\\.?$");
    private static final java.util.regex.Pattern PATTERN_DELETED_BOOK_VI =
            java.util.regex.Pattern.compile("^Xóa sách: (.+)\\.?$");

    private static final java.util.regex.Pattern PATTERN_UPDATE_SETTINGS_EN =
            java.util.regex.Pattern.compile("^Updated borrowing policies and fine settings\\.?$");
    private static final java.util.regex.Pattern PATTERN_UPDATE_SETTINGS_VI =
            java.util.regex.Pattern.compile("^Cập nhật chính sách mượn/trả và phí phạt\\.?$");

    private String extractBookTitle(String desc) {
        if (desc == null) return "";
        String s = desc.trim();
        if (s.startsWith("Added new book: ")) s = s.substring("Added new book: ".length());
        else if (s.startsWith("Thêm sách mới: ")) s = s.substring("Thêm sách mới: ".length());
        else if (s.startsWith("Updated book details: ")) s = s.substring("Updated book details: ".length());
        else if (s.startsWith("Cập nhật thông tin sách: ")) s = s.substring("Cập nhật thông tin sách: ".length());
        else if (s.startsWith("Deleted book: ")) s = s.substring("Deleted book: ".length());
        else if (s.startsWith("Xóa sách: ")) s = s.substring("Xóa sách: ".length());
        else if (s.startsWith("Tạo dữ liệu sách ")) s = s.substring("Tạo dữ liệu sách ".length());
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s.trim();
    }

    private String localizeStatusValue(String status) {
        if (status == null) return "";
        String s = status.trim();
        if ("Active".equalsIgnoreCase(s) || "Hoạt động".equalsIgnoreCase(s) || "Đang hoạt động".equalsIgnoreCase(s)) {
            return messages.get("account.status.active");
        }
        if ("Inactive".equalsIgnoreCase(s) || "Hạn chế".equalsIgnoreCase(s) || "Tạm dừng".equalsIgnoreCase(s) || "Không hoạt động".equalsIgnoreCase(s) || "Restricted".equalsIgnoreCase(s)) {
            return messages.get("member.account.status.restricted");
        }
        if ("Blocked".equalsIgnoreCase(s) || "Đã khóa".equalsIgnoreCase(s) || "Bị khóa".equalsIgnoreCase(s)) {
            return messages.get("account.status.blocked");
        }
        return s;
    }

    private String localizeAccountRole(String role) {
        if (role == null) return "";
        String r = role.trim().toUpperCase();
        if (r.contains("LIBRARIAN") || "THỦ THƯ".equals(r)) {
            return messages.get("role.librarian");
        }
        if (r.contains("MEMBER") || "THÀNH VIÊN".equals(r)) {
            return messages.get("role.member");
        }
        if (r.contains("ADMIN") || "QUẢN TRỊ VIÊN".equals(r)) {
            return messages.get("role.admin");
        }
        return role.trim();
    }

    private String localizeTierName(String tierName) {
        if (tierName == null) return "";
        String t = tierName.trim();
        if ("Regular".equalsIgnoreCase(t) || "Member".equalsIgnoreCase(t) || "Thường".equalsIgnoreCase(t) || "Thành viên".equalsIgnoreCase(t)) {
            return messages.get("tier.regular");
        }
        if ("Silver".equalsIgnoreCase(t) || "Bạc".equalsIgnoreCase(t)) {
            return messages.get("tier.silver");
        }
        if ("Gold".equalsIgnoreCase(t) || "Vàng".equalsIgnoreCase(t)) {
            return messages.get("tier.gold");
        }
        if ("Diamond".equalsIgnoreCase(t) || "Kim cương".equalsIgnoreCase(t)) {
            return messages.get("tier.diamond");
        }
        return t;
    }

    private void localizeLogDescription(SystemLog log) {
        if (log == null) {
            return;
        }

        String actionType = log.getActionType() == null ? "" : log.getActionType().trim();
        String desc = log.getDescription() == null ? "" : log.getDescription().trim();

        // 1. Fixed-text action types
        if ("UPDATE_SETTINGS".equalsIgnoreCase(actionType)) {
            log.setDescription(messages.get("backend.settings.audit.updated"));
            return;
        }
        if ("LOGIN".equalsIgnoreCase(actionType)) {
            log.setDescription(messages.get("backend.auth.audit.login"));
            return;
        }
        if ("LOGOUT".equalsIgnoreCase(actionType)) {
            log.setDescription(messages.get("backend.auth.audit.logout"));
            return;
        }
        if ("GOOGLE".equalsIgnoreCase(actionType)) {
            log.setDescription(messages.get("backend.auth.audit.google_login"));
            return;
        }

        // 2. Book CRUD actions fallback
        if ("CREATE_BOOK".equalsIgnoreCase(actionType)) {
            String title = extractBookTitle(desc);
            log.setDescription(messages.get("backend.inventory.audit.createdBook", title.isEmpty() ? "Demo" : title));
            return;
        }
        if ("UPDATE_BOOK".equalsIgnoreCase(actionType)) {
            String title = extractBookTitle(desc);
            log.setDescription(messages.get("backend.inventory.audit.updatedBook", title));
            return;
        }
        if ("DELETE_BOOK".equalsIgnoreCase(actionType)) {
            String title = extractBookTitle(desc);
            log.setDescription(messages.get("backend.inventory.audit.deletedBook", title));
            return;
        }

        if (desc.isEmpty()) {
            return;
        }

        // 3. Seed data exact matches
        if ("Kiểm tra cấu hình nghiệp vụ.".equalsIgnoreCase(desc) || "Kiểm tra cấu hình nghiệp vụ".equalsIgnoreCase(desc)) {
            log.setDescription(messages.get("backend.settings.audit.updated"));
            return;
        }
        if ("Tạo dữ liệu sách demo.".equalsIgnoreCase(desc) || "Tạo dữ liệu sách demo".equalsIgnoreCase(desc)) {
            log.setDescription(messages.get("backend.inventory.audit.createdBook", "Demo"));
            return;
        }
        if ("Quản trị viên đăng nhập.".equalsIgnoreCase(desc) || "Quản trị viên đăng nhập".equalsIgnoreCase(desc)) {
            log.setDescription(messages.get("backend.auth.audit.login"));
            return;
        }
        if ("Thành viên gửi yêu cầu mượn.".equalsIgnoreCase(desc) || "Thành viên gửi yêu cầu mượn".equalsIgnoreCase(desc)) {
            log.setDescription(messages.get("backend.borrow.audit.multiRequested", "Member", "1", "Demo", "14"));
            return;
        }
        if ("Thành viên gửi yêu cầu trả.".equalsIgnoreCase(desc) || "Thành viên gửi yêu cầu trả".equalsIgnoreCase(desc)) {
            log.setDescription(messages.get("backend.return.audit.requested", "Member", "1"));
            return;
        }
        if ("Thành viên đặt trước sách.".equalsIgnoreCase(desc) || "Thành viên đặt trước sách".equalsIgnoreCase(desc)) {
            log.setDescription(messages.get("backend.borrow.audit.reservationRequested", "Member", "1", "Demo"));
            return;
        }

        // 4. Regex patterns with flexible matching
        java.util.regex.Matcher m;

        m = PATTERN_MULTI_BORROW_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_MULTI_BORROW_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.borrow.audit.multiRequested", m.group(1), m.group(2), m.group(3), m.group(4)));
            return;
        }

        m = PATTERN_SINGLE_BORROW_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_SINGLE_BORROW_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.borrow.audit.requested", m.group(1), m.group(2), m.group(3), m.group(4)));
            return;
        }

        m = PATTERN_RESERVE_BOOK_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_RESERVE_BOOK_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.borrow.audit.reservationRequested", m.group(1), m.group(2), m.group(3)));
            return;
        }

        m = PATTERN_UPDATED_MEMBER_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_UPDATED_MEMBER_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.account.audit.updatedMember", m.group(1)));
            return;
        }

        m = PATTERN_UPDATED_STAFF_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_UPDATED_STAFF_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.account.audit.updatedStaff", m.group(1)));
            return;
        }

        m = PATTERN_CREATED_ACCOUNT_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_CREATED_ACCOUNT_VI.matcher(desc);
        if (m.matches()) {
            String roleName = localizeAccountRole(m.group(2));
            log.setDescription(messages.get("backend.account.audit.created", m.group(1), roleName));
            return;
        }

        m = PATTERN_CREATED_MEMBER_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_CREATED_MEMBER_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.account.audit.createdMember", m.group(1)));
            return;
        }

        m = PATTERN_DEACTIVATED_STAFF_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_DEACTIVATED_STAFF_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.account.audit.deactivatedStaff", m.group(1)));
            return;
        }

        m = PATTERN_DEACTIVATED_MEMBER_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_DEACTIVATED_MEMBER_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.account.audit.deactivatedMember", m.group(1)));
            return;
        }

        m = PATTERN_DELETED_ACCOUNT_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_DELETED_ACCOUNT_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.account.audit.deletedMember", m.group(1)));
            return;
        }

        m = PATTERN_STATUS_CHANGED_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_STATUS_CHANGED_VI.matcher(desc);
        if (m.matches()) {
            String oldStatus = localizeStatusValue(m.group(2));
            String newStatus = localizeStatusValue(m.group(3));
            log.setDescription(messages.get("backend.account.audit.statusChanged", m.group(1), oldStatus, newStatus));
            return;
        }

        m = PATTERN_UPDATED_TIER_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_UPDATED_TIER_VI.matcher(desc);
        if (m.matches()) {
            String localizedTier = localizeTierName(m.group(1));
            log.setDescription(messages.get("backend.tier.auditUpdated", localizedTier, m.group(2)));
            return;
        }

        m = PATTERN_REQUEST_RENEWAL_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_REQUEST_RENEWAL_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.renewal.audit.requested", m.group(1), m.group(2), m.group(3)));
            return;
        }

        m = PATTERN_CANCEL_RENEWAL_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_CANCEL_RENEWAL_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.renewal.audit.cancelled", m.group(1), m.group(2)));
            return;
        }

        m = PATTERN_REQUEST_RETURN_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_REQUEST_RETURN_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.return.audit.requested", m.group(1), m.group(2)));
            return;
        }

        m = PATTERN_CREATED_BOOK_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_CREATED_BOOK_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.inventory.audit.createdBook", m.group(1)));
            return;
        }

        m = PATTERN_UPDATED_BOOK_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_UPDATED_BOOK_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.inventory.audit.updatedBook", m.group(1)));
            return;
        }

        m = PATTERN_DELETED_BOOK_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_DELETED_BOOK_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.inventory.audit.deletedBook", m.group(1)));
            return;
        }

        m = PATTERN_UPDATE_SETTINGS_EN.matcher(desc);
        if (!m.matches()) m = PATTERN_UPDATE_SETTINGS_VI.matcher(desc);
        if (m.matches()) {
            log.setDescription(messages.get("backend.settings.audit.updated"));
            return;
        }
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
            BigDecimal renewalFeePerDay,
            BigDecimal damageCompensationAmount,
            Integer overdueViolationLockLimit,
            BigDecimal depositAmount) {

        validatePositive(maxBorrowDays, messages.get("backend.settings.maxBorrowDaysPositive"));
        validatePositive(maxRenewalDays, messages.get("backend.settings.maxRenewalDaysPositive"));
        validatePositive(maxRenewalRequests, messages.get("backend.settings.maxRenewalRequestsPositive"));
        validatePositive(renewalRejectionCooldownHours, messages.get("backend.settings.renewalCooldownPositive"));
        validatePositive(renewalApprovalTimeoutHours, messages.get("backend.settings.renewalCooldownPositive"));
        validateZeroOrPositive(borrowFeePerBook, messages.get("backend.settings.borrowFeeNonNegative"));
        validateZeroOrPositive(renewalFeePerDay, messages.get("backend.settings.renewalFeeNonNegative"));
        validateZeroOrPositive(damageCompensationAmount, messages.get("backend.settings.compensationNonNegative"));
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
        saveOrUpdateSetting("RENEWAL_FEE_PER_DAY",
                renewalFeePerDay.toPlainString(),
                messages.get("backend.settings.description.renewalFee"));
        saveOrUpdateSetting("New_Book_Overdue_Fine",
                borrowFeePerBook.multiply(BigDecimal.valueOf(2)).toPlainString(),
                messages.get("backend.settings.description.newBookOverdueFine"));

        saveOrUpdateSetting("Damage_Compensation_Amount",
                damageCompensationAmount.toPlainString(),
                messages.get("backend.settings.description.compensation"));

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

    private void validateZeroOrPositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(message);
        }
    }
}
