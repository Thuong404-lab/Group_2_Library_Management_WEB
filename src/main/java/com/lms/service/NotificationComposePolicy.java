package com.lms.service;

import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.enums.NotificationRecipientType;
import java.text.Normalizer;
import java.util.*;

public final class NotificationComposePolicy {
    public static final int TITLE_MIN_LENGTH = 5;
    public static final int TITLE_MAX_LENGTH = 150;
    public static final int CONTENT_MIN_LENGTH = 10;
    public static final int CONTENT_MAX_LENGTH = 2000;
    public static final int MAX_SELECTED_RECIPIENTS = 200;
    public static final int MEMBER_SEARCH_PAGE_SIZE = 20;
    public static final int MEMBER_SEARCH_MIN_LENGTH = 2;

    private NotificationComposePolicy() {
    }

    public static String normalizeTitle(String value) {
        return clean(value).strip().replaceAll("\\s+", " ");
    }

    public static String normalizeContent(String value) {
        return clean(value).replace("\r\n", "\n").replace('\r', '\n').strip()
                .replaceAll("(?:\\n[\\t ]*){3,}", "\n\n");
    }

    private static String clean(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        StringBuilder result = new StringBuilder(normalized.length());
        normalized.codePoints()
                .filter(c -> Character.getType(c) != Character.FORMAT)
                .filter(c -> !Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t')
                .forEach(result::appendCodePoint);
        return result.toString();
    }

    private static boolean containsLetter(String value) {
        return value.codePoints().anyMatch(Character::isLetter);
    }

    private static List<Integer> deduplicateIds(List<Integer> ids) {
        if (ids == null) return List.of();
        Set<Integer> uniqueIds = new LinkedHashSet<>();
        ids.stream().filter(id -> id != null && id > 0).forEach(uniqueIds::add);
        return List.copyOf(uniqueIds);
    }

    private static boolean isValidRequestToken(String token) {
        if (token == null || token.length() != 36) return false;
        try {
            return UUID.fromString(token).toString().equalsIgnoreCase(token);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static Map<String, String> normalizeAndValidate(LibrarianNotificationSendRequest request) {
        request.setTitle(normalizeTitle(request.getTitle()));
        request.setContent(normalizeContent(request.getContent()));
        request.setMemberIds(deduplicateIds(request.getMemberIds()));
        Map<String, String> errors = new LinkedHashMap<>();
        if (request.getRecipientType() == null)
            errors.put("recipientType", "backend.librarian.notification.recipientRequired");
        if (request.getNotificationType() == null)
            errors.put("notificationType", "backend.librarian.notification.typeRequired");
        else if (!request.getNotificationType().isManualSelectable())
            errors.put("notificationType", "backend.librarian.notification.typeInvalidForManual");
        validateTitle(request.getTitle(), errors);
        validateContent(request.getTitle(), request.getContent(), errors);
        if (request.getRecipientType() == NotificationRecipientType.SELECTED) {
            if (request.getMemberIds().isEmpty())
                errors.put("memberIds", "backend.librarian.notification.memberRequired");
            else if (request.getMemberIds().size() > MAX_SELECTED_RECIPIENTS)
                errors.put("memberIds", "backend.librarian.notification.memberMaximum");
        }
        if (!isValidRequestToken(request.getRequestToken()))
            errors.put("requestToken", "backend.librarian.notification.requestExpired");
        return errors;
    }

    private static void validateTitle(String value, Map<String, String> errors) {
        if (value.isEmpty()) errors.put("title", "backend.librarian.notification.titleRequired");
        else if (value.length() < TITLE_MIN_LENGTH) errors.put("title", "backend.librarian.notification.titleMinimum");
        else if (value.length() > TITLE_MAX_LENGTH) errors.put("title", "backend.librarian.notification.titleMaximum");
        else if (!containsLetter(value)) errors.put("title", "backend.librarian.notification.titleLetters");
    }

    private static void validateContent(String title, String value, Map<String, String> errors) {
        if (value.isEmpty()) errors.put("content", "backend.librarian.notification.contentRequired");
        else if (value.length() < CONTENT_MIN_LENGTH) errors.put("content", "backend.librarian.notification.contentMinimum");
        else if (value.length() > CONTENT_MAX_LENGTH) errors.put("content", "backend.librarian.notification.contentMaximum");
        else if (!containsLetter(value)) errors.put("content", "backend.librarian.notification.contentLetters");
        else if (!title.isEmpty() && normalizeTitle(value).equalsIgnoreCase(normalizeTitle(title)))
            errors.put("content", "backend.librarian.notification.contentDifferent");
    }
}
