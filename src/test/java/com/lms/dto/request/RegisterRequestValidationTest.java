package com.lms.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidVietnameseRegistrationData() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    @Test
    void acceptsSpecialCharactersInUsername() {
        RegisterRequest request = validRequest();
        request.setUsername("reader@2026!");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsWhitespaceInUsername() {
        RegisterRequest request = validRequest();
        request.setUsername(" reader@2026! ");

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("username")));
    }
    @Test
    void rejectsInvalidRegistrationFields() {
        RegisterRequest request = validRequest();
        request.setFullName("Nguyen Van 123");
        request.setUsername("ab");
        request.setEmail("invalid-email");
        request.setPhone("0123456789");
        request.setPassword("secret word");
        request.setConfirmPassword("");

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void trimsFieldsBeforeValidation() {
        RegisterRequest request = validRequest();
        request.setFullName("  Nguyễn Văn An  ");
        request.setEmail("  an@example.com  ");
        request.setPhone("  0912345678  ");

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("Nguyễn Văn An", request.getFullName());
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest(
                "nguyenvanan",
                "secret123",
                "secret123",
                "Nguyễn Văn An",
                "an@example.com",
                "0912345678");
    }
}