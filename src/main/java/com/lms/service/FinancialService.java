package com.lms.service;

import java.math.BigDecimal;

public interface FinancialService {

    void payOverdueFine(Integer memberId, Integer fineId);

    void payBorrowingFee(Integer memberId, Integer borrowId);

    BigDecimal calculateBorrowingFeeAmount(Integer borrowId);

    boolean hasPaidBorrowingFee(Integer memberId, Integer borrowId);

    void payReservationDeposit(Integer memberId, Integer reservationId);

    void getTransactionHistory(Integer memberId, int page);

    void createFine(Integer memberId, Double amount, String reason);

    void getAllTransactions(int page, String type);

    void topUpMemberAccount(String memberPhone, Double amount);
}
