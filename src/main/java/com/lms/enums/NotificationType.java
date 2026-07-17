package com.lms.enums;

import org.springframework.context.i18n.LocaleContextHolder;
import java.util.ResourceBundle;

public enum NotificationType {
    GENERAL("notification.type.general"),
    ANNOUNCEMENT("notification.type.announcement"),
    MAINTENANCE("notification.type.maintenance"),
    EVENT("notification.type.event"),
    REMINDER("notification.type.reminder");

    private final String messageKey;

    NotificationType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getDisplayName() {
        return ResourceBundle.getBundle("messages", LocaleContextHolder.getLocale()).getString(messageKey);
    }

    public String getMessageKey() {
        return messageKey;
    }
}
