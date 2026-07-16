package com.lms.enums;

public enum NotificationType {
    GENERAL("Thông báo chung"),
    ANNOUNCEMENT("Thông báo quan trọng"),
    MAINTENANCE("Bảo trì hệ thống"),
    EVENT("Sự kiện / chương trình"),
    REMINDER("Nhắc nhở");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
