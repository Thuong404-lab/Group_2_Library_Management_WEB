package com.lms.dto.response;

import com.lms.enums.NotificationType;
import java.time.LocalDateTime;

public record LibrarianNotificationHistoryResponse(
        Integer notificationId, String title, NotificationType notificationType,
        String senderName, LocalDateTime createdDate, long recipientCount, long readCount) { }
