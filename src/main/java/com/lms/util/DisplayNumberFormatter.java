package com.lms.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Centralizes display-only number formatting for Thymeleaf views.
 * It deliberately uses a stable comma grouping separator in both supported locales.
 */
@Component("displayNumber")
public class DisplayNumberFormatter {

    private static final String MISSING_VALUE = "—";

    public String number(Number value) {
        return value == null ? MISSING_VALUE : decimalFormat("#,##0.##").format(value);
    }

    public String count(Number value) {
        return decimalFormat("#,##0").format(value == null ? 0 : value);
    }

    public String money(Number value) {
        return value == null ? MISSING_VALUE : decimalFormat("#,##0").format(value);
    }

    public String moneyOrZero(Number value) {
        return decimalFormat("#,##0").format(value == null ? BigDecimal.ZERO : value);
    }

    public String percent(Number value) {
        return value == null ? MISSING_VALUE : decimalFormat("#,##0.##").format(value) + "%";
    }

    private DecimalFormat decimalFormat(String pattern) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat formatter = new DecimalFormat(pattern, symbols);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        formatter.setGroupingUsed(true);
        return formatter;
    }
}
