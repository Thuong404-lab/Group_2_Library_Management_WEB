package com.lms.dto.request;

public class AdminAccountCreateRequest {
    private final String fullName;
    private final String email;
    private final String phone;
    private final String username;
    private final String password;
    private final String accountType;
    private final Integer tierId;
    private final String status;

    public AdminAccountCreateRequest(String fullName,
            String email,
            String phone,
            String username,
            String password,
            String accountType,
            Integer tierId,
            String status) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.password = password;
        this.accountType = accountType;
        this.tierId = tierId;
        this.status = status;
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

    public String getPassword() {
        return password;
    }

    public String getAccountType() {
        return accountType;
    }

    public Integer getTierId() {
        return tierId;
    }

    public String getStatus() {
        return status;
    }
}
