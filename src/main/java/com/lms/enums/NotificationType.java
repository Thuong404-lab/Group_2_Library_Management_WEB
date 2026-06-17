package com.lms.enums;

public enum NotificationType {
    GENERAL, OVERDUE, RESERVATION_READY, TOP_UP, NEW_BOOK, SYSTEM;

    public String getDisplayName() {
        return switch (this) {
            case GENERAL           -> "Chung";
            case OVERDUE           -> "Trễ hạn";
            case RESERVATION_READY -> "Sách đã sẵn sàng";
            case TOP_UP            -> "Nạp tiền";
            case NEW_BOOK          -> "Sách mới";
            case SYSTEM            -> "Hệ thống";
        };
    }
}
