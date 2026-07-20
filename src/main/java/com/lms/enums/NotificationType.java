package com.lms.enums;

import org.springframework.context.i18n.LocaleContextHolder;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public enum NotificationType {
    GENERAL("notification.type.general", true),
    ANNOUNCEMENT("notification.type.announcement", true),
    MAINTENANCE("notification.type.maintenance", true),
    EVENT("notification.type.event", true),
    REMINDER("notification.type.reminder", true),
    LOAN("notification.type.loan", false),
    RESERVATION("notification.type.reservation", false),
    FINANCE("notification.type.finance", false),
    REVIEW("notification.type.review", false),
    ACQUISITION("notification.type.acquisition", false);

    private final String messageKey;
    private final boolean manualSelectable;

    NotificationType(String messageKey, boolean manualSelectable) {
        this.messageKey = messageKey;
        this.manualSelectable = manualSelectable;
    }

    public String getDisplayName() {
        return ResourceBundle.getBundle("messages", LocaleContextHolder.getLocale()).getString(messageKey);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public boolean isManualSelectable() {
        return manualSelectable;
    }

    public static List<NotificationType> manualSelectableValues() {
        return Arrays.stream(values())
                .filter(NotificationType::isManualSelectable)
                .toList();
    }
}
