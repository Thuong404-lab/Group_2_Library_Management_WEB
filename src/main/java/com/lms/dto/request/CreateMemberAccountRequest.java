package com.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreateMemberAccountRequest {

    @NotBlank(message = "Họ tên không được để trống.")
    @Pattern(regexp = "^[\\p{L}]+(?:\\s+[\\p{L}]+)*$",
            message = "Họ tên chỉ được chứa chữ cái và khoảng trắng.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    @Pattern(
            regexp = "^[A-Za-z0-9]+(?:[._%+\\-][A-Za-z0-9]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9\\-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$",
            message = "Email không đúng định dạng.")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(
            regexp = "^(?!0{10}$)0\\d{9}$",
            message = "Số điện thoại phải gồm đúng 10 chữ số, bắt đầu bằng số 0 và không được toàn số 0.")
    private String phone;

    @NotBlank(message = "Username không được để trống.")
    @Pattern(regexp = "^(?:|[a-zA-Z0-9_]{3,20})$", message = "Username phải từ 3-20 ký tự, không chứa khoảng trắng và ký tự đặc biệt!")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Pattern(regexp = "^(?:|.{6,})$", message = "Mật khẩu phải có ít nhất 6 ký tự!")
    private String password;

    @NotNull(message = "Vui lòng chọn hạng thành viên.")
    private Integer tierId;

    @NotBlank(message = "Vui lòng chọn trạng thái thành viên.")
    private String status;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    public Integer getTierId() {
        return tierId;
    }

    public void setTierId(Integer tierId) {
        this.tierId = tierId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
