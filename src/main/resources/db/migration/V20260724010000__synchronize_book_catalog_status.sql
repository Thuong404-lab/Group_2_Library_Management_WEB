UPDATE book
SET book.status = CASE
    WHEN EXISTS (
        SELECT 1
        FROM dbo.BookItems item
        WHERE item.book_id = book.book_id
          AND item.status <> 'Unavailable'
    ) THEN 'Active'
    ELSE 'Inactive'
END
FROM dbo.Books book;
