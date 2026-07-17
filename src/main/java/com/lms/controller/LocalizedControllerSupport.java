package com.lms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/** Shared request-locale message lookup for MVC controllers. */
public abstract class LocalizedControllerSupport {

    @Autowired
    private MessageSource messageSource;

    protected String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }

    protected String messageWithDetail(String key, Exception exception) {
        String detail = exception == null ? "" : exception.getMessage();
        return message(key, detail == null ? "" : detail);
    }
}
