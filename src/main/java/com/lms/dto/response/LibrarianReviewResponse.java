package com.lms.dto.response;
import java.time.LocalDateTime;

public class LibrarianReviewResponse {
    private Integer feedbackId;
    private String bookTitle;
    private String memberName;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime createdDate;
    private String librarianResponse;
    private LocalDateTime responseDate;

    public LibrarianReviewResponse() {
    }

    public Integer getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Integer feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getLibrarianResponse() {
        return librarianResponse;
    }

    public void setLibrarianResponse(String librarianResponse) {
        this.librarianResponse = librarianResponse;
    }

    public LocalDateTime getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(LocalDateTime responseDate) {
        this.responseDate = responseDate;
    }
}
