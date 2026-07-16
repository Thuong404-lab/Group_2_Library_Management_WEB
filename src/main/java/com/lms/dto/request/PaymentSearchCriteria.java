package com.lms.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentSearchCriteria(String keyword, String status, String purpose,
                                    LocalDate fromDate, LocalDate toDate) {
    public String normalizedKeyword() { return keyword == null ? "" : keyword.trim(); }
    public String normalizedStatus() { return status == null ? "" : status.trim(); }
    public String normalizedPurpose() { return purpose == null ? "" : purpose.trim(); }
    public Long orderCode() {
        try { return normalizedKeyword().isEmpty() ? null : Long.valueOf(normalizedKeyword()); }
        catch (NumberFormatException ignored) { return null; }
    }
    public LocalDateTime fromDateTime() { return fromDate == null ? null : fromDate.atStartOfDay(); }
    public LocalDateTime toDateTimeExclusive() { return toDate == null ? null : toDate.plusDays(1).atStartOfDay(); }
}
