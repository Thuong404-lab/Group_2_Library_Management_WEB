package com.lms.exception;

import org.springframework.http.HttpStatus;

public class DataProcessingException extends ApplicationException {
    public DataProcessingException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý dữ liệu", message);
    }

    public DataProcessingException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xử lý dữ liệu", message, cause);
    }
}
