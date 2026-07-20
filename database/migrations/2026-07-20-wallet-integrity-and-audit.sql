SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF EXISTS (SELECT 1 FROM dbo.Wallets WHERE member_id IS NULL)
    THROW 51000, 'Cannot enforce wallet ownership: Wallets contains rows without member_id.', 1;

IF EXISTS (SELECT member_id FROM dbo.Wallets GROUP BY member_id HAVING COUNT(*) > 1)
    THROW 51001, 'Cannot enforce one wallet per member: duplicate Wallets.member_id values exist.', 1;

IF COL_LENGTH('dbo.Transactions', 'reference_code') IS NULL
    ALTER TABLE dbo.Transactions ADD reference_code varchar(64) NULL;
IF COL_LENGTH('dbo.Transactions', 'performed_by_staff_id') IS NULL
    ALTER TABLE dbo.Transactions ADD performed_by_staff_id int NULL;
IF COL_LENGTH('dbo.Transactions', 'channel') IS NULL
    ALTER TABLE dbo.Transactions ADD channel varchar(20) NULL;
IF COL_LENGTH('dbo.Transactions', 'balance_before') IS NULL
    ALTER TABLE dbo.Transactions ADD balance_before decimal(18,2) NULL;
IF COL_LENGTH('dbo.Transactions', 'balance_after') IS NULL
    ALTER TABLE dbo.Transactions ADD balance_after decimal(18,2) NULL;
IF COL_LENGTH('dbo.BorrowDetails', 'condition_code') IS NULL
    ALTER TABLE dbo.BorrowDetails ADD condition_code varchar(20) NULL;

UPDATE dbo.BorrowDetails
SET condition_code = CASE
    WHEN condition_note LIKE N'%Mất%' OR LOWER(condition_note) LIKE '%lost%' THEN 'LOST'
    WHEN condition_note LIKE N'%Hư hỏng%' OR LOWER(condition_note) LIKE '%damage%' THEN 'DAMAGED'
    WHEN condition_note IS NOT NULL THEN 'GOOD'
    ELSE NULL
END
WHERE condition_code IS NULL;

ALTER TABLE dbo.Wallets ALTER COLUMN member_id int NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Wallets_member_id' AND object_id = OBJECT_ID('dbo.Wallets'))
    CREATE UNIQUE INDEX UX_Wallets_member_id ON dbo.Wallets(member_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Transactions_reference_code' AND object_id = OBJECT_ID('dbo.Transactions'))
    CREATE UNIQUE INDEX UX_Transactions_reference_code ON dbo.Transactions(reference_code) WHERE reference_code IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Transactions_performed_by_staff')
    ALTER TABLE dbo.Transactions ADD CONSTRAINT FK_Transactions_performed_by_staff
        FOREIGN KEY (performed_by_staff_id) REFERENCES dbo.Staff(staff_id);

COMMIT TRANSACTION;
