package com.lms.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AccountFormValidationException extends ValidationException {
    private final Map<String, String> fieldErrors;

    public AccountFormValidationException(Map<String, String> fieldErrors) {
        super(fieldErrors == null || fieldErrors.isEmpty()
                ? "Account form is invalid."
                : fieldErrors.values().iterator().next());
        this.fieldErrors = fieldErrors == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
