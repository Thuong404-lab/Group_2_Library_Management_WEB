-- Normalizes optional rejection notes: blank text means no note and is stored as NULL.
-- Safe to run repeatedly.
SET XACT_ABORT ON;
GO

UPDATE dbo.Borrows
SET rejection_reason = NULL
WHERE rejection_reason IS NOT NULL
  AND LTRIM(RTRIM(rejection_reason)) = N'';
GO

UPDATE dbo.BorrowDetails
SET rejection_reason = NULL
WHERE rejection_reason IS NOT NULL
  AND LTRIM(RTRIM(rejection_reason)) = N'';
GO

UPDATE dbo.Reservations
SET rejection_reason = NULL
WHERE rejection_reason IS NOT NULL
  AND LTRIM(RTRIM(rejection_reason)) = N'';
GO

-- Verification: each count must be zero.
SELECT 'Borrows' AS table_name, COUNT(*) AS blank_rejection_reason_count
FROM dbo.Borrows
WHERE rejection_reason IS NOT NULL AND LTRIM(RTRIM(rejection_reason)) = N''
UNION ALL
SELECT 'BorrowDetails', COUNT(*)
FROM dbo.BorrowDetails
WHERE rejection_reason IS NOT NULL AND LTRIM(RTRIM(rejection_reason)) = N''
UNION ALL
SELECT 'Reservations', COUNT(*)
FROM dbo.Reservations
WHERE rejection_reason IS NOT NULL AND LTRIM(RTRIM(rejection_reason)) = N'';
GO