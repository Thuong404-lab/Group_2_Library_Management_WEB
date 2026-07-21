package com.lms.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsbnUtilsTest {

    @Test
    void normalizesAndValidatesIsbn10AndIsbn13() {
        assertEquals("9780134685991", IsbnUtils.normalize(" 978-0-13-468599-1 "));
        assertTrue(IsbnUtils.isValid("9780134685991"));
        assertTrue(IsbnUtils.isValid("0134685997"));
        assertTrue(IsbnUtils.isValid("080442957X"));
    }

    @Test
    void rejectsBadChecksumsAndUnsupportedInput() {
        assertFalse(IsbnUtils.isValid("9780134685992"));
        assertFalse(IsbnUtils.isValid("0134685998"));
        assertFalse(IsbnUtils.isValid("not-an-isbn"));
        assertNull(IsbnUtils.normalize("   "));
    }
}
