package com.lms.repository;

import com.lms.entity.MembershipTierTranslation;
import com.lms.entity.MembershipTierTranslationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipTierTranslationRepository
        extends JpaRepository<MembershipTierTranslation, MembershipTierTranslationId> {
    List<MembershipTierTranslation> findByLanguageCode(String languageCode);
    Optional<MembershipTierTranslation> findByTierTierIdAndLanguageCode(Integer tierId, String languageCode);
}
