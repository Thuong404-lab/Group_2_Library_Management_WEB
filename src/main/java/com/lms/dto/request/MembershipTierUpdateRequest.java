package com.lms.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Fields an administrator is allowed to change on an existing membership tier. */
public class MembershipTierUpdateRequest {
    @NotNull(message = "{backend.tier.notFound}")
    private Integer tierId;
    @NotNull(message = "{backend.tier.versionRequired}")
    private Long version;
    @NotNull(message = "{backend.tier.discountRequired}")
    @DecimalMin(value = "0", message = "{backend.tier.discountRange}")
    @DecimalMax(value = "100", message = "{backend.tier.discountRange}")
    @Digits(integer = 3, fraction = 2, message = "{backend.tier.discountPrecision}")
    private BigDecimal discountPercent;
    @NotNull(message = "{backend.tier.borrowLimitMin}")
    @Min(value = 1, message = "{backend.tier.borrowLimitMin}")
    @Max(value = 100, message = "{backend.tier.borrowLimitMax}")
    private Integer borrowLimit;
    @NotNull(message = "{backend.tier.conditionRequired}")
    @DecimalMin(value = "0", message = "{backend.tier.conditionNonNegative}")
    @Digits(integer = 16, fraction = 2, message = "{backend.tier.conditionPrecision}")
    private BigDecimal condition;

    public Integer getTierId() { return tierId; }
    public void setTierId(Integer tierId) { this.tierId = tierId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public Integer getBorrowLimit() { return borrowLimit; }
    public void setBorrowLimit(Integer borrowLimit) { this.borrowLimit = borrowLimit; }
    public BigDecimal getCondition() { return condition; }
    public void setCondition(BigDecimal condition) { this.condition = condition; }
}
