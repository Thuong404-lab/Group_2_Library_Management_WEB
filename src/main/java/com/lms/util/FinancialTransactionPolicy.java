package com.lms.util;

import java.util.List;

/** Shared financial reporting rules. */
public final class FinancialTransactionPolicy {
    public static final String COMPLETED_STATUS = "Completed";
    public static final String REFUND_TYPE = "REFUND";
    public static final List<String> REVENUE_TYPES = List.of(
            "BORROW_FEE", "RENEWAL_FEE", "FINE", "DAMAGE_FEE",
            "PAYMENT", "OVERDUE_FINE", "FEE");

    private FinancialTransactionPolicy() {
    }
}
