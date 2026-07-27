package com.lms.enums;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public enum BorrowListStatusFilter {
    ACTIVE("Active", "librarian.borrowList.status.active"),
    WAITING_PICKUP("Waiting_Pickup", "librarian.borrowList.status.pickup"),
    RETURNED("Returned", "librarian.borrowList.status.returned"),
    OVERDUE("Overdue", "librarian.borrowList.status.overdue"),
    PENDING("Pending", "librarian.borrowList.status.pending"),
    REJECTED("Rejected", "librarian.borrowList.status.rejected"),
    RETURN_PENDING("Return_Pending", "librarian.borrowList.status.returnPending"),
    PAYMENT_PENDING("Payment_Pending", "loan.status.paymentPending"),
    PAYMENT_EXPIRED("Payment_Expired", "loan.status.paymentExpired"),
    CANCELED("Canceled", "librarian.borrowMember.status.canceled", "Cancelled");

    private final String value;
    private final String messageKey;
    private final Set<String> acceptedValues;

    BorrowListStatusFilter(String value, String messageKey, String... aliases) {
        this.value = value;
        this.messageKey = messageKey;
        this.acceptedValues = new LinkedHashSet<>();
        this.acceptedValues.add(value);
        this.acceptedValues.addAll(Arrays.asList(aliases));
    }

    public String getValue() {
        return value;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public static Optional<BorrowListStatusFilter> fromRequestValue(String requestValue) {
        if (requestValue == null || requestValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = requestValue.trim();
        return Arrays.stream(values())
                .filter(option -> option.acceptedValues.contains(normalized))
                .findFirst();
    }
}