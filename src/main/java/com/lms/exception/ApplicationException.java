package com.lms.exception;

import org.springframework.http.HttpStatus;

public abstract class ApplicationException extends RuntimeException {
    private final HttpStatus status;
    private final String errorTitle;

    protected ApplicationException(HttpStatus status, String errorTitle, String message) {
        super(message);
        this.status = status;
        this.errorTitle = errorTitle;
    }

    protected ApplicationException(HttpStatus status, String errorTitle, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorTitle = errorTitle;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorTitle() { return errorTitle; }
}
