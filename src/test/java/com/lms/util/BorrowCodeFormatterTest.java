package com.lms.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BorrowCodeFormatterTest {
    @Test
    void formatsCanonicalBorrowCode() {
        assertThat(BorrowCodeFormatter.format(42)).isEqualTo("BOR-42");
    }

    @Test
    void rejectsMissingOrInvalidIds() {
        assertThatIllegalArgumentException().isThrownBy(() -> BorrowCodeFormatter.format(null));
        assertThatIllegalArgumentException().isThrownBy(() -> BorrowCodeFormatter.format(0));
    }
}