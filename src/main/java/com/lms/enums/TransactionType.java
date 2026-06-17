package com.lms.enums;

public enum TransactionType {
    TOP_UP, PAY_FINE, DEPOSIT, BORROW_FEE, REFUND;

    public String getDisplayName() {
        return switch (this) {
            case TOP_UP     -> "Nạp tiền";
            case PAY_FINE   -> "Đóng phạt";
            case DEPOSIT    -> "Đặt cọc";
            case BORROW_FEE -> "Phí mượn sách";
            case REFUND     -> "Hoàn tiền";
        };
    }
}
