package com.lms.dto.request;

import com.lms.enums.NotificationRecipientType;
import java.util.List;

public class LibrarianNotificationSendRequest {

    private NotificationRecipientType recipientType;
    private List<Integer> memberIds;
    private String title;
    private String content;

    public LibrarianNotificationSendRequest() {
    }

    public NotificationRecipientType getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(NotificationRecipientType recipientType) {
        this.recipientType = recipientType;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Integer> memberIds) {
        this.memberIds = memberIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}