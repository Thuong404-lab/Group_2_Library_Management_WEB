package com.lms.entity;

import java.io.Serializable;
import java.util.Objects;

public class MembershipTierTranslationId implements Serializable {
    private Integer tier;
    private String languageCode;

    public MembershipTierTranslationId() {}
    public MembershipTierTranslationId(Integer tier, String languageCode) {
        this.tier = tier;
        this.languageCode = languageCode;
    }
    public Integer getTier() { return tier; }
    public void setTier(Integer tier) { this.tier = tier; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof MembershipTierTranslationId other)) return false;
        return Objects.equals(tier, other.tier) && Objects.equals(languageCode, other.languageCode);
    }
    @Override public int hashCode() { return Objects.hash(tier, languageCode); }
}
