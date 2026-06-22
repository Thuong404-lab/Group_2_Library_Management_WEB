package com.lms.repository;
import com.lms.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Integer> {
}
