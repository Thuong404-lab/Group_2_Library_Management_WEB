package com.lms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BorrowFormRequest {

    @NotNull(message = "{validation.borrow.bookRequired}")
    private Integer bookId;

    @NotNull(message = "{validation.borrow.daysRequired}")
    @Min(value = 1, message = "{validation.borrow.daysMinimum}")
    @Max(value = 90, message = "{validation.borrow.daysMaximum}")
    private Integer numberOfDays; // Số ngày mượn mong muốn (Ví dụ mặc định: 14 ngày)

    public BorrowFormRequest() {
    }

    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public Integer getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(Integer numberOfDays) {
        this.numberOfDays = numberOfDays;
    }
}
