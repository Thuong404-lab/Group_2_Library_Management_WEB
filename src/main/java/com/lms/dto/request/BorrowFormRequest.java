package com.lms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BorrowFormRequest {

    @NotNull(message = "Vui lòng chọn sách cần mượn")
    private Integer bookId;

    @NotNull(message = "Vui lòng nhập số ngày mượn")
    @Min(value = 1, message = "Số ngày mượn tối thiểu là 1 ngày")
    @Max(value = 90, message = "Số ngày mượn tối đa không quá 90 ngày")
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