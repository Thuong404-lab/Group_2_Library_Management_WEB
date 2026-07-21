package com.lms.dto.response;

public record NotificationRecipientSearchResponse(
        Integer memberId, String memberCode, String fullName, String email, String phone) { }
