IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_BookItems_status')
    ALTER TABLE dbo.BookItems DROP CONSTRAINT CK_BookItems_status;

ALTER TABLE dbo.BookItems ALTER COLUMN book_condition NVARCHAR(100) NULL;

UPDATE dbo.BookItems
SET status = 'MinorDamaged',
    book_condition = N'Minor damage (Bent corners, worn cover, or small tears)',
    updated_at = SYSUTCDATETIME()
WHERE status = 'Disposed';

ALTER TABLE dbo.BookItems ADD CONSTRAINT CK_BookItems_status
CHECK (status IN ('Available','Borrowed','Waiting_Pickup','Payment_Pending','Lost','Damaged','MinorDamaged'));
