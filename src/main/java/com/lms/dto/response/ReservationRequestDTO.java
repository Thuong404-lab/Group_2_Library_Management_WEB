package com.lms.dto.response;

import java.time.LocalDateTime;

public class ReservationRequestDTO {
    private Integer id; // reservationId
    private String memberName;
    private String bookTitle;
    private LocalDateTime bookingDate;
    private Integer queuePosition;

    public ReservationRequestDTO() {}

    public ReservationRequestDTO(Integer id, String memberName, String bookTitle, LocalDateTime bookingDate, Integer queuePosition) {
        this.id = id;
        this.memberName = memberName;
        this.bookTitle = bookTitle;
        this.bookingDate = bookingDate;
        this.queuePosition = queuePosition;
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
}