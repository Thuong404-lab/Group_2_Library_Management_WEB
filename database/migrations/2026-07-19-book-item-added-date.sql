-- Adds the automatically populated date on which each physical book copy entered inventory.
IF COL_LENGTH('dbo.BookItems', 'added_date') IS NULL
BEGIN
    ALTER TABLE dbo.BookItems ADD added_date DATE NULL;
END;
GO

UPDATE dbo.BookItems
SET added_date = CONVERT(date, GETDATE())
WHERE added_date IS NULL;
GO

ALTER TABLE dbo.BookItems ALTER COLUMN added_date DATE NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
    JOIN sys.columns c
      ON c.object_id = dc.parent_object_id
     AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID('dbo.BookItems')
      AND c.name = 'added_date'
)
BEGIN
    ALTER TABLE dbo.BookItems
        ADD CONSTRAINT DF_BookItems_AddedDate
        DEFAULT (CONVERT(date, GETDATE())) FOR added_date;
END;
GO
