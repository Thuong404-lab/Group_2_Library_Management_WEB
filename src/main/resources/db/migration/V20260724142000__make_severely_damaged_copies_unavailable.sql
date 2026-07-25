ALTER TABLE dbo.BookItems DROP CONSTRAINT CK_BookItems_unavailable_condition;

UPDATE dbo.BookItems
SET status = 'Unavailable',
    updated_at = SYSDATETIME()
WHERE status = 'Available'
  AND (
      LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE '%severely%'
      OR LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE '%severe damage%'
      OR LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE N'%hư hỏng nặng%'
      OR LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE '%lost%'
      OR LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE N'%mất sách%'
  );

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

ALTER TABLE dbo.BookItems ADD CONSTRAINT CK_BookItems_unavailable_condition
CHECK (
    (
        status = 'Unavailable'
        AND (
            LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE '%severely%'
            OR LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE '%severe damage%'
            OR LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE N'%hư hỏng nặng%'
            OR LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE '%lost%'
            OR LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) LIKE N'%mất sách%'
        )
    )
    OR
    (
        status <> 'Unavailable'
        AND LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) NOT LIKE '%severely%'
        AND LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) NOT LIKE '%severe damage%'
        AND LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) NOT LIKE N'%hư hỏng nặng%'
        AND LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) NOT LIKE '%lost%'
        AND LOWER(LTRIM(RTRIM(COALESCE(book_condition, '')))) NOT LIKE N'%mất sách%'
    )
);
