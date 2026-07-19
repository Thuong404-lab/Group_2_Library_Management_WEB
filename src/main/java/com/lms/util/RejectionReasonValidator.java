package com.lms.util;

import com.lms.exception.ValidationException;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RejectionReasonValidator {
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "BORROW", Set.of("NO_COPY", "LIMIT_EXCEEDED", "ACCOUNT_RESTRICTED", "OUTSTANDING_OBLIGATION", "INVALID_INFORMATION", "OTHER"),
            "RENEWAL", Set.of("RESERVED_BY_OTHER", "OVERDUE", "RENEWAL_LIMIT_REACHED", "ACCOUNT_RESTRICTED", "BOOK_RECALL", "APPROVAL_EXPIRED", "RETURNED_BEFORE_APPROVAL", "OTHER"),
            "RESERVATION", Set.of("COPY_AVAILABLE", "UNFULFILLABLE", "DUPLICATE_REQUEST", "ACCOUNT_RESTRICTED", "INVALID_DEPOSIT", "OTHER")
    );

    private RejectionReasonValidator() {}

    public static RejectionReason validate(String flow, String code, String detail) {
        String normalizedFlow = normalize(flow);
        String normalizedCode = normalize(code);
        String normalizedDetail = detail == null ? "" : detail.trim().replaceAll("(?:\\r?\\n\\s*){3,}", "\n\n");
        if (!ALLOWED.getOrDefault(normalizedFlow, Set.of()).contains(normalizedCode)) {
            throw new ValidationException("Invalid rejection reason type.");
        }
        boolean otherReason = "OTHER".equals(normalizedCode);
        if (otherReason && normalizedDetail.isEmpty()) {
            throw new ValidationException("Rejection details are required when the reason type is Other.");
        }
        if (!normalizedDetail.isEmpty()
                && (normalizedDetail.length() < 5
                || normalizedDetail.length() > 500
                || normalizedDetail.chars().noneMatch(Character::isLetter))) {
            throw new ValidationException("When provided, rejection details must contain letters and be between 5 and 500 characters.");
        }
        return new RejectionReason(normalizedCode, normalizedDetail.isEmpty() ? null : normalizedDetail);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public record RejectionReason(String code, String detail) {}
}