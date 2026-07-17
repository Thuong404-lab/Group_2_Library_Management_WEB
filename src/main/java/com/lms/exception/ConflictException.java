package com.lms.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "error.conflict.title", message);
    }

    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "error.conflict.title", message, cause);
    }
}
