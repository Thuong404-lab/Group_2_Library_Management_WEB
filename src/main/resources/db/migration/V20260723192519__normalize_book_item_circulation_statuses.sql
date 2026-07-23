/*
 * BookItems.status describes circulation only. Physical condition remains in
 * BookItems.book_condition.
 */
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_BookItems_status')
    ALTER TABLE dbo.BookItems DROP CONSTRAINT CK_BookItems_status;

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_BookItems_damage_note')
    ALTER TABLE dbo.BookItems DROP CONSTRAINT CK_BookItems_damage_note;

UPDATE dbo.BookItems
SET status = CASE
    WHEN book_condition = N'Lost book' OR status IN ('Lost', 'Disposed') THEN 'Unavailable'
    WHEN status = 'Payment_Pending' THEN 'Waiting_Pickup'
    WHEN status IN ('Damaged', 'MinorDamaged') THEN 'Available'
    ELSE status
END;

UPDATE dbo.BookItems
SET book_condition = N'Lost book'
WHERE status = 'Unavailable';

ALTER TABLE dbo.BookItems ADD CONSTRAINT CK_BookItems_status
CHECK (status IN ('Available', 'Borrowed', 'Waiting_Pickup', 'Unavailable'));

ALTER TABLE dbo.BookItems ADD CONSTRAINT CK_BookItems_unavailable_condition
CHECK (
    (status = 'Unavailable' AND book_condition = N'Lost book')
    OR (status <> 'Unavailable' AND book_condition <> N'Lost book')
);
