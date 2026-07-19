-- Adds structured rejection reasons for online borrow, renewal, and reservation decisions.
-- Safe to run more than once before starting the application (Hibernate ddl-auto=validate).
SET XACT_ABORT ON;
GO

IF COL_LENGTH('dbo.Borrows', 'rejection_code') IS NULL
    ALTER TABLE dbo.Borrows ADD rejection_code VARCHAR(50) NULL;
IF COL_LENGTH('dbo.Borrows', 'rejection_reason') IS NULL
    ALTER TABLE dbo.Borrows ADD rejection_reason NVARCHAR(500) NULL;
GO

IF COL_LENGTH('dbo.BorrowDetails', 'rejection_code') IS NULL
    ALTER TABLE dbo.BorrowDetails ADD rejection_code VARCHAR(50) NULL;
IF COL_LENGTH('dbo.BorrowDetails', 'rejection_reason') IS NULL
    ALTER TABLE dbo.BorrowDetails ADD rejection_reason NVARCHAR(500) NULL;
GO

IF COL_LENGTH('dbo.Reservations', 'rejection_code') IS NULL
    ALTER TABLE dbo.Reservations ADD rejection_code VARCHAR(50) NULL;
IF COL_LENGTH('dbo.Reservations', 'rejection_reason') IS NULL
    ALTER TABLE dbo.Reservations ADD rejection_reason NVARCHAR(500) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Borrows_RejectionCode')
    ALTER TABLE dbo.Borrows ADD CONSTRAINT CK_Borrows_RejectionCode CHECK (
        rejection_code IS NULL OR rejection_code IN (
            'NO_COPY', 'LIMIT_EXCEEDED', 'ACCOUNT_RESTRICTED',
            'OUTSTANDING_OBLIGATION', 'INVALID_INFORMATION', 'OTHER'
        )
    );
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_BorrowDetails_RejectionCode')
    ALTER TABLE dbo.BorrowDetails ADD CONSTRAINT CK_BorrowDetails_RejectionCode CHECK (
        rejection_code IS NULL OR rejection_code IN (
            'RESERVED_BY_OTHER', 'OVERDUE', 'RENEWAL_LIMIT_REACHED',
            'ACCOUNT_RESTRICTED', 'BOOK_RECALL', 'OTHER'
        )
    );
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Reservations_RejectionCode')
    ALTER TABLE dbo.Reservations ADD CONSTRAINT CK_Reservations_RejectionCode CHECK (
        rejection_code IS NULL OR rejection_code IN (
            'COPY_AVAILABLE', 'UNFULFILLABLE', 'DUPLICATE_REQUEST',
            'ACCOUNT_RESTRICTED', 'INVALID_DEPOSIT', 'OTHER'
        )
    );
GO

-- Verification: this query must return six rows.
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'dbo'
  AND TABLE_NAME IN ('Borrows', 'BorrowDetails', 'Reservations')
  AND COLUMN_NAME IN ('rejection_code', 'rejection_reason')
ORDER BY TABLE_NAME, COLUMN_NAME;
GO