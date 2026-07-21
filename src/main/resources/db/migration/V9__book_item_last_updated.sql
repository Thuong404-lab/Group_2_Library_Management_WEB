IF COL_LENGTH('dbo.BookItems', 'updated_at') IS NULL
BEGIN
    ALTER TABLE dbo.BookItems ADD updated_at DATETIME2(0) NULL;
    EXEC(N'UPDATE dbo.BookItems SET updated_at = CAST(added_date AS DATETIME2(0)) WHERE updated_at IS NULL');
    EXEC(N'ALTER TABLE dbo.BookItems ALTER COLUMN updated_at DATETIME2(0) NOT NULL');
    EXEC(N'ALTER TABLE dbo.BookItems ADD CONSTRAINT DF_BookItems_updated_at DEFAULT SYSUTCDATETIME() FOR updated_at');
END;
