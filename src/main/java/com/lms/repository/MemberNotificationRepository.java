package com.lms.repository;

import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberNotificationRepository extends JpaRepository<MemberNotification, MemberNotificationId> {

    List<MemberNotification> findByMember_MemberIdOrderByNotification_CreatedDateDesc(Integer memberId);

    // Lấy 5 thông báo mới nhất cho chuông thông báo.
    List<MemberNotification> findTop5ByMember_MemberIdOrderByNotification_CreatedDateDesc(Integer memberId);

    long countByMember_MemberIdAndIsReadFalse(Integer memberId);

    List<MemberNotification> findByMemberMemberIdAndNotificationTitleContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
            Integer memberId,
            String title);

    List<MemberNotification> findByMemberMemberIdAndNotificationContentContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
            Integer memberId,
            String content);

    // Lấy 1 bản ghi theo cặp (memberId, notificationId)
    java.util.Optional<MemberNotification> findByMemberMemberIdAndNotificationNotificationId(
            Integer memberId,
            Integer notificationId);

    // Helper: cho service gọi nhanh hơn theo tên method.
    default java.util.Optional<MemberNotification> findByMemberMemberIdAndNotificationId(
            Integer memberId,
            Integer notificationId) {
        return findByMemberMemberIdAndNotificationNotificationId(memberId, notificationId);
    }
}

