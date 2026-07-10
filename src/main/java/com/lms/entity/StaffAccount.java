package com.lms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Staff_Accounts")
public class StaffAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 50)
    private String status = "Active";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "Staff_Account_Roles",
        joinColumns = @JoinColumn(name = "staff_account_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private java.util.Set<Role> roles = new java.util.HashSet<>();

    public StaffAccount() {
    }

    public StaffAccount(Integer id, Staff staff, String username, String passwordHash, String status) {
        this.id = id;
        this.staff = staff;
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    /**
     * Backward-compatible property used by existing admin views.
     */
    @Transient
    public Integer getAccountId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    /**
     * Exposes the user associated through the staff profile to legacy views.
     */
    @Transient
    public User getUser() {
        return staff != null ? staff.getUser() : null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.util.Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(java.util.Set<Role> roles) {
        this.roles = roles;
    }
}
