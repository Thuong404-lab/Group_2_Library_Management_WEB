package com.lms.enums;

/**
 * Trang thai tai khoan nguoi dung.
 * BR-11: Khoa sau 3 lan vi pham qua han.
 * BR-12: Blocked mat quyen su dung dich vu.
 */
public enum UserStatus {
    ACTIVE,
    DISABLED,
    BLOCKED;

    public boolean canBorrow() {
        return this == ACTIVE;
    }

    public boolean isBlocked() {
        return this == BLOCKED;
    }

    public String getDisplayName() {
        return switch (this) {
            case ACTIVE   -> "Đang hoạt động";
            case DISABLED -> "Đã vô hiệu hóa";
            case BLOCKED  -> "Đã bị khóa";
        };
    }

    public String getBadgeClass() {
        return switch (this) {
            case ACTIVE   -> "badge-success";
            case DISABLED -> "badge-secondary";
            case BLOCKED  -> "badge-danger";
        };
    }
}
