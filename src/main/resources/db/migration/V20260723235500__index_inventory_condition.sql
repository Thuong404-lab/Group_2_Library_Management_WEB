IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_BookItems_condition_book'
      AND object_id = OBJECT_ID(N'dbo.BookItems')
)
BEGIN
    CREATE INDEX IX_BookItems_condition_book
        ON dbo.BookItems(book_condition, book_id)
        INCLUDE(status, barcode, shelf_id);
END;
