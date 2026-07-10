package com.lms.dto.response;

import java.math.BigDecimal;

public class ReportMetric {
    private final String label;
    private final long count;
    private final BigDecimal amount;

    public ReportMetric(String label, long count) {
        this(label, count, BigDecimal.ZERO);
    }

    public ReportMetric(String label, long count, BigDecimal amount) {
        this.label = label;
        this.count = count;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public String getLabel() {
        return label;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
