package com.lms.dto.request;

public class AdminMemberAccountCreateRequest {
    private final String fullName;
    private final String email;
    private final String phone;
    private final String username;
    private final String password;

    public AdminMemberAccountCreateRequest(String fullName, String email, String phone,
            String username, String password) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.password = password;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
