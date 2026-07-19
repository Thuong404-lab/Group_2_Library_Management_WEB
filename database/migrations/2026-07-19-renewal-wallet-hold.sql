SET QUOTED_IDENTIFIER ON;
GO
-- Run once before starting the application (Hibernate ddl-auto=validate).
IF COL_LENGTH('dbo.Transactions', 'borrow_detail_id') IS NULL
    ALTER TABLE dbo.Transactions ADD borrow_detail_id INT NULL;
IF COL_LENGTH('dbo.Transactions', 'renewal_days') IS NULL
    ALTER TABLE dbo.Transactions ADD renewal_days INT NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Transactions_BorrowDetails_Renewal')
    ALTER TABLE dbo.Transactions ADD CONSTRAINT FK_Transactions_BorrowDetails_Renewal
        FOREIGN KEY (borrow_detail_id) REFERENCES dbo.BorrowDetails(borrow_detail_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Transactions_PendingRenewal' AND object_id = OBJECT_ID('dbo.Transactions'))
    CREATE UNIQUE INDEX UX_Transactions_PendingRenewal ON dbo.Transactions(borrow_detail_id)
    WHERE borrow_detail_id IS NOT NULL AND transaction_type = 'RENEWAL_FEE' AND status = 'Pending';
