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
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberNotificationRepository extends JpaRepository<MemberNotification, MemberNotificationId> {

    @EntityGraph(attributePaths = {"notification", "notification.staff"})
    Page<MemberNotification> findByMember_MemberIdOrderByNotification_CreatedDateDesc(Integer memberId,
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

    @EntityGraph(attributePaths = {"notification", "notification.staff"})
    List<MemberNotification> findByMember_MemberIdOrderByNotification_CreatedDateDesc(Integer memberId);

    long countByMember_MemberIdAndIsReadFalse(Integer memberId);

    List<MemberNotification> findByMemberMemberIdAndNotificationTitleContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
            Integer memberId,
            String title);

    List<MemberNotification> findByMemberMemberIdAndNotificationContentContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
            Integer memberId,
            String content);

    @Query("""
            select mn
            from MemberNotification mn
            where mn.member.memberId = :memberId
              and (
                    lower(mn.notification.title) like lower(concat('%', :keyword, '%'))
                 or lower(mn.notification.content) like lower(concat('%', :keyword, '%'))
              )
            order by mn.notification.createdDate desc
            """)
    Page<MemberNotification> findTopupNotifications(@Param("memberId") Integer memberId,
                                                    @Param("keyword") String keyword,
                                                    Pageable pageable);
}
