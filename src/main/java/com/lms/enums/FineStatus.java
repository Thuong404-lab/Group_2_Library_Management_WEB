package com.lms.enums;

public enum FineStatus {
    UNPAID, PAID;

    public String getDisplayName() {
        return switch (this) {
            case UNPAID -> "Chưa thanh toán";
            case PAID   -> "Đã thanh toán";
        };
    }
}
