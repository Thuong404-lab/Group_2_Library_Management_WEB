package com.lms.service;

import java.math.BigDecimal;

import com.lms.entity.Transaction;
import org.springframework.data.domain.Page;

public interface FinancialService {

    void payOverdueFine(Integer memberId, Integer fineId);

    void payBorrowingFee(Integer memberId, Integer borrowId);

    BigDecimal calculateBorrowingFeeAmount(Integer borrowId);

    boolean hasPaidBorrowingFee(Integer memberId, Integer borrowId);

    void payReservationDeposit(Integer memberId, Integer reservationId);

    Page<Transaction> getTransactionHistory(Integer memberId, int page, String type);

    void createFine(Integer memberId, Double amount, String reason);

    Page<Transaction> getAllTransactions(int page, String type);

    void topUpMemberAccount(String memberPhone, Double amount);
}
