package com.lms.dto.request;

import java.math.BigDecimal;

/** Fields an administrator is allowed to change on an existing membership tier. */
public class MembershipTierUpdateRequest {
    private Integer tierId;
    private BigDecimal discountPercent;
    private Integer borrowLimit;
    private BigDecimal condition;
    private String benefits;

    public Integer getTierId() { return tierId; }
    public void setTierId(Integer tierId) { this.tierId = tierId; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public Integer getBorrowLimit() { return borrowLimit; }
    public void setBorrowLimit(Integer borrowLimit) { this.borrowLimit = borrowLimit; }
    public BigDecimal getCondition() { return condition; }
    public void setCondition(BigDecimal condition) { this.condition = condition; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }
}
