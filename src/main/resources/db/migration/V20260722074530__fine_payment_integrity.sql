IF COL_LENGTH('dbo.Transactions', 'created_at') IS NULL
BEGIN
    EXEC('ALTER TABLE dbo.Transactions ADD created_at DATETIME2(6) NULL');
    EXEC('UPDATE dbo.Transactions SET created_at = transaction_date WHERE created_at IS NULL');
    EXEC('ALTER TABLE dbo.Transactions ALTER COLUMN created_at DATETIME2(6) NOT NULL');
    EXEC('ALTER TABLE dbo.Transactions ADD CONSTRAINT DF_Transactions_created DEFAULT (SYSDATETIME()) FOR created_at');
END;

IF COL_LENGTH('dbo.Transactions', 'paid_at') IS NULL
BEGIN
    EXEC('ALTER TABLE dbo.Transactions ADD paid_at DATETIME2(6) NULL');
    EXEC('UPDATE dbo.Transactions SET paid_at = transaction_date WHERE UPPER(status) IN (''COMPLETED'', ''PAID'')');
END;

-- Remove only the known fine demo chain. Business/member data is untouched.
DELETE fi
FROM dbo.PayOSPaymentFineItems fi
JOIN dbo.Transactions t ON t.transaction_id = fi.fine_transaction_id
WHERE t.reference_code IN ('DEMO-FINE-0001', 'DEMO-FINE-0002', 'DEMO-DAMAGE-0015');

DELETE al
FROM dbo.PayOSPaymentAuditLogs al
JOIN dbo.PayOSPayments p ON p.payment_id = al.payment_id
WHERE p.payment_link_id = 'demo-link-0004';

DELETE FROM dbo.PayOSPayments WHERE payment_link_id = 'demo-link-0004';
DELETE FROM dbo.Transactions
WHERE reference_code IN ('DEMO-FINE-0001', 'DEMO-FINE-0002', 'DEMO-DAMAGE-0015');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Transactions_one_fine_per_detail')
BEGIN
    CREATE UNIQUE INDEX UX_Transactions_one_fine_per_detail
        ON dbo.Transactions(borrow_detail_id, transaction_type)
        WHERE borrow_detail_id IS NOT NULL AND transaction_type IN ('FINE','DAMAGE_FEE');
END;
