package com.lms.repository;
import com.lms.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.lms.enums.NotificationEventType;
import com.lms.enums.NotificationType;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    Optional<Notification> findByRequestKey(String requestKey);
    @EntityGraph(attributePaths = {"staff", "staff.user"})
    Page<Notification> findByEventTypeOrderByCreatedDateDesc(
            NotificationEventType eventType, Pageable pageable);

    @EntityGraph(attributePaths = {"staff", "staff.user"})
    Page<Notification> findByEventTypeAndNotificationTypeOrderByCreatedDateDesc(
            NotificationEventType eventType, NotificationType notificationType, Pageable pageable);
}
