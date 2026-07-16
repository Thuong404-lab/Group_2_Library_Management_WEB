package com.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class LibrarianReviewReplyRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    @Size(min = 5, max = 1000, message = "Nội dung phản hồi phải có từ 5 đến 1000 ký tự")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "Nội dung phản hồi không được chỉ gồm số hoặc ký tự đặc biệt")
    private String response;

    public LibrarianReviewReplyRequest() {
    }

    public LibrarianReviewReplyRequest(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
