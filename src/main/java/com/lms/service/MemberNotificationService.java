package com.lms.service;

import com.lms.dto.response.MemberNotificationResponse;

import java.util.List;

public interface MemberNotificationService {

    List<MemberNotificationResponse> getMyNotifications(String username);

    // ======= THÊM MỚI =======
    List<MemberNotificationResponse> getLatestNotifications(String username);

    long countUnreadNotifications(String username);

    void markAllNotificationsAsRead(String username);

    void deleteNotificationById(Integer memberNotificationId, String username);

    void deleteAllNotifications(String username);
}

