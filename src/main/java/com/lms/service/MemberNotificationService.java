package com.lms.service;

import com.lms.dto.response.MemberNotificationResponse;
import java.util.List;

public interface MemberNotificationService {

    List<MemberNotificationResponse> getMyNotifications(String username);

    List<MemberNotificationResponse> getLatestNotifications(String username);

    long countUnreadNotifications(String username);

    void markAllNotificationsAsRead(String username);

    // ======= BỔ SUNG PHƯƠNG THỨC GỬI THÔNG BÁO =======
    void sendNotificationToUser(String username, String title, String content);

    void sendNotificationToAllLibrarians(String title, String content);
}