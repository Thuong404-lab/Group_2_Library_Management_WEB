/* A browser retry or double click must not create a second notification campaign. */
ALTER TABLE dbo.Notifications ADD request_key VARCHAR(36) NULL;
GO

CREATE UNIQUE INDEX UX_Notifications_request_key
    ON dbo.Notifications(request_key)
    WHERE request_key IS NOT NULL;
