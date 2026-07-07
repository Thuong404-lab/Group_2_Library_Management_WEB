package com.lms.dto.response;

import java.time.LocalDateTime;

public class ReturnRequestDTO {
    private Integer id; // borrowDetailId hoặc borrowId tùy cấu trúc hiển thị, ở đây map với req.id
    private String memberName;
    private String memberEmail;
    private String bookTitle;
    private String barcode;
    private LocalDateTime borrowDate;

    public ReturnRequestDTO() {}

    public ReturnRequestDTO(Integer id, String memberName, String memberEmail, String bookTitle, String barcode, LocalDateTime borrowDate) {
        this.id = id;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.bookTitle = bookTitle;
        this.barcode = barcode;
        this.borrowDate = borrowDate;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMemberEmail() { return memberEmail; }
    public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }
}