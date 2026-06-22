package com.lms.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "MemberNotifications")
public class MemberNotification {
    @EmbeddedId
    private MemberNotificationId id;

    @ManyToOne
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne
    @MapsId("notificationId")
    @JoinColumn(name = "notification_id")
    private Notification notification;

    private Boolean isRead = false;
    private LocalDateTime readDate;

    public MemberNotification() {
    }

    public MemberNotification(MemberNotificationId id, Member member, Notification notification, Boolean isRead, LocalDateTime readDate) {
        this.id = id;
        this.member = member;
        this.notification = notification;
        this.isRead = isRead;
        this.readDate = readDate;
    }

    public MemberNotificationId getId() { return id; }
    public void setId(MemberNotificationId id) { this.id = id; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Notification getNotification() { return notification; }
    public void setNotification(Notification notification) { this.notification = notification; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getReadDate() { return readDate; }
    public void setReadDate(LocalDateTime readDate) { this.readDate = readDate; }
}
