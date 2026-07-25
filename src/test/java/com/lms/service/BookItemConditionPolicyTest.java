package com.lms.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookItemConditionPolicyTest {

    @Test
    void severelyDamagedAndLostCopiesAreUnavailable() {
        assertThat(BookItemConditionPolicy.circulationStatus("Severely damaged")).isEqualTo("Unavailable");
        assertThat(BookItemConditionPolicy.circulationStatus("Severe damage")).isEqualTo("Unavailable");
        assertThat(BookItemConditionPolicy.circulationStatus("Hư hỏng nặng")).isEqualTo("Unavailable");
        assertThat(BookItemConditionPolicy.circulationStatus("Lost book")).isEqualTo("Unavailable");
    }

    @Test
    void newAndMinorDamageCopiesRemainAvailable() {
        assertThat(BookItemConditionPolicy.circulationStatus("New")).isEqualTo("Available");
        assertThat(BookItemConditionPolicy.circulationStatus("Minor damage")).isEqualTo("Available");
    }
}
