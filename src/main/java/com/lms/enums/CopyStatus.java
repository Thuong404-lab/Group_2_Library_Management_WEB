package com.lms.enums;

/** BR-5: Chi muon khi co ban sao 'Available'. BR-22: Thanh ly khi condition_pct < 40%. */
public enum CopyStatus {
    AVAILABLE, BORROWED, RESERVED, LOST, DAMAGED;

    public boolean isAvailable() { return this == AVAILABLE; }
    public boolean canBeReturned() { return this == BORROWED; }

    public String getDisplayName() {
        return switch (this) {
            case AVAILABLE -> "Có sẵn";
            case BORROWED  -> "Đang mượn";
            case RESERVED  -> "Đã đặt trước";
            case LOST      -> "Mất";
            case DAMAGED   -> "Hư hỏng";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case AVAILABLE -> "badge-success";
            case BORROWED  -> "badge-warning";
            case RESERVED  -> "badge-info";
            case LOST      -> "badge-dark";
            case DAMAGED   -> "badge-danger";
        };
    }
}
