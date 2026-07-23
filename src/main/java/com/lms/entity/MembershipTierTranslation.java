package com.lms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "MembershipTierTranslations")
@IdClass(MembershipTierTranslationId.class)
public class MembershipTierTranslation {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id")
    private MembershipTier tier;

    @Id
    @Column(name = "language_code", length = 5)
    private String languageCode;

    @Column(name = "tier_name", nullable = false, length = 100)
    private String tierName;

    @Column(name = "benefits", columnDefinition = "NVARCHAR(MAX)")
    private String benefits;

    public MembershipTier getTier() { return tier; }
    public void setTier(MembershipTier tier) { this.tier = tier; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getTierName() { return tierName; }
    public void setTierName(String tierName) { this.tierName = tierName; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }
}
