package com.lms.util;

import org.springframework.stereotype.Component;

/** Formats the canonical, display-only code for a persisted borrow record. */
@Component("borrowCodeFormatter")
public class BorrowCodeFormatter {
    private static final String PREFIX = "BOR-";

    public BorrowCodeFormatter() {
    }

    public static String format(Integer borrowId) {
        if (borrowId == null || borrowId < 1) {
            return "";
        }
        return PREFIX + borrowId;
    }
}