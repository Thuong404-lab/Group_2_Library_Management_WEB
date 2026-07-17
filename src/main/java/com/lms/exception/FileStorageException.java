package com.lms.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends ApplicationException {
    public FileStorageException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "error.fileStorage.title", message, cause);
    }
}
