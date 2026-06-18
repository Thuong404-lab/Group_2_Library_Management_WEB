package com.lms.entity;

import com.lms.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Borrows")

public class LoanRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "borrow_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id", nullable = false)
    private BookCopy copy;

    @CreationTimestamp
    @Column(name = "borrow_date", updatable = false)
    private LocalDateTime borrowDate;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "renewal_count")
    private Integer renewalCount = 0;

    @Column(name = "borrow_fee", precision = 12, scale = 2)
    private BigDecimal borrowFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private LoanStatus status = LoanStatus.BORROWED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    public LoanRecord(Integer id, BookCopy copy, User member, LocalDateTime borrowDate, LocalDateTime dueDate, LocalDateTime returnDate, Integer renewalCount, BigDecimal borrowFee, LoanStatus status, User processedBy) {
        this.id = id;
        this.copy = copy;
        this.member = member;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.renewalCount = renewalCount;
        this.borrowFee = borrowFee;
        this.status = status;
        this.processedBy = processedBy;
    }

    public LoanRecord() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getMember() {
        return member;
    }

    public void setMember(User member) {
        this.member = member;
    }

    public BookCopy getCopy() {
        return copy;
    }

    public void setCopy(BookCopy copy) {
        this.copy = copy;
    }

    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDateTime borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public Integer getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(Integer renewalCount) {
        this.renewalCount = renewalCount;
    }

    public BigDecimal getBorrowFee() {
        return borrowFee;
    }

    public void setBorrowFee(BigDecimal borrowFee) {
        this.borrowFee = borrowFee;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }
}
