package com.lms.exception;

import org.springframework.http.HttpStatus;
public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "error.notFound.title", message);
    }
}
