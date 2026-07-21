package com.lms.service;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.enums.NotificationRecipientType;
import com.lms.enums.NotificationType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NotificationComposePolicyTest {

    @Test
    void normalizesInvisibleCharactersAndDuplicateRecipients() {
        LibrarianNotificationSendRequest request = validRequest();
        request.setTitle("  Thông\u200B báo   mới  ");
        request.setMemberIds(List.of(3, 3, 2));

        Map<String, String> errors = NotificationComposePolicy.normalizeAndValidate(request);

        assertTrue(errors.isEmpty());
        assertEquals("Thông báo mới", request.getTitle());
        assertEquals(List.of(3, 2), request.getMemberIds());
    }

    @Test
    void rejectsTextWithoutLettersAndEquivalentContent() {
        LibrarianNotificationSendRequest request = validRequest();
        request.setTitle("12345 !!!");
        request.setContent("1234567890");
        Map<String, String> errors = NotificationComposePolicy.normalizeAndValidate(request);
        assertEquals("backend.librarian.notification.titleLetters", errors.get("title"));
        assertEquals("backend.librarian.notification.contentLetters", errors.get("content"));

        request = validRequest();
        request.setTitle("Thông báo bảo trì");
        request.setContent("Thông   báo\n bảo trì");
        errors = NotificationComposePolicy.normalizeAndValidate(request);
        assertEquals("backend.librarian.notification.contentDifferent", errors.get("content"));
    }

    @Test
    void rejectsTooManySelectedRecipientsAndInvalidToken() {
        LibrarianNotificationSendRequest request = validRequest();
        request.setMemberIds(java.util.stream.IntStream.rangeClosed(1, 201).boxed().toList());
        request.setRequestToken("invalid");
        Map<String, String> errors = NotificationComposePolicy.normalizeAndValidate(request);
        assertTrue(errors.containsKey("memberIds"));
        assertTrue(errors.containsKey("requestToken"));
    }

    private LibrarianNotificationSendRequest validRequest() {
        LibrarianNotificationSendRequest request = new LibrarianNotificationSendRequest();
        request.setRecipientType(NotificationRecipientType.SELECTED);
        request.setNotificationType(NotificationType.GENERAL);
        request.setMemberIds(List.of(1));
        request.setTitle("Thông báo mới");
        request.setContent("Nội dung thông báo hợp lệ");
        request.setRequestToken("123e4567-e89b-12d3-a456-426614174000");
        return request;
    }
}
