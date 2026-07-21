package com.lms.service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

public final class TopUpPolicy {
    public static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(10_000);
    public static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(500_000_000);
    public static final BigDecimal MAX_WALLET_BALANCE = BigDecimal.valueOf(500_000_000);
    public static final List<BigDecimal> QUICK_AMOUNTS = List.of(
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(100_000),
            BigDecimal.valueOf(200_000),
            BigDecimal.valueOf(500_000));
    public static final List<String> COMPLETED_STATUSES = List.of("completed", "paid");
    public static final int MEMBER_SEARCH_LIMIT = 20;
    public static final int RECENT_TRANSACTION_LIMIT = 10;
    public static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private TopUpPolicy() {
    }
}
