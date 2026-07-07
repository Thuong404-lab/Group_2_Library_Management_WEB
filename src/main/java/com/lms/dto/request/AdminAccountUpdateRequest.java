package com.lms.dto.request;

public class AdminAccountUpdateRequest {
    private final Integer accountId;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String username;
    private final Integer tierId;
    private final String staffType;
    private final String status;
    private final String source;

    public AdminAccountUpdateRequest(Integer accountId,
            String fullName,
            String email,
            String phone,
            String username,
            Integer tierId,
            String staffType,
            String status,
            String source) {
        this.accountId = accountId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.tierId = tierId;
        this.staffType = staffType;
        this.status = status;
        this.source = source;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getUsername() {
        return username;
    }

    public Integer getTierId() {
        return tierId;
    }

    public String getStaffType() {
        return staffType;
    }

    public String getStatus() {
        return status;
    }

    public String getSource() {
        return source;
    }
}
