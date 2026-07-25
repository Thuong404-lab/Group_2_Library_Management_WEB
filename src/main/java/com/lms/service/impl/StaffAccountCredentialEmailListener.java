package com.lms.service.impl;

import com.lms.event.StaffAccountCreatedEvent;
import com.lms.service.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Schedules initial librarian credentials for delivery only after account data
 * is committed. An email failure from the after-commit callback is propagated
 * to the MVC layer so the administrator sees an accurate warning while the
 * already-created account remains intact.
 */
@Component
public class StaffAccountCredentialEmailListener {

    private final EmailService emailService;

    public StaffAccountCredentialEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @EventListener
    public void scheduleInitialCredentials(StaffAccountCreatedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendInitialCredentials(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendInitialCredentials(event);
            }
        });
    }

    private void sendInitialCredentials(StaffAccountCreatedEvent event) {
        emailService.sendStaffAccountCredentials(
                event.getRecipientEmail(),
                event.getRecipientName(),
                event.getUsername(),
                event.getRawPassword());
    }
}
