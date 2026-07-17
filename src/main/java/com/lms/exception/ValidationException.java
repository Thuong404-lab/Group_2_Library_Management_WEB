package com.lms.exception;

import org.springframework.http.HttpStatus;
public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", message);
    }

    public ValidationException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", message, cause);
    }
}
