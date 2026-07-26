package com.lms.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberReviewSubmitRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void pathBasedReviewDoesNotRequireHiddenBookId() {
        MemberReviewSubmitRequest request = validRequestWithoutBookId();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void standaloneReviewFormStillRequiresBookId() {
        MemberReviewSubmitRequest request = validRequestWithoutBookId();

        assertFalse(validator.validate(
                request, Default.class, MemberReviewSubmitRequest.RequiresBookId.class).isEmpty());
    }

    private MemberReviewSubmitRequest validRequestWithoutBookId() {
        MemberReviewSubmitRequest request = new MemberReviewSubmitRequest();
        request.setRating(5);
        request.setComment("A thoughtful and useful review.");
        return request;
    }
}
