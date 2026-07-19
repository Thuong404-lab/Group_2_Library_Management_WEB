package com.lms.dto.response;

import java.time.LocalDateTime;

public class ReservationRequestDTO {
    private Integer id; // reservationId
    private String memberName;
    private String bookTitle;
    private LocalDateTime bookingDate;
    private Integer queuePosition;
    
    // New fields for search
    private String memberEmail;
    private String memberPhone;
    private String memberUsername;

    public ReservationRequestDTO() {}

    public ReservationRequestDTO(Integer id, String memberName, String bookTitle, LocalDateTime bookingDate, Integer queuePosition) {
        this.id = id;
        this.memberName = memberName;
        this.bookTitle = bookTitle;
        this.bookingDate = bookingDate;
        this.queuePosition = queuePosition;
    }

    public ReservationRequestDTO(Integer id, String memberName, String bookTitle, LocalDateTime bookingDate, Integer queuePosition,
                                 String memberEmail, String memberPhone, String memberUsername) {
        this.id = id;
        this.memberName = memberName;
        this.bookTitle = bookTitle;
        this.bookingDate = bookingDate;
        this.queuePosition = queuePosition;
        this.memberEmail = memberEmail;
        this.memberPhone = memberPhone;
        this.memberUsername = memberUsername;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }
    public Integer getQueuePosition() { return queuePosition; }
    public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }

    public String getMemberEmail() { return memberEmail; }
    public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }
    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }
    public String getMemberUsername() { return memberUsername; }
    public void setMemberUsername(String memberUsername) { this.memberUsername = memberUsername; }
}