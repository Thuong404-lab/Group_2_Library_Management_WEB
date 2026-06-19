package com.lms.repository;
import com.lms.entity.MemberNotification;
import com.lms.entity.MemberNotificationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberNotificationRepository extends JpaRepository<MemberNotification, MemberNotificationId> {
}
