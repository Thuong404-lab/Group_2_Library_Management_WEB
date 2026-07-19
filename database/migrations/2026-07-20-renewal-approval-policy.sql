-- Adds the system expiry rejection code used by the fixed renewal policy.
-- System defaults: submit at least 24 hours before due; approval timeout is 12 hours.
SET XACT_ABORT ON;
GO

DELETE FROM dbo.SystemSettings
WHERE UPPER(setting_key) IN ('RENEWAL_MIN_HOURS_BEFORE_DUE', 'RENEWAL_APPROVAL_TIMEOUT_HOURS');
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_BorrowDetails_RejectionCode')
    ALTER TABLE dbo.BorrowDetails DROP CONSTRAINT CK_BorrowDetails_RejectionCode;
GO

ALTER TABLE dbo.BorrowDetails ADD CONSTRAINT CK_BorrowDetails_RejectionCode CHECK (
    rejection_code IS NULL OR rejection_code IN (
        'RESERVED_BY_OTHER', 'OVERDUE', 'RENEWAL_LIMIT_REACHED',
        'ACCOUNT_RESTRICTED', 'BOOK_RECALL', 'APPROVAL_EXPIRED',
        'RETURNED_BEFORE_APPROVAL', 'OTHER'
    )
);
GO