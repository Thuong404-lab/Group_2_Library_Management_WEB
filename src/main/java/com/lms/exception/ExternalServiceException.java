package com.lms.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends ApplicationException {
    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, "error.external.title", message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "error.external.title", message, cause);
    }
}
