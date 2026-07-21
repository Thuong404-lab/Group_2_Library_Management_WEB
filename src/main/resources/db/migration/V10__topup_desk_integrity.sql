/* Make librarian KQPay top-ups idempotent and attributable to a staff member. */
ALTER TABLE dbo.PayOSPayments ADD
    request_key           VARCHAR(48) NULL,
    initiated_by_staff_id INT NULL;
GO

ALTER TABLE dbo.PayOSPayments ADD CONSTRAINT FK_PayOSPayments_initiated_staff
    FOREIGN KEY (initiated_by_staff_id) REFERENCES dbo.Staff(staff_id);
GO

CREATE UNIQUE INDEX UX_PayOSPayments_request_key
    ON dbo.PayOSPayments(request_key)
    WHERE request_key IS NOT NULL;

CREATE INDEX IX_PayOSPayments_active_topup
    ON dbo.PayOSPayments(member_id, purpose, status, created_at DESC)
    INCLUDE(amount, order_code, request_key);
