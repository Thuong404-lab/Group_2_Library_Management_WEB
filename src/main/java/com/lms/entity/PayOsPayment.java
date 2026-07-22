package com.lms.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PayOSPayments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payos_payment_order_code", columnNames = "order_code")
})
public class PayOsPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false, length = 30)
    private String purpose;

    private Integer referenceId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Long orderCode;

    @Column(length = 48, unique = true)
    private String requestKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_staff_id")
    private Staff initiatedByStaff;

    @Column(length = 100)
    private String paymentLinkId;

    @Column(length = 1000)
    private String checkoutUrl;

    @Lob
    @Column(columnDefinition = "nvarchar(max)")
    private String qrCode;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 100)
    private String bankReference;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Long getOrderCode() { return orderCode; }
    public void setOrderCode(Long orderCode) { this.orderCode = orderCode; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }
    public Staff getInitiatedByStaff() { return initiatedByStaff; }
    public void setInitiatedByStaff(Staff initiatedByStaff) { this.initiatedByStaff = initiatedByStaff; }
    public String getPaymentLinkId() { return paymentLinkId; }
    public void setPaymentLinkId(String paymentLinkId) { this.paymentLinkId = paymentLinkId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
