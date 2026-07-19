package com.lms.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {
    private String field;

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "error.conflict.title", message);
    }

    public ConflictException(String field, String message) {
        super(HttpStatus.CONFLICT, "error.conflict.title", message);
        this.field = field;
    }

    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "error.conflict.title", message, cause);
    }

    public String getField() {
        return field;
    }
}
