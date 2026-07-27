package com.lms.controller.member;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartControllerValidationTest {

    @Test
    void selectionCannotRequestMoreCopiesThanCartContains() {
        assertFalse(CartController.selectionMatchesCart(Map.of(15, 3L), List.of(15)));
        assertTrue(CartController.selectionMatchesCart(Map.of(15, 2L), List.of(15, 15)));
    }

    @Test
    void externalRefererIsConvertedToLocalPath() {
        assertEquals("/phishing?source=cart",
                CartController.localRedirectTarget(
                        "https://external.example/phishing?source=cart", "/books/15"));
    }

    @Test
    void malformedRefererFallsBackToKnownPage() {
        assertEquals("/books/15",
                CartController.localRedirectTarget("://invalid uri", "/books/15"));
    }
}
