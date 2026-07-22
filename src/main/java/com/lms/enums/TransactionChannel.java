package com.lms.enums;

import java.util.Arrays;
import java.util.Optional;

public enum TransactionChannel {
    WALLET("transaction.channel.wallet"),
    CASH("transaction.channel.cash"),
    PAYOS("transaction.channel.payos"),
    SYSTEM("transaction.channel.system");

    private final String messageKey;

    TransactionChannel(String messageKey) { this.messageKey = messageKey; }
    public String getMessageKey() { return messageKey; }

    public static Optional<TransactionChannel> fromCode(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        return Arrays.stream(values()).filter(channel -> channel.name().equalsIgnoreCase(value.trim())).findFirst();
    }
}
