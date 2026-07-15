package com.lms.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "PayOSPaymentFineItems")
public class PayOsPaymentFineItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PayOsPayment payment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fine_transaction_id", nullable = false)
    private Transaction fineTransaction;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amountSnapshot;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public PayOsPayment getPayment() { return payment; }
    public void setPayment(PayOsPayment payment) { this.payment = payment; }
    public Transaction getFineTransaction() { return fineTransaction; }
    public void setFineTransaction(Transaction fineTransaction) { this.fineTransaction = fineTransaction; }
    public BigDecimal getAmountSnapshot() { return amountSnapshot; }
    public void setAmountSnapshot(BigDecimal amountSnapshot) { this.amountSnapshot = amountSnapshot; }
}
