/*
 * Book-copy timestamps are displayed and compared at second precision.
 */
ALTER TABLE dbo.BookItems
ALTER COLUMN added_date DATETIME2(0) NOT NULL;

ALTER TABLE dbo.BookItems
ALTER COLUMN updated_at DATETIME2(0) NOT NULL;
