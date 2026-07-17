package com.lms.dto.response;

import com.lms.entity.MemberAccount;
import com.lms.entity.Member;
import com.lms.entity.MembershipTier;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public record MemberListViewData(
        Page<MemberAccount> accounts,
        Map<Integer, Member> memberByUserId,
        List<MembershipTier> tiers,
        Map<String, Long> summaryCounts) {
}
