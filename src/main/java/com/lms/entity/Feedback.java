package com.lms.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "Feedbacks")
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer feedbackId;
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private String status = "PENDING";

    public Feedback() {
    }

    public Feedback(Integer feedbackId, Member member, Book book, Integer rating, String comment, LocalDateTime createdDate, String status) {
        this.feedbackId = feedbackId;
        this.member = member;
        this.book = book;
        this.rating = rating;
        this.comment = comment;
        this.createdDate = createdDate;
        this.status = status;
    }

    public Integer getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Integer feedbackId) { this.feedbackId = feedbackId; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getStatus() {return status;}
    public void setStatus(String status) {this.status = status;}
}
