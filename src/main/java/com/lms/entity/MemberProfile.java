package com.lms.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "MemberDetails")

public class MemberProfile {
    @Id
    @Column(name = "member_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "member_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private MembershipTier tier;

    @Column(precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "total_spending", precision = 12, scale = 2)
    private BigDecimal totalSpending = BigDecimal.ZERO;

    @Column(name = "last_borrow_date")
    private LocalDate lastBorrowDate;

    @Column(name = "join_date", updatable = false)
    private LocalDate joinDate = LocalDate.now();

    public MemberProfile(Integer id, User user, MembershipTier tier, BigDecimal balance, BigDecimal totalSpending, LocalDate lastBorrowDate, LocalDate joinDate) {
        this.id = id;
        this.user = user;
        this.tier = tier;
        this.balance = balance;
        this.totalSpending = totalSpending;
        this.lastBorrowDate = lastBorrowDate;
        this.joinDate = joinDate;
    }

    public MemberProfile() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getTotalSpending() {
        return totalSpending;
    }

    public void setTotalSpending(BigDecimal totalSpending) {
        this.totalSpending = totalSpending;
    }

    public LocalDate getLastBorrowDate() {
        return lastBorrowDate;
    }

    public void setLastBorrowDate(LocalDate lastBorrowDate) {
        this.lastBorrowDate = lastBorrowDate;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public MembershipTier getTier() {
        return tier;
    }

    public void setTier(MembershipTier tier) {
        this.tier = tier;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }
}
