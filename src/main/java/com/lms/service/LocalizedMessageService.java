package com.lms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.entity.Notification;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import com.lms.entity.Member;
import com.lms.repository.MemberAccountRepository;

import java.util.Locale;

@Service
public class LocalizedMessageService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final MessageSource messageSource;
    private final MemberAccountRepository memberAccountRepository;

    public LocalizedMessageService(MessageSource messageSource,
                                   MemberAccountRepository memberAccountRepository) {
        this.messageSource = messageSource;
        this.memberAccountRepository = memberAccountRepository;
    }

    /** Provides the same bundle lookup for service instances created directly in unit tests. */
    public static LocalizedMessageService fallback() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return new LocalizedMessageService(source, null);
    }

    public String get(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }

    public String getForMember(Member member, String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, resolveMemberLocale(member));
    }

    public String getEnglish(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, Locale.ENGLISH);
    }

    /**
     * Stores an English fallback plus the i18n template metadata used to render
     * the same notification in the locale of the current request.
     */
    public void prepareNotification(Notification notification,
                                    String titleKey,
                                    String contentKey,
                                    Object... arguments) {
        Object[] safeArguments = arguments == null ? new Object[0] : arguments;
        notification.setTitleKey(titleKey);
        notification.setContentKey(contentKey);
        notification.setTitle(getEnglish(titleKey, safeArguments));
        notification.setContent(getEnglish(contentKey, safeArguments));
        try {
            notification.setMessageArguments(JSON.writeValueAsString(safeArguments));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize notification arguments", exception);
        }
    }

    public String renderNotificationTitle(Notification notification) {
        return renderNotificationMessage(
                notification.getTitleKey(), notification.getTitle(), notification.getMessageArguments());
    }

    public String renderNotificationContent(Notification notification) {
        return renderNotificationMessage(
                notification.getContentKey(), notification.getContent(), notification.getMessageArguments());
    }

    private String renderNotificationMessage(String key, String fallback, String serializedArguments) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        try {
            Object[] arguments = serializedArguments == null || serializedArguments.isBlank()
                    ? new Object[0]
                    : JSON.readValue(serializedArguments, Object[].class);
            return messageSource.getMessage(key, arguments, fallback, LocaleContextHolder.getLocale());
        } catch (JsonProcessingException exception) {
            return fallback;
        }
    }

    private Locale resolveMemberLocale(Member member) {
        if (memberAccountRepository == null || member == null || member.getMemberId() == null) {
            return LocaleContextHolder.getLocale();
        }
        String language = memberAccountRepository.findByMemberMemberId(member.getMemberId())
                .map(account -> account.getPreferredLanguage())
                .filter(value -> value != null && !value.isBlank())
                .orElse("en");
        return Locale.forLanguageTag(language);
    }
}
