package com.lms.enums;

public enum LoanStatus {
    BORROWED, RETURNED, OVERDUE, LOST;

    public boolean isActive() {
        return this == BORROWED || this == OVERDUE;
    }

    public boolean canRenew() {
        return this == BORROWED || this == OVERDUE;
    }

    public String getDisplayName() {
        return switch (this) {
            case BORROWED -> "Đang mượn";
            case RETURNED -> "Đã trả";
            case OVERDUE  -> "Quá hạn";
            case LOST     -> "Đã mất";
        };
    }
}
