/* Run once on SQL Server when spring.jpa.hibernate.ddl-auto is not "update". */
IF OBJECT_ID(N'dbo.PayOSPayments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PayOSPayments (
        payment_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        member_id INT NOT NULL,
        transaction_id INT NULL,
        purpose VARCHAR(30) NOT NULL,
        reference_id INT NULL,
        amount DECIMAL(18,2) NOT NULL,
        order_code BIGINT NOT NULL,
        payment_link_id VARCHAR(100) NULL,
        checkout_url VARCHAR(1000) NULL,
        qr_code NVARCHAR(MAX) NULL,
        status VARCHAR(30) NOT NULL,
        bank_reference VARCHAR(100) NULL,
        created_at DATETIME2 NOT NULL,
        paid_at DATETIME2 NULL,
        CONSTRAINT uk_payos_payment_order_code UNIQUE (order_code),
        CONSTRAINT fk_payos_payment_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id),
        CONSTRAINT fk_payos_payment_transaction FOREIGN KEY (transaction_id) REFERENCES dbo.Transactions(transaction_id)
    );

    CREATE INDEX ix_payos_payment_member_created
        ON dbo.PayOSPayments(member_id, created_at DESC);
    CREATE INDEX ix_payos_payment_reference
        ON dbo.PayOSPayments(purpose, reference_id, status);
END;
GO

IF OBJECT_ID(N'dbo.PayOSPaymentAuditLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PayOSPaymentAuditLogs (
        audit_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        payment_id BIGINT NOT NULL,
        actor_user_id INT NULL,
        event_type VARCHAR(50) NOT NULL,
        source VARCHAR(30) NOT NULL,
        old_status VARCHAR(30) NULL,
        new_status VARCHAR(30) NULL,
        successful BIT NOT NULL,
        message NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT fk_payos_audit_payment FOREIGN KEY (payment_id) REFERENCES dbo.PayOSPayments(payment_id),
        CONSTRAINT fk_payos_audit_actor FOREIGN KEY (actor_user_id) REFERENCES dbo.Users(user_id)
    );
    CREATE INDEX ix_payos_audit_payment_created
        ON dbo.PayOSPaymentAuditLogs(payment_id, created_at DESC);
    CREATE INDEX ix_payos_audit_event_created
        ON dbo.PayOSPaymentAuditLogs(event_type, created_at DESC);
END;
GO

IF OBJECT_ID(N'dbo.PayOSReconciliationIssues', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PayOSReconciliationIssues (
        issue_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        payment_id BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL,
        attempt_count INT NOT NULL DEFAULT 0,
        first_seen_at DATETIME2 NOT NULL,
        last_attempt_at DATETIME2 NOT NULL,
        resolved_at DATETIME2 NULL,
        error_message NVARCHAR(MAX) NULL,
        CONSTRAINT fk_payos_reconciliation_payment FOREIGN KEY (payment_id) REFERENCES dbo.PayOSPayments(payment_id)
    );
    CREATE INDEX ix_payos_reconciliation_status_attempt
        ON dbo.PayOSReconciliationIssues(status, last_attempt_at DESC);
    CREATE INDEX ix_payos_reconciliation_payment
        ON dbo.PayOSReconciliationIssues(payment_id);
END;
GO

IF OBJECT_ID(N'dbo.PayOSPaymentFineItems', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PayOSPaymentFineItems (
        item_id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        payment_id BIGINT NOT NULL,
        fine_transaction_id INT NOT NULL,
        amount_snapshot DECIMAL(18,2) NOT NULL,
        CONSTRAINT fk_payos_fine_item_payment FOREIGN KEY (payment_id) REFERENCES dbo.PayOSPayments(payment_id),
        CONSTRAINT fk_payos_fine_item_transaction FOREIGN KEY (fine_transaction_id) REFERENCES dbo.Transactions(transaction_id)
    );
    CREATE INDEX ix_payos_fine_item_payment ON dbo.PayOSPaymentFineItems(payment_id);
END;
GO
