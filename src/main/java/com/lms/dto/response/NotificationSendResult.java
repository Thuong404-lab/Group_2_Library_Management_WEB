package com.lms.dto.response;

import java.time.LocalDateTime;

public record NotificationSendResult(Integer notificationId, int recipientCount, LocalDateTime sentAt, boolean duplicateRequest) { }
