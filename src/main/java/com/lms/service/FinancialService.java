package com.lms.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.lms.entity.Reservation;
import com.lms.entity.Staff;
import com.lms.entity.Transaction;
import org.springframework.data.domain.Page;

public interface FinancialService {

    void payOverdueFine(Integer memberId, Integer fineId);

    void payBorrowingFee(Integer memberId, Integer borrowId);

    BigDecimal calculateBorrowingFeeAmount(Integer borrowId);

    boolean hasPaidBorrowingFee(Integer memberId, Integer borrowId);

    void payReservationDeposit(Integer memberId, Integer reservationId);

    void requestReservationDepositRefund(Integer memberId, Integer reservationId);

    void refundReservationDeposit(Integer memberId, Integer reservationId);

    List<Reservation> getRefundableReservationDeposits(Integer memberId);

    List<Reservation> getPendingReservationDepositRefunds();

    BigDecimal getReservationDepositAmount();

    Page<Transaction> getTransactionHistory(Integer memberId, int page, String type);

    void issueOverdueFine(Integer borrowDetailId);

    void issueDamageCompensation(Integer borrowDetailId);

    BigDecimal getDamageCompensationAmount();

    List<Transaction> getPendingFines();

    void payFineByCash(Integer fineId);

    void payFineByWalletAtDesk(Integer fineId);

    Page<Transaction> getAllTransactions(int page, String query, String type,
            String status, String channel, LocalDate fromDate, LocalDate toDate);

    void topUpMemberAccount(String memberLookup, BigDecimal amount, String requestId, Staff performedBy);
}
