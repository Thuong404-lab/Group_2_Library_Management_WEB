package com.lms.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApplicationException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "error.unauthorized.title", message);
    }
}
