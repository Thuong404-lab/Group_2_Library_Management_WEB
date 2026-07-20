package com.lms.dto.response;

import com.lms.enums.NotificationType;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import java.time.LocalDateTime;

public class MemberNotificationResponse {

    private Integer notificationId;
    private String title;
    private String content;
    private NotificationType notificationType;
    private LocalDateTime sentDate;
    private Boolean read;
    private Boolean fromLibrarian;
    private NotificationSource notificationSource;
    private NotificationEventType eventType;

    public MemberNotificationResponse() {
    }

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
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

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Boolean getFromLibrarian() { return fromLibrarian; }
    public void setFromLibrarian(Boolean fromLibrarian) { this.fromLibrarian = fromLibrarian; }
    public NotificationSource getNotificationSource() { return notificationSource; }
    public void setNotificationSource(NotificationSource notificationSource) { this.notificationSource = notificationSource; }
    public NotificationEventType getEventType() { return eventType; }
    public void setEventType(NotificationEventType eventType) { this.eventType = eventType; }
}
