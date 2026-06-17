package com.lms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "MembershipTiers")
@Getter
@Setter
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
}
