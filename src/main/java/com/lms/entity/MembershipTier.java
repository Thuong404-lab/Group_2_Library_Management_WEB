package com.lms.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
@Table(name = "MembershipTiers")
public class MembershipTier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tierId;
    @Column(nullable = false, length = 100)
    private String tierName;
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercent;
    private Integer borrowLimit;
    @Column(precision = 18, scale = 2, name = "[condition]")
    private BigDecimal condition;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String benefits;

    public MembershipTier() {
    }

    public MembershipTier(Integer tierId, String tierName, BigDecimal discountPercent, Integer borrowLimit, BigDecimal condition, String benefits) {
        this.tierId = tierId;
        this.tierName = tierName;
        this.discountPercent = discountPercent;
        this.borrowLimit = borrowLimit;
        this.condition = condition;
        this.benefits = benefits;
    }

    public Integer getTierId() { return tierId; }
    public void setTierId(Integer tierId) { this.tierId = tierId; }
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public Integer getBorrowLimit() { return borrowLimit; }
    public void setBorrowLimit(Integer borrowLimit) { this.borrowLimit = borrowLimit; }
    public BigDecimal getCondition() { return condition; }
    public void setCondition(BigDecimal condition) { this.condition = condition; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }
}
