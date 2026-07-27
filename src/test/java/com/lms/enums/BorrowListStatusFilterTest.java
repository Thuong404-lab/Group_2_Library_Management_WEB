package com.lms.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorrowListStatusFilterTest {

    @Test
    void resolvesCanonicalStatusValue() {
        assertEquals(BorrowListStatusFilter.ACTIVE,
                BorrowListStatusFilter.fromRequestValue("Active").orElseThrow());
    }

    @Test
    void normalizesLegacyCancelledAlias() {
        assertEquals("Canceled",
                BorrowListStatusFilter.fromRequestValue("Cancelled").orElseThrow().getValue());
    }

    @Test
    void rejectsUnknownStatus() {
        assertTrue(BorrowListStatusFilter.fromRequestValue("Unknown_Status").isEmpty());
    }
}