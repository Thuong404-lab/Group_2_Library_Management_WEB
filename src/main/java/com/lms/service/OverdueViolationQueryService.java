package com.lms.service;

import com.lms.dto.response.OverdueViolationView;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Member;
import com.lms.entity.User;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds a read-only list of active overdue loans for the librarian fine desk.
 * It intentionally does not change loan status or create fines automatically.
 */
@Service
public class OverdueViolationQueryService {

    private final LocalizedMessageService messages;
    private static final String FINE_PER_DAY_SETTING_KEY = "Fine_Per_Day";
    private static final BigDecimal DEFAULT_FINE_PER_DAY = BigDecimal.valueOf(5000);
    private static final Set<String> OVERDUE_ELIGIBLE_STATUSES =
            Set.of("BORROWED", "OVERDUE", "RETURN_PENDING");

    private final BorrowDetailRepository borrowDetailRepository;
    private final SystemSettingRepository systemSettingRepository;

    public OverdueViolationQueryService(BorrowDetailRepository borrowDetailRepository, SystemSettingRepository systemSettingRepository, LocalizedMessageService messages) {
        this.borrowDetailRepository = borrowDetailRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<OverdueViolationView> getActiveOverdueViolations() {
        LocalDate today = LocalDate.now();
        BigDecimal finePerDay = getConfiguredFinePerDay();

        return borrowDetailRepository.findAllBorrowDetailsWithRelationships().stream()
                .filter(detail -> isActiveOverdue(detail, today))
                .map(detail -> toView(detail, today, finePerDay))
                .sorted(Comparator.comparingLong(OverdueViolationView::overdueDays)
                        .reversed()
                        .thenComparing(OverdueViolationView::dueDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getConfiguredFinePerDay() {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(FINE_PER_DAY_SETTING_KEY)
                    .map(setting -> new BigDecimal(setting.getSettingValue()))
                    .filter(amount -> amount.signum() >= 0)
                    .orElse(DEFAULT_FINE_PER_DAY);
        } catch (NumberFormatException exception) {
            return DEFAULT_FINE_PER_DAY;
        }
    }

    private boolean isActiveOverdue(BorrowDetail detail, LocalDate today) {
        if (detail == null
                || detail.getDueDate() == null
                || detail.getReturnDate() != null
                || detail.getBorrow() == null
                || detail.getBorrow().getMember() == null) {
            return false;
        }

        String status = detail.getStatus() == null
                ? ""
                : detail.getStatus().trim().toUpperCase(Locale.ROOT);

        return OVERDUE_ELIGIBLE_STATUSES.contains(status)
                && detail.getDueDate().toLocalDate().isBefore(today);
    }

    private OverdueViolationView toView(
            BorrowDetail detail,
            LocalDate today,
            BigDecimal finePerDay) {
        Member member = detail.getBorrow().getMember();
        User user = member.getUser();
        long overdueDays = Math.max(
                ChronoUnit.DAYS.between(detail.getDueDate().toLocalDate(), today),
                1L);

        return new OverdueViolationView(
                detail.getBorrowDetailId(),
                member.getMemberId(),
                user == null || isBlank(user.getFullName()) ? messages.get("common.member") : user.getFullName(),
                user == null ? "" : valueOrEmpty(user.getEmail()),
                user == null ? "" : valueOrEmpty(user.getPhone()),
                detail.getBook() == null || isBlank(detail.getBook().getTitle())
                        ? messages.get("backend.book.unknownTitle")
                        : detail.getBook().getTitle(),
                detail.getBookItem() == null ? "" : valueOrEmpty(detail.getBookItem().getBarcode()),
                detail.getDueDate(),
                overdueDays,
                finePerDay,
                finePerDay.multiply(BigDecimal.valueOf(overdueDays)),
                valueOrEmpty(detail.getStatus()));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
