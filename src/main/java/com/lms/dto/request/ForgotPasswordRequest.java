package com.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * ForgotPasswordRequest - DTO cho yêu cầu quên mật khẩu
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Getter
@Setter
public class ForgotPasswordRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}
