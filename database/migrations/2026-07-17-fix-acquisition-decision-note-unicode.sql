/*
 * Preserve Vietnamese librarian decision notes in SQL Server and repair the
 * legacy value already damaged by the former VARCHAR column.
 * Safe to run once against an existing LMW database.
 */
SET XACT_ABORT ON;
BEGIN TRANSACTION;

ALTER TABLE dbo.BookAcquisitionRequests
    ALTER COLUMN decision_note NVARCHAR(500) NULL;

UPDATE dbo.BookAcquisitionRequests
SET decision_note = N'Chưa có nhà cung cấp phù hợp.'
WHERE decision_note = N'Chua có nhà cung c?p phù h?p.';

COMMIT TRANSACTION;
