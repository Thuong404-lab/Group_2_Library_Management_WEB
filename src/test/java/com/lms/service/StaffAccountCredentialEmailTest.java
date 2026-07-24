package com.lms.service;

import com.lms.event.StaffAccountCreatedEvent;
import com.lms.service.impl.EmailServiceImpl;
import com.lms.service.impl.StaffAccountCredentialEmailListener;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffAccountCredentialEmailTest {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void listenerForwardsInitialCredentialsToEmailService() {
        EmailService emailService = mock(EmailService.class);
        StaffAccountCredentialEmailListener listener =
                new StaffAccountCredentialEmailListener(emailService);

        listener.scheduleInitialCredentials(new StaffAccountCreatedEvent(
                "librarian@example.com",
                "Library Staff",
                "librarian01",
                "InitialPassword123"));

        verify(emailService).sendStaffAccountCredentials(
                "librarian@example.com",
                "Library Staff",
                "librarian01",
                "InitialPassword123");
    }

    @Test
    void listenerDefersCredentialsUntilTransactionCommit() {
        EmailService emailService = mock(EmailService.class);
        StaffAccountCredentialEmailListener listener =
                new StaffAccountCredentialEmailListener(emailService);
        StaffAccountCreatedEvent event = new StaffAccountCreatedEvent(
                "librarian@example.com",
                "Library Staff",
                "librarian01",
                "InitialPassword123");
        TransactionSynchronizationManager.initSynchronization();

        listener.scheduleInitialCredentials(event);

        verify(emailService, never()).sendStaffAccountCredentials(
                "librarian@example.com",
                "Library Staff",
                "librarian01",
                "InitialPassword123");

        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(emailService).sendStaffAccountCredentials(
                "librarian@example.com",
                "Library Staff",
                "librarian01",
                "InitialPassword123");
    }

    @Test
    void credentialsEmailUsesHtmlTemplateAndEscapesDynamicValues() throws Exception {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        EmailServiceImpl emailService = new EmailServiceImpl(mailSender);

        emailService.sendStaffAccountCredentials(
                "librarian@example.com",
                "<Library Staff>",
                "staff&01",
                "P&<123>");

        verify(mailSender).send(mimeMessage);
        assertEquals("Your library staff account", mimeMessage.getSubject());
        assertEquals("librarian@example.com", mimeMessage.getAllRecipients()[0].toString());

        String html = String.valueOf(mimeMessage.getContent());
        assertTrue(html.contains("&lt;Library Staff&gt;"));
        assertTrue(html.contains("staff&amp;01"));
        assertTrue(html.contains("P&amp;&lt;123&gt;"));
        assertTrue(html.contains("<meta charset=\"UTF-8\">"));
        assertTrue(html.contains("font-family:Arial,'Helvetica Neue',Helvetica,sans-serif"));
        assertFalse(html.contains("font-family:Georgia"));
        assertFalse(html.contains("<Library Staff>"));
    }

}
