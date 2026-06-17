package com.lms.enums;

public enum FineType {
    OVERDUE, DAMAGED, LOST;

    public String getDisplayName() {
        return switch (this) {
            case OVERDUE -> "Quá hạn";
            case DAMAGED -> "Làm hỏng sách";
            case LOST    -> "Làm mất sách";
        };
    }
}
