/*
 * Book acquisition workflow migration for SQL Server.
 * Change the database name below when the target environment is not "tes".
 * The script is idempotent and can be run more than once.
 */
USE tes;
GO

IF OBJECT_ID(N'dbo.BookAcquisitionRequests', N'U') IS NULL
    THROW 50001, 'Table dbo.BookAcquisitionRequests does not exist.', 1;
GO

IF COL_LENGTH('dbo.BookAcquisitionRequests', 'isbn') IS NULL
    ALTER TABLE dbo.BookAcquisitionRequests ADD isbn VARCHAR(20) NULL;
GO

IF COL_LENGTH('dbo.BookAcquisitionRequests', 'publisher') IS NULL
    ALTER TABLE dbo.BookAcquisitionRequests ADD publisher NVARCHAR(255) NULL;
GO

IF COL_LENGTH('dbo.BookAcquisitionRequests', 'publication_year') IS NULL
    ALTER TABLE dbo.BookAcquisitionRequests ADD publication_year INT NULL;
GO

IF COL_LENGTH('dbo.BookAcquisitionRequests', 'request_reason') IS NULL
    ALTER TABLE dbo.BookAcquisitionRequests ADD request_reason NVARCHAR(1000) NULL;
GO

IF COL_LENGTH('dbo.BookAcquisitionRequests', 'reference_url') IS NULL
    ALTER TABLE dbo.BookAcquisitionRequests ADD reference_url NVARCHAR(500) NULL;
GO

IF COL_LENGTH('dbo.BookAcquisitionRequests', 'status') IS NULL
BEGIN
    ALTER TABLE dbo.BookAcquisitionRequests
    ADD status VARCHAR(20) NOT NULL
        CONSTRAINT DF_BookAcquisitionRequests_Status DEFAULT 'PENDING' WITH VALUES;
END;
GO

IF COL_LENGTH('dbo.BookAcquisitionRequests', 'decision_note') IS NULL
    ALTER TABLE dbo.BookAcquisitionRequests ADD decision_note NVARCHAR(500) NULL;
GO

IF COL_LENGTH('dbo.BookAcquisitionRequests', 'processed_date') IS NULL
    ALTER TABLE dbo.BookAcquisitionRequests ADD processed_date DATETIME2 NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_BookAcquisitionRequests_Status'
      AND parent_object_id = OBJECT_ID('dbo.BookAcquisitionRequests')
)
BEGIN
    ALTER TABLE dbo.BookAcquisitionRequests WITH CHECK
    ADD CONSTRAINT CK_BookAcquisitionRequests_Status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));
END;
GO
