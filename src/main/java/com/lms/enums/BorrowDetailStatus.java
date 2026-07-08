package com.lms.enums;

public enum BorrowDetailStatus {
    BORROWED,
    RETURN_PENDING, // Thêm trạng thái này cho việc chờ duyệt trả
    RETURNED,
    OVERDUE
}