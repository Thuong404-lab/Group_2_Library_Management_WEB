package com.lms.util;

/** Formats the canonical, display-only code for a persisted borrow record. */
public final class BorrowCodeFormatter {
    private static final String PREFIX = "BOR-";

    private BorrowCodeFormatter() {
    }

    public static String format(Integer borrowId) {
        if (borrowId == null || borrowId < 1) {
            throw new IllegalArgumentException("A positive borrow ID is required");
        }
        return PREFIX + borrowId;
    }
}