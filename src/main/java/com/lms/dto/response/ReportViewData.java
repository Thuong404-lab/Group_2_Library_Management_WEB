package com.lms.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportViewData {
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final LocalDateTime generatedAt;
    private final long totalBorrows;
    private final long totalBorrowedItems;
    private final long onTimeReturns;
    private final long lateReturns;
    private final long overdueItems;
    private final long totalMembers;
    private final long activeBooks;
    private final long availableItems;
    private final BigDecimal totalRevenue;
    private final BigDecimal totalRefunds;
    private final long totalTransactions;
    private final BigDecimal averageTransaction;
    private final List<ReportMetric> transactionBreakdown;
    private final List<ReportMetric> monthlyRevenueStats;
    private final List<ReportMetric> topBooks;
    private final List<ReportMetric> topMembers;
    private final List<ReportMetric> monthlyBorrowStats;
    private final long activeBorrows;
    private final long pendingReservationRequests;
    private final long depositPaidReservations;
    private final long readyReservations;

    public ReportViewData(LocalDate fromDate,
            LocalDate toDate,
            LocalDateTime generatedAt,
            long totalBorrows,
            long totalBorrowedItems,
            long onTimeReturns,
            long lateReturns,
            long overdueItems,
            long totalMembers,
            long activeBooks,
            long availableItems,
            BigDecimal totalRevenue,
            BigDecimal totalRefunds,
            long totalTransactions,
            BigDecimal averageTransaction,
            List<ReportMetric> transactionBreakdown,
            List<ReportMetric> monthlyRevenueStats,
            List<ReportMetric> topBooks,
            List<ReportMetric> topMembers,
            List<ReportMetric> monthlyBorrowStats,
            long activeBorrows,
            long pendingReservationRequests,
            long depositPaidReservations,
            long readyReservations) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.generatedAt = generatedAt;
        this.totalBorrows = totalBorrows;
        this.totalBorrowedItems = totalBorrowedItems;
        this.onTimeReturns = onTimeReturns;
        this.lateReturns = lateReturns;
        this.overdueItems = overdueItems;
        this.totalMembers = totalMembers;
        this.activeBooks = activeBooks;
        this.availableItems = availableItems;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        this.totalRefunds = totalRefunds == null ? BigDecimal.ZERO : totalRefunds;
        this.totalTransactions = totalTransactions;
        this.averageTransaction = averageTransaction == null ? BigDecimal.ZERO : averageTransaction;
        this.transactionBreakdown = transactionBreakdown;
        this.monthlyRevenueStats = monthlyRevenueStats;
        this.topBooks = topBooks;
        this.topMembers = topMembers;
        this.monthlyBorrowStats = monthlyBorrowStats;
        this.activeBorrows = activeBorrows;
        this.pendingReservationRequests = pendingReservationRequests;
        this.depositPaidReservations = depositPaidReservations;
        this.readyReservations = readyReservations;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public long getTotalBorrows() {
        return totalBorrows;
    }

    public long getTotalBorrowedItems() {
        return totalBorrowedItems;
    }

    public long getOnTimeReturns() {
        return onTimeReturns;
    }

    public long getLateReturns() {
        return lateReturns;
    }

    public long getOverdueItems() {
        return overdueItems;
    }

    public long getTotalMembers() {
        return totalMembers;
    }

    public long getActiveBooks() {
        return activeBooks;
    }

    public long getAvailableItems() {
        return availableItems;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getTotalRefunds() {
        return totalRefunds;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getAverageTransaction() {
        return averageTransaction;
    }

    public List<ReportMetric> getTransactionBreakdown() {
        return transactionBreakdown;
    }

    public List<ReportMetric> getMonthlyRevenueStats() {
        return monthlyRevenueStats;
    }

    public List<ReportMetric> getTopBooks() {
        return topBooks;
    }

    public List<ReportMetric> getTopMembers() {
        return topMembers;
    }

    public List<ReportMetric> getMonthlyBorrowStats() {
        return monthlyBorrowStats;
    }

    public long getActiveBorrows() {
        return activeBorrows;
    }

    public long getPendingReservationRequests() {
        return pendingReservationRequests;
    }

    public long getDepositPaidReservations() {
        return depositPaidReservations;
    }

    public long getReadyReservations() {
        return readyReservations;
    }
}
