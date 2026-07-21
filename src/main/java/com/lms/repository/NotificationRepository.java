package com.lms.repository;
import com.lms.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.lms.enums.NotificationEventType;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    Optional<Notification> findByRequestKey(String requestKey);
    List<Notification> findTop10ByEventTypeOrderByCreatedDateDesc(NotificationEventType eventType);
}
