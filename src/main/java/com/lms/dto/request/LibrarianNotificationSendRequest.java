package com.lms.dto.request;

import com.lms.enums.NotificationRecipientType;
import com.lms.enums.NotificationType;
import java.util.List;

public class LibrarianNotificationSendRequest {

    private NotificationRecipientType recipientType;
    private NotificationType notificationType;
    private List<Integer> memberIds;
    private String title;
    private String content;
    private String requestToken;

    public LibrarianNotificationSendRequest() {
    }

    public NotificationRecipientType getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(NotificationRecipientType recipientType) {
        this.recipientType = recipientType;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
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

    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String requestToken) { this.requestToken = requestToken; }
}
