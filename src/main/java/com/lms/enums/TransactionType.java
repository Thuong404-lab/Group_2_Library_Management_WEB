package com.lms.enums;

import java.util.Arrays;
import java.util.Optional;

public enum TransactionType {
    TOP_UP("transaction.type.topUp", "type-topup"),
    BORROW_FEE("transaction.type.borrowFee", "type-borrow-fee"),
    DEPOSIT("transaction.type.deposit", "type-deposit"),
    FINE("transaction.type.fine", "type-fine"),
    DAMAGE_FEE("transaction.type.damageFee", "type-damage-fee"),
    RENEWAL_FEE("transaction.type.renewalFee", "type-renewal-fee"),
    REFUND("transaction.type.refund", "type-refund");

    private final String messageKey;
    private final String cssClass;

    TransactionType(String messageKey, String cssClass) {
        this.messageKey = messageKey;
        this.cssClass = cssClass;
    }

    public String getMessageKey() { return messageKey; }
    public String getCssClass() { return cssClass; }

    public static Optional<TransactionType> fromCode(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return Arrays.stream(values()).filter(type -> type.name().equalsIgnoreCase(value.trim())).findFirst();
    }
}
