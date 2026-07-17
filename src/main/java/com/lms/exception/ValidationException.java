package com.lms.exception;

import org.springframework.http.HttpStatus;
public class ValidationException extends ApplicationException {
    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "error.validation.title", message);
    }

    public ValidationException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "error.validation.title", message, cause);
    }
}
