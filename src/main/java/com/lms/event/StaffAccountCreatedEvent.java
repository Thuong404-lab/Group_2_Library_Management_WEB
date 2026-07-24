package com.lms.event;

/**
 * Carries the one-time credentials required to notify a newly created
 * librarian after the account transaction commits.
 *
 * <p>The raw password is intentionally not exposed through {@code toString}
 * and must never be persisted or logged.</p>
 */
public final class StaffAccountCreatedEvent {

    private final String recipientEmail;
    private final String recipientName;
    private final String username;
    private final String rawPassword;

    public StaffAccountCreatedEvent(String recipientEmail,
            String recipientName,
            String username,
            String rawPassword) {
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.username = username;
        this.rawPassword = rawPassword;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getUsername() {
        return username;
    }

    public String getRawPassword() {
        return rawPassword;
    }
}
