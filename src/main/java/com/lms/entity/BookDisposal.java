package com.lms.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "BookDisposals")
public class BookDisposal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer disposalId;
    @ManyToOne
    @JoinColumn(name = "book_item_id", nullable = false)
    private BookItem bookItem;
    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String reason;
    private LocalDateTime disposalDate;
    @Column(length = 50)
    private String status = "Completed";

    public BookDisposal() {
    }

    public BookDisposal(Integer disposalId, BookItem bookItem, Staff staff, String reason, LocalDateTime disposalDate, String status) {
        this.disposalId = disposalId;
        this.bookItem = bookItem;
        this.staff = staff;
        this.reason = reason;
        this.disposalDate = disposalDate;
        this.status = status;
    }

    public Integer getDisposalId() { return disposalId; }
    public void setDisposalId(Integer disposalId) { this.disposalId = disposalId; }
    public BookItem getBookItem() { return bookItem; }
    public void setBookItem(BookItem bookItem) { this.bookItem = bookItem; }
    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getDisposalDate() { return disposalDate; }
    public void setDisposalDate(LocalDateTime disposalDate) { this.disposalDate = disposalDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
