package com.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "{validation.reset.tokenRequired}")
    private String token;

    @NotBlank(message = "{validation.reset.newPasswordRequired}")
    @Size(min = 6, message = "{validation.passwordMin}")
    private String newPassword;

    @NotBlank(message = "{validation.reset.confirmRequired}")
    private String confirmPassword;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String token, String newPassword, String confirmPassword) {
        this.token = token;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
