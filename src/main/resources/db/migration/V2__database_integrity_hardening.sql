/*
 * Database integrity hardening for operational and financial workflows.
 * V1 is an immutable baseline; all production-safe changes start here.
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

IF OBJECT_ID(N'dbo.DemoSeedHistory', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DemoSeedHistory (
        seed_key   VARCHAR(100) NOT NULL CONSTRAINT PK_DemoSeedHistory PRIMARY KEY,
        applied_at DATETIME2(6) NOT NULL CONSTRAINT DF_DemoSeedHistory_applied DEFAULT (SYSUTCDATETIME())
    );
END;
GO

/* Keep only the newest OPEN reconciliation issue before enforcing uniqueness. */
;WITH RankedOpenIssues AS (
    SELECT issue_id,
           ROW_NUMBER() OVER (
               PARTITION BY payment_id
               ORDER BY last_attempt_at DESC, issue_id DESC
           ) AS row_number
    FROM dbo.PayOSReconciliationIssues
    WHERE status = 'OPEN'
)
UPDATE issue
SET status = 'RESOLVED',
    resolved_at = COALESCE(
        issue.resolved_at,
        CASE WHEN SYSUTCDATETIME() < issue.first_seen_at
             THEN issue.first_seen_at ELSE SYSUTCDATETIME() END)
FROM dbo.PayOSReconciliationIssues issue
JOIN RankedOpenIssues ranked ON ranked.issue_id = issue.issue_id
WHERE ranked.row_number > 1;

UPDATE dbo.PayOSReconciliationIssues
SET resolved_at = NULL
WHERE status = 'OPEN' AND resolved_at IS NOT NULL;

UPDATE dbo.PayOSReconciliationIssues
SET resolved_at = COALESCE(
        resolved_at,
        CASE WHEN SYSUTCDATETIME() < first_seen_at
             THEN first_seen_at ELSE SYSUTCDATETIME() END)
WHERE status = 'RESOLVED' AND resolved_at IS NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.PayOSReconciliationIssues')
      AND name = N'CK_PayOSReconciliationIssues_resolution'
)
BEGIN
    ALTER TABLE dbo.PayOSReconciliationIssues WITH CHECK
    ADD CONSTRAINT CK_PayOSReconciliationIssues_resolution
        CHECK ((status = 'OPEN' AND resolved_at IS NULL)
            OR (status = 'RESOLVED' AND resolved_at IS NOT NULL));
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.PayOSReconciliationIssues')
      AND name = N'UX_PayOSReconciliationIssues_open_payment'
)
BEGIN
    CREATE UNIQUE INDEX UX_PayOSReconciliationIssues_open_payment
        ON dbo.PayOSReconciliationIssues(payment_id)
        WHERE status = 'OPEN';
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.PayOSPayments')
      AND name = N'IX_PayOSPayments_status_created'
)
BEGIN
    CREATE INDEX IX_PayOSPayments_status_created
        ON dbo.PayOSPayments(status, created_at)
        INCLUDE(order_code, transaction_id, member_id, purpose, amount);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.PayOSPaymentAuditLogs')
      AND name = N'IX_PayOSPaymentAuditLogs_created'
)
BEGIN
    CREATE INDEX IX_PayOSPaymentAuditLogs_created
        ON dbo.PayOSPaymentAuditLogs(created_at DESC)
        INCLUDE(payment_id, actor_user_id, event_type, source, successful, old_status, new_status);
END;
GO
