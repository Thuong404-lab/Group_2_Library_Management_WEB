package com.lms.enums;

import java.util.Arrays;
import java.util.Optional;

public enum TransactionStatus {
    PENDING("Pending", "transaction.status.pending", "status-pending"),
    COMPLETED("Completed", "transaction.status.completed", "status-completed"),
    PAID("Paid", "transaction.status.completed", "status-completed"),
    FAILED("Failed", "transaction.status.failed", "status-failed"),
    CANCELLED("Cancelled", "transaction.status.canceled", "status-canceled"),
    EXPIRED("Expired", "transaction.status.expired", "status-expired"),
    REFUNDED("Refunded", "transaction.status.refunded", "status-refunded");

    private final String databaseValue;
    private final String messageKey;
    private final String cssClass;

    TransactionStatus(String databaseValue, String messageKey, String cssClass) {
        this.databaseValue = databaseValue;
        this.messageKey = messageKey;
        this.cssClass = cssClass;
    }

    public String getDatabaseValue() { return databaseValue; }
    public String getMessageKey() { return messageKey; }
    public String getCssClass() { return cssClass; }

    public static Optional<TransactionStatus> fromValue(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return Arrays.stream(values())
                .filter(status -> status.databaseValue.equalsIgnoreCase(value.trim())
                        || status.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
