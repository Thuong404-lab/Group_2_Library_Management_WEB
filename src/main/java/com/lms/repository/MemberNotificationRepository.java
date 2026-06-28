package com.lms.repository;

import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberNotificationRepository extends JpaRepository<MemberNotification, MemberNotificationId> {

    List<MemberNotification> findByMember_MemberIdOrderByNotification_CreatedDateDesc(Integer memberId);

    // ======= THÊM MỚI: lấy 5 thông báo mới nhất cho chuông =======
    List<MemberNotification> findTop5ByMember_MemberIdOrderByNotification_CreatedDateDesc(Integer memberId);
}
    List<MemberNotification> findByMemberMemberIdAndNotificationTitleContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
            Integer memberId,
            String title
    );

    List<MemberNotification> findByMemberMemberIdAndNotificationContentContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
            Integer memberId,
            String content
    );
}
