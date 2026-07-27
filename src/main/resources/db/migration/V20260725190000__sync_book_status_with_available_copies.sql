/*
 * A title is Active only while at least one physical copy is Available.
 * Keep this invariant at database level because copy statuses are changed by
 * inventory, borrowing, returns, reservations, payment and expiry workflows.
 */
UPDATE book
SET book.[status] = CASE
    WHEN EXISTS (
        SELECT 1
        FROM dbo.BookItems item
        WHERE item.book_id = book.book_id
          AND UPPER(LTRIM(RTRIM(item.[status]))) = 'AVAILABLE'
    ) THEN 'Active'
    ELSE 'Inactive'
END
FROM dbo.Books book;

EXEC(N'
CREATE OR ALTER TRIGGER dbo.TR_BookItems_SyncBookStatus
ON dbo.BookItems
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH affected_books AS (
        SELECT book_id FROM inserted
        UNION
        SELECT book_id FROM deleted
    )
    UPDATE book
    SET book.[status] = CASE
        WHEN EXISTS (
            SELECT 1
            FROM dbo.BookItems item
            WHERE item.book_id = book.book_id
              AND UPPER(LTRIM(RTRIM(item.[status]))) = ''AVAILABLE''
        ) THEN ''Active''
        ELSE ''Inactive''
    END
    FROM dbo.Books book
    INNER JOIN affected_books affected ON affected.book_id = book.book_id;
END
');
