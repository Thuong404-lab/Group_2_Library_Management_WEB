package com.lms.util;

import java.util.Locale;

public final class IsbnUtils {
    private IsbnUtils() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("[-\\s]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String isbn) {
        return isValidIsbn10(isbn) || isValidIsbn13(isbn);
    }

    private static boolean isValidIsbn10(String isbn) {
        if (isbn == null || !isbn.matches("\\d{9}[\\dX]")) return false;
        int sum = 0;
        for (int index = 0; index < 10; index++) {
            int digit = isbn.charAt(index) == 'X' ? 10 : isbn.charAt(index) - '0';
            sum += (10 - index) * digit;
        }
        return sum % 11 == 0;
    }

    private static boolean isValidIsbn13(String isbn) {
        if (isbn == null || !isbn.matches("\\d{13}")) return false;
        int sum = 0;
        for (int index = 0; index < 12; index++) {
            int digit = isbn.charAt(index) - '0';
            sum += digit * (index % 2 == 0 ? 1 : 3);
        }
        return (10 - sum % 10) % 10 == isbn.charAt(12) - '0';
    }
}
