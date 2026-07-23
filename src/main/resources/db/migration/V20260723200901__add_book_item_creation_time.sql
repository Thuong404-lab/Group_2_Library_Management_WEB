/*
 * Preserve the creation time, not only the calendar date, for each physical
 * book copy.
 */
IF EXISTS (
    SELECT 1
    FROM sys.default_constraints
    WHERE name = 'DF_BookItems_added'
      AND parent_object_id = OBJECT_ID(N'dbo.BookItems')
)
    ALTER TABLE dbo.BookItems DROP CONSTRAINT DF_BookItems_added;

ALTER TABLE dbo.BookItems
ALTER COLUMN added_date DATETIME2(6) NOT NULL;

ALTER TABLE dbo.BookItems ADD CONSTRAINT DF_BookItems_added
DEFAULT (SYSUTCDATETIME()) FOR added_date;
