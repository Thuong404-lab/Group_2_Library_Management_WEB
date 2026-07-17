package com.lms.exception;

import org.springframework.http.HttpStatus;

public class DataProcessingException extends ApplicationException {
    public DataProcessingException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "error.dataProcessing.title", message);
    }

    public DataProcessingException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "error.dataProcessing.title", message, cause);
    }
}
