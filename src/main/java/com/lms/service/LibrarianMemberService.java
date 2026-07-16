package com.lms.service;

import com.lms.dto.request.CreateMemberAccountRequest;
import com.lms.dto.request.UpdateMemberAccountRequest;
import com.lms.dto.response.MemberListViewData;
import com.lms.entity.MembershipTier;

import java.util.List;
import java.util.Map;

public interface LibrarianMemberService {

    MemberListViewData getMemberList(int page, String keyword, String status, String tier);

    List<MembershipTier> getMembershipTiers();

    Map<String, String> validateCreate(CreateMemberAccountRequest request);

    void createMember(CreateMemberAccountRequest request);

    Map<String, String> validateUpdate(Integer accountId, UpdateMemberAccountRequest request);

    void updateMember(Integer accountId, UpdateMemberAccountRequest request);

    boolean deactivateMember(Integer accountId);

    void changeMemberStatus(Integer accountId, String status);
}
