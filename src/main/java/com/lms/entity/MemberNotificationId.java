package com.lms.entity;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MemberNotificationId implements Serializable {
    private Integer memberId;
    private Integer notificationId;

    public MemberNotificationId() {}
    public MemberNotificationId(Integer memberId, Integer notificationId) {
        this.memberId = memberId;
        this.notificationId = notificationId;
    }

    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    public Integer getNotificationId() { return notificationId; }
    public void setNotificationId(Integer notificationId) { this.notificationId = notificationId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberNotificationId that = (MemberNotificationId) o;
        return Objects.equals(memberId, that.memberId) && Objects.equals(notificationId, that.notificationId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(memberId, notificationId);
    }
}
