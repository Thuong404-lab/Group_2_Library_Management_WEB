package com.lms.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayNumberFormatterTest {

    private final DisplayNumberFormatter formatter = new DisplayNumberFormatter();

    @Test
    void formatsMoneyWithStableCommaGrouping() {
        assertThat(formatter.money(new BigDecimal("245000"))).isEqualTo("245,000");
    }

    @Test
    void keepsZeroVisibleForCountsAndAggregateMoney() {
        assertThat(formatter.count(null)).isEqualTo("0");
        assertThat(formatter.moneyOrZero(null)).isEqualTo("0");
    }

    @Test
    void usesEmDashForMissingNonAggregateValues() {
        assertThat(formatter.number(null)).isEqualTo("—");
        assertThat(formatter.money(null)).isEqualTo("—");
    }

    @Test
    void formatsPercentWithoutUnnecessaryTrailingZeros() {
        assertThat(formatter.percent(new BigDecimal("12.50"))).isEqualTo("12.5%");
    }
}
