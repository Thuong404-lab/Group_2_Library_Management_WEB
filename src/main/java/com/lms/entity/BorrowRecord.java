package com.lms.entity;

import com.lms.enums.BorrowStatus;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;
import java.util.List;

public class BorrowRecord {
    private Long id;

    private String memberEmail; // Lưu email member để xác định ai mượn
    private LocalDate borrowDate;
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private BorrowStatus status; // PENDING, APPROVED, RETURNED, REJECTED

    @ElementCollection
    private List<String> bookBarcodes; // Danh sách mã vạch sách mượn

    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public void setBorrowDate(LocalDate borrowDate) {
        this.borrowDate = borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BorrowStatus getStatus() {
        return status;
    }

    public void setStatus(BorrowStatus status) {
        this.status = status;
    }

    public List<String> getBookBarcodes() {
        return bookBarcodes;
    }

    public void setBookBarcodes(List<String> bookBarcodes) {
        this.bookBarcodes = bookBarcodes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
