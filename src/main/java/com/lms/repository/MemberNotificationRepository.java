package com.lms.repository;

import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.lms.enums.NotificationEventType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MemberNotificationRepository extends JpaRepository<MemberNotification, MemberNotificationId> {

    @EntityGraph(attributePaths = {"notification", "notification.staff"})
    @Query("""
            select mn
            from MemberNotification mn
            where mn.member.memberId = :memberId
              and (:source is null or mn.notification.notificationSource = :source)
              and (:type is null or mn.notification.notificationType = :type)
            order by mn.notification.createdDate desc
            """)
    Page<MemberNotification> findNotificationPage(
            @Param("memberId") Integer memberId,
            @Param("source") com.lms.enums.NotificationSource source,
            @Param("type") com.lms.enums.NotificationType type,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MemberNotification mn
            set mn.isRead = true, mn.readDate = :readDate
            where mn.member.memberId = :memberId
              and mn.isRead = false
            """)
    int markUnreadNotificationsAsRead(@Param("memberId") Integer memberId,
                                      @Param("readDate") java.time.LocalDateTime readDate);

    // Lấy 5 thông báo mới nhất cho chuông thông báo.
    List<MemberNotification> findTop5ByMember_MemberIdOrderByNotification_CreatedDateDesc(Integer memberId);

    @EntityGraph(attributePaths = {"notification", "notification.staff"})
    List<MemberNotification> findTop20ByMember_MemberIdOrderByNotification_CreatedDateDesc(Integer memberId);

    long countByMember_MemberIdAndIsReadFalse(Integer memberId);

    long countByMember_MemberId(Integer memberId);

    long countByNotification_NotificationId(Integer notificationId);

    long countByNotification_NotificationIdAndIsReadTrue(Integer notificationId);

    long countByMember_MemberIdAndNotification_NotificationSource(
            Integer memberId, com.lms.enums.NotificationSource source);

    List<MemberNotification> findByMemberMemberIdAndNotificationTitleContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
            Integer memberId,
            String title);

    List<MemberNotification> findByMemberMemberIdAndNotificationContentContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
            Integer memberId,
            String content);

    Page<MemberNotification> findByMember_MemberIdAndNotification_EventTypeOrderByNotification_CreatedDateDesc(
            Integer memberId,
            NotificationEventType eventType,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update MemberNotification mn
            set mn.isRead = true, mn.readDate = :readDate
            where mn.member.memberId = :memberId
              and mn.notification.eventType = :eventType
              and mn.isRead = false
            """)
    int markUnreadNotificationsAsReadByEventType(@Param("memberId") Integer memberId,
                                                  @Param("eventType") NotificationEventType eventType,
                                                  @Param("readDate") java.time.LocalDateTime readDate);
}
