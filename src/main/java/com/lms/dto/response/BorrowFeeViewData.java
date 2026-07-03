package com.lms.dto.response;

import java.math.BigDecimal;

public class BorrowFeeViewData {
    private Integer borrowId;
    private Integer quantity; // số quyển
    private Integer days; // số ngày dự kiến
    private BigDecimal amount; // tổng phí

    public Integer getBorrowId() {
        return borrowId;
    }

    public void setBorrowId(Integer borrowId) {
        this.borrowId = borrowId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

