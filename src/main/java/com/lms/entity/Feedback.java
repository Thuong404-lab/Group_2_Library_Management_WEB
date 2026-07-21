package com.lms.entity;
import com.lms.enums.FeedbackStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Column(name = "librarian_response", columnDefinition = "NVARCHAR(MAX)")
    private String librarianResponse;

    @Column(name = "response_date")
    private LocalDateTime responseDate;

    @Column(name = "moderation_reason", length = 500)
    private String moderationReason;

    @Column(name = "moderated_date")
    private LocalDateTime moderatedDate;

    @Version
    @Column(nullable = false)
    private Long version;

    public Feedback() {
    }

    public Feedback(Integer feedbackId, Member member, Book book, Integer rating, String comment, LocalDateTime createdDate, FeedbackStatus status) {
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
    public FeedbackStatus getStatus() {return status;}
    public void setStatus(FeedbackStatus status) {this.status = status;}
    public String getLibrarianResponse() {return librarianResponse;}
    public void setLibrarianResponse(String librarianResponse) {this.librarianResponse = librarianResponse;}
    public LocalDateTime getResponseDate() {return responseDate;}
    public void setResponseDate(LocalDateTime responseDate) {this.responseDate = responseDate;}
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public LocalDateTime getModeratedDate() { return moderatedDate; }
    public void setModeratedDate(LocalDateTime moderatedDate) { this.moderatedDate = moderatedDate; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
