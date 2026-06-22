package com.lms.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "Borrows")
public class Borrow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer borrowId;
    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;
    private LocalDateTime borrowDate;
    @Column(length = 50)
    private String status = "Active";

    public Borrow() {
    }

    public Borrow(Integer borrowId, Member member, Staff staff, LocalDateTime borrowDate, String status) {
        this.borrowId = borrowId;
        this.member = member;
        this.staff = staff;
        this.borrowDate = borrowDate;
        this.status = status;
    }

    public Integer getBorrowId() { return borrowId; }
    public void setBorrowId(Integer borrowId) { this.borrowId = borrowId; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
