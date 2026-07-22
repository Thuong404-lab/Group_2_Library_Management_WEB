UPDATE dbo.BookItems
SET book_condition = CASE
    WHEN status = 'Lost' OR LOWER(book_condition) LIKE '%lost%' OR book_condition LIKE N'%Mất sách%' THEN N'Lost book'
    WHEN status = 'Damaged' OR LOWER(book_condition) LIKE '%severely%' OR book_condition LIKE N'%Hư hỏng nặng%' THEN N'Severely damaged'
    WHEN status = 'MinorDamaged' OR LOWER(book_condition) LIKE '%minor%' OR book_condition LIKE N'%Hư hỏng nhẹ%' THEN N'Minor damage'
    ELSE N'New'
END;
