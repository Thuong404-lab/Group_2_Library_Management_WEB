package com.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UpdateMemberAccountRequest {

    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Username không được để trống.")
    @Pattern(
            regexp = "^(?:|[a-zA-Z0-9_]{3,20})$",
            message = "Username phải từ 3-20 ký tự, chỉ gồm chữ cái, chữ số và dấu gạch dưới.")
    private String username;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = "0\\d{9}",
            message = "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0.")
    private String phone;

    @NotNull(message = "Hạng thành viên không hợp lệ.")
    private Integer tierId;

    @NotBlank(message = "Trạng thái tài khoản không hợp lệ.")
    private String status;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getTierId() { return tierId; }
    public void setTierId(Integer tierId) { this.tierId = tierId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
