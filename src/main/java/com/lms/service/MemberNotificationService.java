package com.lms.service;

import com.lms.dto.response.MemberNotificationResponse;
import com.lms.enums.NotificationSource;
import com.lms.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberNotificationService {

    Page<MemberNotificationResponse> getMyNotifications(
            String username, NotificationSource source, NotificationType type, Pageable pageable);

    long countMyNotifications(String username);

    long countMyNotificationsBySource(String username, NotificationSource source);

    List<MemberNotificationResponse> getLatestNotifications(String username);

    long countUnreadNotifications(String username);

    void markAllNotificationsAsRead(String username);

    long markNotificationAsRead(String username, Integer notificationId);

    // ======= BỔ SUNG PHƯƠNG THỨC GỬI THÔNG BÁO =======
    void sendNotificationToUser(String username, String title, String content);
}
