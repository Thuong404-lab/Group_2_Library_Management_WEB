package com.lms.enums;

public enum ReservationStatus {
    PENDING, APPROVED, READY, COMPLETED, CANCELED, EXPIRED;

    public boolean isActive() {
        return this == PENDING || this == APPROVED || this == READY;
    }

    public String getDisplayName() {
        return switch (this) {
            case PENDING   -> "Đang chờ";
            case APPROVED  -> "Đã duyệt";
            case READY     -> "Sẵn sàng nhận";
            case COMPLETED -> "Hoàn thành";
            case CANCELED  -> "Đã hủy";
            case EXPIRED   -> "Đã hết hạn";
        };
    }
}
