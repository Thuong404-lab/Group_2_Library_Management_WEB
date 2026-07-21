package com.lms.dto.response;

import com.lms.enums.UserStatus;

import java.math.BigDecimal;

public record TopUpMemberOption(
        Integer memberId,
        String fullName,
        String email,
        String phone,
        String userStatus,
        String accountStatus,
        String tierName,
        BigDecimal walletBalance) {

    public TopUpMemberOption(Integer memberId,
                             String fullName,
                             String email,
                             String phone,
                             UserStatus userStatus,
                             String accountStatus,
                             String tierName,
                             BigDecimal walletBalance) {
        this(memberId, fullName, email, phone,
                userStatus == null ? "" : userStatus.name(),
                accountStatus == null ? "" : accountStatus,
                tierName == null ? "" : tierName,
                walletBalance == null ? BigDecimal.ZERO : walletBalance);
    }
}
