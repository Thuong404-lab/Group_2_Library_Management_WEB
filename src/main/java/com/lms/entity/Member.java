package com.lms.entity;
import jakarta.persistence.*;
@Entity
@Table(name = "Members")
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer memberId;
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne
    @JoinColumn(name = "tier_id")
    private MembershipTier tier;
    
    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public MembershipTier getTier() { return tier; }
    public void setTier(MembershipTier tier) { this.tier = tier; }
}
