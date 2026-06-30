package com.lms.dto.response;

import java.math.BigDecimal;

public class MemberDashboardProfileResponse {
    // Thống tin cá nhân (View Profile)
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private String status;

    // Thông tin hạng thẻ (View Membership Tier)
    private String tierName;
    private BigDecimal currentSpent; // Tổng chi tiêu tích lũy
    private BigDecimal amountNeededToNextTier; // Số tiền cần để lên hạng tiếp theo
    private String nextTierName; // Tên hạng tiếp theo
    private BigDecimal walletBalance; // Số dư ví hiện tại

    // Thông tin đặc quyền (View Benefits & Privileges)
    private Integer borrowLimit;
    private BigDecimal discountPercent;
    private String benefitsDescription;

    public MemberDashboardProfileResponse() {
    }

    // Getters and Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public BigDecimal getCurrentSpent() { return currentSpent; }
    public void setCurrentSpent(BigDecimal currentSpent) { this.currentSpent = currentSpent; }
    public BigDecimal getAmountNeededToNextTier() { return amountNeededToNextTier; }
    public void setAmountNeededToNextTier(BigDecimal amountNeededToNextTier) { this.amountNeededToNextTier = amountNeededToNextTier; }
    public String getNextTierName() { return nextTierName; }
    public void setNextTierName(String nextTierName) { this.nextTierName = nextTierName; }
    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    public Integer getBorrowLimit() { return borrowLimit; }
    public void setBorrowLimit(Integer borrowLimit) { this.borrowLimit = borrowLimit; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public String getBenefitsDescription() { return benefitsDescription; }
    public void setBenefitsDescription(String benefitsDescription) { this.benefitsDescription = benefitsDescription; }
}