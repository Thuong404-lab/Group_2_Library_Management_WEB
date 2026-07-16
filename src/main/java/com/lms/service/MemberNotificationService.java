package com.lms.service;

import com.lms.dto.response.MemberNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberNotificationService {

    Page<MemberNotificationResponse> getMyNotifications(String username, Pageable pageable);

    List<MemberNotificationResponse> getLatestNotifications(String username);

    long countUnreadNotifications(String username);

    void markAllNotificationsAsRead(String username);

    // ======= BỔ SUNG PHƯƠNG THỨC GỬI THÔNG BÁO =======
    void sendNotificationToUser(String username, String title, String content);

    void sendNotificationToAllLibrarians(String title, String content);
}
