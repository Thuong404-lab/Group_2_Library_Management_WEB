package com.lms.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
@Table(name = "Wallets")
public class Wallet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer walletId;
    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    @Column(precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    public Integer getWalletId() { return walletId; }
    public void setWalletId(Integer walletId) { this.walletId = walletId; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
