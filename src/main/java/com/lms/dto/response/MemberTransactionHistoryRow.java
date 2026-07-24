package com.lms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberTransactionHistoryRow(
        String code,
        LocalDateTime occurredAt,
        String typeLabel,
        String description,
        String detail,
        BigDecimal amount,
        String statusLabel,
        boolean completed) {
}
