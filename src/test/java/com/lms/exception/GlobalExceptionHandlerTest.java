package com.lms.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void memberValidationErrorRemainsBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/member/cart/submit");

        Object result = handler.handleBadRequest(new IllegalArgumentException("invalid"), request);

        ModelAndView view = assertInstanceOf(ModelAndView.class, result);
        assertEquals(HttpStatus.BAD_REQUEST, view.getStatus());
    }

    @Test
    void memberMissingResourceRemainsNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/member/interaction/notifications/999");

        Object result = handler.handleNotFound(new NoSuchElementException(), request);

        ModelAndView view = assertInstanceOf(ModelAndView.class, result);
        assertEquals(HttpStatus.NOT_FOUND, view.getStatus());
    }
}
