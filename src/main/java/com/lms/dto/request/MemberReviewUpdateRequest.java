package com.lms.dto.request;

import com.lms.util.ReviewPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class MemberReviewUpdateRequest {

    @NotNull(message = "{validation.review.ratingRequired}")
    @Min(value = 1, message = "{validation.review.ratingMinimum}")
    @Max(value = 5, message = "{validation.review.ratingMaximum}")
    private Integer rating;

    @NotBlank(message = "{backend.review.contentRequired}")
    @Size(min = ReviewPolicy.CONTENT_MIN_LENGTH, max = ReviewPolicy.CONTENT_MAX_LENGTH,
            message = "{backend.review.contentRange}")
    @Pattern(regexp = "(?s).*\\p{L}.*", message = "{backend.review.contentLetters}")
    private String comment;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
