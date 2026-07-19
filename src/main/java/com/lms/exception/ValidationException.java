package com.lms.exception;

import org.springframework.http.HttpStatus;
public class ValidationException extends ApplicationException {
    private String field;

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "error.validation.title", message);
    }

    public ValidationException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, "error.validation.title", message);
        this.field = field;
    }

    public ValidationException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "error.validation.title", message, cause);
    }

    public String getField() {
        return field;
    }
}
