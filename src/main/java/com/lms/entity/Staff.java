package com.lms.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "Staff")
public class Staff {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer staffId;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 50)
    private String staffType;

    public Staff(Integer staffId, User user, String staffType) {
        this.staffId = staffId;
        this.user = user;
        this.staffType = staffType;
    }

    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getStaffType() { return staffType; }
    public void setStaffType(String staffType) { this.staffType = staffType; }
}
