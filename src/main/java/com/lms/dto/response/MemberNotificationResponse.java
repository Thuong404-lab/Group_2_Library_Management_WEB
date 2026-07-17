package com.lms.dto.response;

import com.lms.enums.NotificationType;
import java.time.LocalDateTime;

public class MemberNotificationResponse {

    private Integer notificationId;
    private String title;
    private String content;
    private NotificationType notificationType;
    private LocalDateTime sentDate;
    private Boolean read;
    private Boolean fromLibrarian;

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
}
