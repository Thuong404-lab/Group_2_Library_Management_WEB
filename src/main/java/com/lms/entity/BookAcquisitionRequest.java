package com.lms.entity;

import com.lms.enums.AcquisitionRequestStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;

@Entity
@Table(name = "BookAcquisitionRequests", schema = "dbo")
public class BookAcquisitionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(length = 255)
    private String author;

    @Column(length = 20)
    private String isbn;

    @Column(length = 255)
    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    // Kept nullable at database level so Hibernate can add the column to
    // installations that already contain legacy acquisition requests.
    // New submissions still require this field through DTO validation.
    @Column(name = "request_reason", length = 1000)
    private String requestReason;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(nullable = false, length = 20)
    private AcquisitionRequestStatus status = AcquisitionRequestStatus.PENDING;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    public BookAcquisitionRequest() {
    }

    public BookAcquisitionRequest(Integer requestId, Member member, String title, LocalDateTime createdDate, String author) {
        this.requestId = requestId;
        this.member = member;
        this.title = title;
        this.createdDate = createdDate;
        this.author = author;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }
    public String getRequestReason() { return requestReason; }
    public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
    public String getReferenceUrl() { return referenceUrl; }
    public void setReferenceUrl(String referenceUrl) { this.referenceUrl = referenceUrl; }
    public AcquisitionRequestStatus getStatus() { return status; }
    public void setStatus(AcquisitionRequestStatus status) { this.status = status; }
    public String getDecisionNote() { return decisionNote; }
    public void setDecisionNote(String decisionNote) { this.decisionNote = decisionNote; }
    public LocalDateTime getProcessedDate() { return processedDate; }
    public void setProcessedDate(LocalDateTime processedDate) { this.processedDate = processedDate; }
}
