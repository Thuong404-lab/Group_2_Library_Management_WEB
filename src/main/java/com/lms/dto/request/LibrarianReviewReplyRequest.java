package com.lms.dto.request;

import com.lms.util.ReviewPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class LibrarianReviewReplyRequest {

    @NotBlank(message = "{backend.librarian.reviewReply.required}")
    @Size(min = ReviewPolicy.CONTENT_MIN_LENGTH, max = ReviewPolicy.CONTENT_MAX_LENGTH,
            message = "{backend.librarian.reviewReply.range}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{backend.librarian.reviewReply.letters}")
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
