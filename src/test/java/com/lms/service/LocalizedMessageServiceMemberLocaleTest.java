package com.lms.service;

import com.lms.entity.Member;
import com.lms.entity.MemberAccount;
import com.lms.entity.Notification;
import com.lms.repository.MemberAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalizedMessageServiceMemberLocaleTest {

    @Test
    void resolvesI18nBundleFromMemberPreference() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("notification.sample", Locale.ENGLISH, "English notification");
        messageSource.addMessage("notification.sample", Locale.forLanguageTag("vi"), "Thông báo tiếng Việt");

        MemberAccountRepository accountRepository = mock(MemberAccountRepository.class);
        Member member = new Member();
        member.setMemberId(7);
        MemberAccount account = new MemberAccount();
        account.setPreferredLanguage("vi");
        when(accountRepository.findByMemberMemberId(7)).thenReturn(Optional.of(account));

        LocalizedMessageService service = new LocalizedMessageService(messageSource, accountRepository);

        assertThat(service.getForMember(member, "notification.sample"))
                .isEqualTo("Thông báo tiếng Việt");
    }
    @Test
    void rendersTheSameStoredNotificationInTheCurrentRequestLocale() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("loan.title", Locale.ENGLISH, "Loan approved");
        messageSource.addMessage("loan.content", Locale.ENGLISH, "Book {0} is ready");
        messageSource.addMessage("loan.title", Locale.forLanguageTag("vi"), "Đã duyệt mượn sách");
        messageSource.addMessage("loan.content", Locale.forLanguageTag("vi"), "Sách {0} đã sẵn sàng");
        LocalizedMessageService service = new LocalizedMessageService(messageSource, null);
        Notification notification = new Notification();

        service.prepareNotification(notification, "loan.title", "loan.content", "Clean Code");

        assertThat(notification.getTitle()).isEqualTo("Loan approved");
        assertThat(notification.getContent()).isEqualTo("Book Clean Code is ready");

        try {
            LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
            assertThat(service.renderNotificationTitle(notification)).isEqualTo("Đã duyệt mượn sách");
            assertThat(service.renderNotificationContent(notification)).isEqualTo("Sách Clean Code đã sẵn sàng");

            LocaleContextHolder.setLocale(Locale.ENGLISH);
            assertThat(service.renderNotificationTitle(notification)).isEqualTo("Loan approved");
            assertThat(service.renderNotificationContent(notification)).isEqualTo("Book Clean Code is ready");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void keepsManualNotificationTextWhenNoTemplateKeysExist() {
        LocalizedMessageService service = new LocalizedMessageService(new StaticMessageSource(), null);
        Notification notification = new Notification();
        notification.setTitle("Library closes early today");
        notification.setContent("Please return books before 4 PM.");

        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        try {
            assertThat(service.renderNotificationTitle(notification)).isEqualTo("Library closes early today");
            assertThat(service.renderNotificationContent(notification)).isEqualTo("Please return books before 4 PM.");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
