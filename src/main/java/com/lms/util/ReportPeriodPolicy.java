package com.lms.util;

import java.time.ZoneId;

public final class ReportPeriodPolicy {

    public static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final long DEFAULT_LOOKBACK_DAYS = 29;
    public static final long MAX_RANGE_DAYS = 1_826;

    private ReportPeriodPolicy() {
    }
}
