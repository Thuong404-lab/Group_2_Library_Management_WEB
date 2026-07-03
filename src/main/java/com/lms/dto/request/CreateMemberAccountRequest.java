package com.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreateMemberAccountRequest {

    @NotBlank(message = "Họ tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = "0\\d{9}",
            message = "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0.")
    private String phone;

    @NotBlank(message = "Username không được để trống.")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống.")
    private String password;

    @NotNull(message = "Vui lòng chọn hạng thành viên.")
    private Integer tierId;

    @NotBlank(message = "Vui lòng chọn trạng thái thành viên.")
    private String status;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getTierId() { return tierId; }
    public void setTierId(Integer tierId) { this.tierId = tierId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

