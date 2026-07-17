package com.lms.service;

import com.lms.entity.Transaction;
import com.lms.entity.Wallet;
import com.lms.exception.ConflictException;
import com.lms.exception.ForbiddenException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.TransactionRepository;
import com.lms.repository.payos.PayOsTransactionRepository;
import com.lms.repository.payos.PayOsWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Batch fine payment owned by the aggregate payment flow. */
@Service
public class FineBatchPaymentService {
    private final TransactionRepository transactionRepository;
    private final PayOsTransactionRepository lockedTransactionRepository;
    private final PayOsWalletRepository walletRepository;

    public FineBatchPaymentService(TransactionRepository transactionRepository,
                                   PayOsTransactionRepository lockedTransactionRepository,
                                   PayOsWalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.lockedTransactionRepository = lockedTransactionRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void payAllFromWallet(Integer memberId) {
        List<Transaction> pending = transactionRepository.findUnpaidFineTransactions(
                memberId, List.of("FINE", "DAMAGE_FEE"));
        if (pending.isEmpty()) {
            throw new ConflictException("Không có khoản phạt nào cần thanh toán.");
        }
        pending.sort(Comparator.comparing(Transaction::getTransactionId));

        BigDecimal total = BigDecimal.ZERO;
        List<Transaction> lockedFines = new java.util.ArrayList<>();
        for (Transaction candidate : pending) {
            Transaction fine = lockedTransactionRepository.findByIdForUpdate(candidate.getTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khoản phạt."));
            validateFine(fine, memberId);
            total = total.add(fine.getAmount().abs());
            lockedFines.add(fine);
        }

        Wallet wallet = walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví thành viên."));
        BigDecimal balance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        if (balance.compareTo(total) < 0) {
            throw new ConflictException("Số dư ví không đủ để thanh toán tổng phí phạt.");
        }
        wallet.setBalance(balance.subtract(total));
        walletRepository.save(wallet);

        LocalDateTime paidAt = LocalDateTime.now();
        for (Transaction fine : lockedFines) {
            fine.setAmount(fine.getAmount().abs().negate());
            fine.setStatus("Completed");
            fine.setTransactionDate(paidAt);
            lockedTransactionRepository.save(fine);
        }
    }

    private void validateFine(Transaction fine, Integer memberId) {
        if (fine.getWallet() == null || fine.getWallet().getMember() == null
                || !memberId.equals(fine.getWallet().getMember().getMemberId())) {
            throw new ForbiddenException("Khoản phạt không thuộc về thành viên.");
        }
        String type = normalize(fine.getTransactionType());
        String status = normalize(fine.getStatus());
        if ((!"FINE".equals(type) && !"DAMAGE_FEE".equals(type))
                || "COMPLETED".equals(status) || "PAID".equals(status)
                || fine.getAmount() == null || fine.getAmount().signum() == 0) {
            throw new ConflictException("Danh sách phí phạt đã thay đổi. Vui lòng tải lại trang.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
