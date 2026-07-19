package com.lms.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Transactions")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer transactionId;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(name = "borrow_id")
    private Borrow borrow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrow_detail_id")
    private BorrowDetail borrowDetail;

    @Column(name = "renewal_days")
    private Integer renewalDays;

    @Column(nullable = false, length = 50)
    private String transactionType;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    private LocalDateTime transactionDate;

    @Column(length = 50)
    private String status = "Completed";

    public Transaction() {
    }

    public Transaction(Integer transactionId, Wallet wallet, Borrow borrow, String transactionType, BigDecimal amount, LocalDateTime transactionDate, String status) {
        this.transactionId = transactionId;
        this.wallet = wallet;
        this.borrow = borrow;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.status = status;
    }

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public Borrow getBorrow() { return borrow; }
    public void setBorrow(Borrow borrow) { this.borrow = borrow; }
    public BorrowDetail getBorrowDetail() { return borrowDetail; }
    public void setBorrowDetail(BorrowDetail borrowDetail) { this.borrowDetail = borrowDetail; }
    public Integer getRenewalDays() { return renewalDays; }
    public void setRenewalDays(Integer renewalDays) { this.renewalDays = renewalDays; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

