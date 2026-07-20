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
    private static final String MESSAGE_ARGUMENT_PREFIX = "@@i18n:";
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
        return messageSource.getMessage(key, resolveMessageArguments(arguments, Locale.ENGLISH), Locale.ENGLISH);
    }

    /** Stores a message-bundle key as a locale-aware notification argument. */
    public String messageArgument(String key) {
        return MESSAGE_ARGUMENT_PREFIX + key;
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
            Locale locale = LocaleContextHolder.getLocale();
            String effectiveKey = key;
            if (key.endsWith(".contentWithReason") && arguments.length > 0) {
                Object detail = arguments[arguments.length - 1];
                if (detail == null || "null".equalsIgnoreCase(String.valueOf(detail).trim())) {
                    effectiveKey = key.substring(0, key.length() - ".contentWithReason".length())
                            + ".contentWithoutDetail";
                    arguments = java.util.Arrays.copyOf(arguments, arguments.length - 1);
                }
            }
            return messageSource.getMessage(effectiveKey, resolveMessageArguments(arguments, locale), fallback, locale);
        } catch (JsonProcessingException exception) {
            return fallback;
        }
    }

    private Object[] resolveMessageArguments(Object[] arguments, Locale locale) {
        if (arguments == null || arguments.length == 0) {
            return new Object[0];
        }
        Object[] resolved = arguments.clone();
        for (int index = 0; index < resolved.length; index++) {
            Object argument = resolved[index];
            if (argument instanceof String value) {
                if (value.startsWith(MESSAGE_ARGUMENT_PREFIX)) {
                    String messageKey = value.substring(MESSAGE_ARGUMENT_PREFIX.length());
                    resolved[index] = messageSource.getMessage(messageKey, null, messageKey, locale);
                } else {
                    String legacyReason = messageSource.getMessage("rejection.code." + value, null, null, locale);
                    if (legacyReason != null) {
                        resolved[index] = legacyReason;
                    }
                }
            }
        }
        return resolved;
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
