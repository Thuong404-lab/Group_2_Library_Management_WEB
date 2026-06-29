package com.lms.dto.request;
import jakarta.validation.constraints.NotBlank;

public class LibrarianReviewModerateRequest {
    @NotBlank(message = "Hành động kiểm duyệt không được để trống")
    private String action; // "APPROVE" hoặc "REJECT"

    public LibrarianReviewModerateRequest() {
    }

    public LibrarianReviewModerateRequest(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
