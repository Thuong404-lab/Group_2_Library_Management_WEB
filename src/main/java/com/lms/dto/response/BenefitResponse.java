package com.lms.dto.response;

import java.math.BigDecimal;

public class BenefitResponse {
    private String tierName;
    private BigDecimal discountPercent;
    private Integer borrowLimit;
    private String benefitsDescription;

    public BenefitResponse(String tierName, BigDecimal discountPercent, Integer borrowLimit, String benefitsDescription) {
        this.tierName = tierName;
        this.discountPercent = discountPercent;
        this.borrowLimit = borrowLimit;
        this.benefitsDescription = benefitsDescription;
    }

    // Getters và Setters
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public Integer getBorrowLimit() { return borrowLimit; }
    public void setBorrowLimit(Integer borrowLimit) { this.borrowLimit = borrowLimit; }
    public String getBenefitsDescription() { return benefitsDescription; }
    public void setBenefitsDescription(String benefitsDescription) { this.benefitsDescription = benefitsDescription; }
}