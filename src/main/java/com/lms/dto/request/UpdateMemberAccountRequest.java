package com.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateMemberAccountRequest {

        @NotBlank(message = "{validation.fullNameRequired}")
        @Pattern(regexp = "^$|^[\\p{L}]+(?:\\s+[\\p{L}]+)*$", message = "{validation.fullNameLetters}")
        @Pattern(regexp = "^$|^[\\p{L}]{1,15}(?:\\s+[\\p{L}]{1,15}){0,7}$", message = "{validation.fullNameWords}")
        @Pattern(regexp = "^$|^(?!.*([\\p{L}])\\1\\1).*$", message = "{validation.fullNameTriple}")
        @Pattern(regexp = "^$|^(?!([\\p{L}])\\1+$).+$", message = "{validation.fullNameRepeated}")
        @Size(max = 50, message = "{validation.fullNameMax}")
        private String fullName;

        @NotBlank(message = "{validation.usernameRequired}")
        @Pattern(regexp = "^(?:|[a-zA-Z0-9_]{3,20})$", message = "{validation.username}")
        private String username;

        @NotBlank(message = "{validation.emailRequired}")
        @Email(message = "{validation.email}")
        private String email;

        @NotBlank(message = "{validation.phoneRequired}")
        @Pattern(regexp = "^$|^(?!0{10}$)0\\d{9}$", message = "{validation.phone}")
        private String phone;

        @NotNull(message = "{validation.tier}")
        private Integer tierId;

        @NotBlank(message = "{validation.status}")
        private String status;

        public String getFullName() {
                return fullName;
        }

        public void setFullName(String fullName) {
                this.fullName = fullName == null ? null : fullName.trim();
        }

        public String getUsername() {
                return username;
        }

        public void setUsername(String username) {
                this.username = username == null ? null : username.trim();
        }

        public String getEmail() {
                return email;
        }

        public void setEmail(String email) {
                this.email = email;
        }

        public String getPhone() {
                return phone;
        }

        public void setPhone(String phone) {
                this.phone = phone;
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
