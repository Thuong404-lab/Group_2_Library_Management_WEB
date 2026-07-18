package com.lms.entity;
import com.lms.enums.NotificationType;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationSource;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDateTime;
@Entity
@Table(name = "Notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;
    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String content;
    @Column(name = "title_key", length = 255)
    private String titleKey;
    @Column(name = "content_key", length = 255)
    private String contentKey;
    @Column(name = "message_arguments", columnDefinition = "NVARCHAR(MAX)")
    private String messageArguments;
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'GENERAL'")
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType = NotificationType.GENERAL;
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_source", nullable = false, length = 20)
    private NotificationSource notificationSource = NotificationSource.SYSTEM;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private NotificationEventType eventType = NotificationEventType.GENERAL;
    private LocalDateTime createdDate;
    @Column(length = 50)
    private String status = "Active";

    public Notification() {
    }

    public Notification(Integer notificationId, Staff staff, String title, String content, LocalDateTime createdDate, String status) {
        this.notificationId = notificationId;
        this.staff = staff;
        this.title = title;
        this.content = content;
        this.createdDate = createdDate;
        this.status = status;
    }

    public Integer getNotificationId() { return notificationId; }
    public void setNotificationId(Integer notificationId) { this.notificationId = notificationId; }
    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTitleKey() { return titleKey; }
    public void setTitleKey(String titleKey) { this.titleKey = titleKey; }
    public String getContentKey() { return contentKey; }
    public void setContentKey(String contentKey) { this.contentKey = contentKey; }
    public String getMessageArguments() { return messageArguments; }
    public void setMessageArguments(String messageArguments) { this.messageArguments = messageArguments; }
    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public NotificationSource getNotificationSource() { return notificationSource; }
    public void setNotificationSource(NotificationSource notificationSource) { this.notificationSource = notificationSource; }
    public NotificationEventType getEventType() { return eventType; }
    public void setEventType(NotificationEventType eventType) { this.eventType = eventType; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
