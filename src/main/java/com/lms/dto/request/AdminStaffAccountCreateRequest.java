package com.lms.dto.request;

public class AdminStaffAccountCreateRequest {
    private final String fullName;
    private final String email;
    private final String phone;
    private final String username;
    private final String password;
    private final String staffType;

    public AdminStaffAccountCreateRequest(String fullName, String email, String phone,
            String username, String password, String staffType) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.password = password;
        this.staffType = staffType;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getStaffType() { return staffType; }
}
