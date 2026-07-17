package com.lms.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "Dữ liệu xung đột", message);
    }

    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "Dữ liệu xung đột", message, cause);
    }
}
