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
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    private Integer rating;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;
    private LocalDateTime createdDate;

    public Feedback(Integer feedbackId, Member member, Book book, Integer rating, String comment, LocalDateTime createdDate) {
        this.feedbackId = feedbackId;
        this.member = member;
        this.book = book;
        this.rating = rating;
        this.comment = comment;
        this.createdDate = createdDate;
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
}
