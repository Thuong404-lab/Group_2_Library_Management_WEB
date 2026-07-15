package com.lms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only view used by the librarian fine desk.
 * Maintained by Pham Kien Quoc for the fine-management flow.
 */
public record OverdueViolationView(
        Integer borrowDetailId,
        Integer memberId,
        String memberName,
        String email,
        String phone,
        String bookTitle,
        String barcode,
        LocalDateTime dueDate,
        long overdueDays,
        BigDecimal finePerDay,
        BigDecimal suggestedFineAmount,
        String status) {
}
