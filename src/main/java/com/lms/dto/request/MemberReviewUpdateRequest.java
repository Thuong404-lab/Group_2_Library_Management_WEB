package com.lms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MemberReviewUpdateRequest {

    @NotNull(message = "Vui lòng chọn đánh giá sao")
    @Min(value = 1, message = "Tối thiểu 1 sao")
    @Max(value = 5, message = "Tối đa 5 sao")
    private Integer rating;

    @NotBlank(message = "Vui lòng nhập nội dung đánh giá")
    @Size(max = 1000, message = "Nội dung đánh giá tối đa 1000 ký tự")
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
