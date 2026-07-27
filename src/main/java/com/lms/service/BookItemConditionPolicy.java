package com.lms.service;

import java.util.Locale;

/**
 * Centralizes the circulation rule derived from a physical copy's condition.
 */
public final class BookItemConditionPolicy {
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_UNAVAILABLE = "Unavailable";

    private BookItemConditionPolicy() {
    }

    public static boolean isLendable(String condition) {
        String normalized = condition == null ? "" : condition.trim().toLowerCase(Locale.ROOT);
        return !normalized.contains("severely")
                && !normalized.contains("severe damage")
                && !normalized.contains("hư hỏng nặng")
                && !normalized.contains("lost")
                && !normalized.contains("mất sách");
    }

    public static String circulationStatus(String condition) {
        return isLendable(condition) ? STATUS_AVAILABLE : STATUS_UNAVAILABLE;
    }
}
