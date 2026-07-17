package com.lms.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class LocalizedMessageService {

    private final MessageSource messageSource;

    public LocalizedMessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }
}
