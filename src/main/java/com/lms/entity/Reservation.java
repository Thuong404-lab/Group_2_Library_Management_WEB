package com.lms.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "Reservations")
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reservationId;
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    private LocalDateTime reservationDate;
    @Column(length = 50)
    private String status = "Pending";
    @Column(name = "rejection_code", length = 50)
    private String rejectionCode;
    @Column(name = "rejection_reason", length = 500, columnDefinition = "nvarchar(500)")
    private String rejectionReason;

    public Reservation() {
    }

    public Reservation(Integer reservationId, Member member, Book book, LocalDateTime reservationDate, String status) {
        this.reservationId = reservationId;
        this.member = member;
        this.book = book;
        this.reservationDate = reservationDate;
        this.status = status;
    }

    public Integer getReservationId() { return reservationId; }
    public void setReservationId(Integer reservationId) { this.reservationId = reservationId; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public LocalDateTime getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDateTime reservationDate) { this.reservationDate = reservationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectionCode() { return rejectionCode; }
    public void setRejectionCode(String rejectionCode) { this.rejectionCode = rejectionCode; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
