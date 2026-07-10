package com.lms.dto.response;

import java.time.LocalDateTime;

public class MemberBorrowDTO {
    private Integer id;
    private String bookTitle;
    private String authorName;
    private String bookImage;
    private String bookIdStr;
    private LocalDateTime actionDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate; // Dành cho tab Lịch sử
    private String status;
    private long daysLeft;
    private int progressPercentage;   // Dành cho thanh tiến trình ở Tab Đang mượn
    private int renewCount;

    public MemberBorrowDTO() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getBookImage() { return bookImage; }
    public void setBookImage(String bookImage) { this.bookImage = bookImage; }
    public String getBookIdStr() { return bookIdStr; }
    public void setBookIdStr(String bookIdStr) { this.bookIdStr = bookIdStr; }
    public LocalDateTime getActionDate() { return actionDate; }
    public void setActionDate(LocalDateTime actionDate) { this.actionDate = actionDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getDaysLeft() { return daysLeft; }
    public void setDaysLeft(long daysLeft) { this.daysLeft = daysLeft; }
    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }
    public int getRenewCount() { return renewCount; }
    public void setRenewCount(int renewCount) { this.renewCount = renewCount; }
}