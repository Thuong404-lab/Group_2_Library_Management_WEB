package com.lms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "MemberDetails")
@Getter
@Setter
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
}
