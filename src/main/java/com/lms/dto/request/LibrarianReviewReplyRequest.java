package com.lms.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LibrarianReviewReplyRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
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