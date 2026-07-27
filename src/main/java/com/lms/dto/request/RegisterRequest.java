package com.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "{validation.usernameRequired}")
    @Pattern(regexp = "^(?:|\\S{3,20})$", message = "{validation.username}")
    private String username;
    @NotBlank(message = "{backend.account.passwordRequired}")
    @Pattern(regexp = "^$|^.{6,}$", message = "{validation.passwordMin}")
    @Pattern(regexp = "^\\S*$", message = "{validation.passwordNoSpaces}")
    @Size(max = 50, message = "{validation.passwordMax}")
    private String password;
    @NotBlank(message = "{validation.confirmPasswordRequired}")
    @Pattern(regexp = "^$|^.{6,}$", message = "{validation.passwordMin}")
    @Pattern(regexp = "^\\S*$", message = "{validation.passwordNoSpaces}")
    @Size(max = 50, message = "{validation.passwordMax}")
    private String confirmPassword;
    @NotBlank(message = "{validation.fullNameRequired}")
    @Size(max = 50, message = "{validation.fullNameMax}")
    @Pattern(regexp = "^$|^[\\p{L}]+(?:\\s+[\\p{L}]+)*$", message = "{validation.fullNameLetters}")
    private String fullName;
    @NotBlank(message = "{validation.emailRequired}")
    @Email(message = "{validation.email}")
    @Size(max = 255, message = "{validation.emailMax}")
    private String email;
    @NotBlank(message = "{validation.phoneRequired}")
    @Pattern(regexp = "^$|^(0|\\+84)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-46-9])\\d{7}$", message = "{backend.profile.phoneFormat}")
    private String phone;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password, String confirmPassword, String fullName, String email, String phone) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.fullName = fullName == null ? null : fullName.trim();
        this.email = email == null ? null : email.trim();
        this.phone = phone == null ? null : phone.trim();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.trim();
    }
}
