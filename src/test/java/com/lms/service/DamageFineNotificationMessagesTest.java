package com.lms.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DamageFineNotificationMessagesTest {

    private static final List<String> KEYS = List.of(
            "systemNotification.damageFine.paidWallet.title",
            "systemNotification.damageFine.paidWallet.content",
            "systemNotification.damageFine.paidCash.title",
            "systemNotification.damageFine.paidCash.content",
            "systemNotification.damageFine.created.title",
            "systemNotification.damageFine.created.content");

    @Test
    void damageFineNotificationMessagesExistInEnglishAndVietnamese() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");

        for (Locale locale : List.of(Locale.ENGLISH, Locale.forLanguageTag("vi"))) {
            for (String key : KEYS) {
                assertDoesNotThrow(() -> messages.getMessage(
                        key, new Object[]{"10 VND", "Test Book"}, locale), key + " / " + locale);
            }
        }
    }
}
