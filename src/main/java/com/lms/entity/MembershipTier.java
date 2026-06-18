package com.lms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "MembershipTiers")
public class MembershipTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    private Integer id;

    @Column(name = "tier_name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "max_borrow_books", nullable = false)
    private Integer maxBorrowBooks = 5;

    @Column(name = "borrow_fee_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal borrowFeeRate = BigDecimal.ONE;

    @Column(length = 500)
    private String description;

    public MembershipTier(BigDecimal borrowFeeRate, Integer maxBorrowBooks, String name, Integer id, String description) {
        this.borrowFeeRate = borrowFeeRate;
        this.maxBorrowBooks = maxBorrowBooks;
        this.name = name;
        this.id = id;
        this.description = description;
    }

    public MembershipTier() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxBorrowBooks() {
        return maxBorrowBooks;
    }

    public void setMaxBorrowBooks(Integer maxBorrowBooks) {
        this.maxBorrowBooks = maxBorrowBooks;
    }

    public BigDecimal getBorrowFeeRate() {
        return borrowFeeRate;
    }

    public void setBorrowFeeRate(BigDecimal borrowFeeRate) {
        this.borrowFeeRate = borrowFeeRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
