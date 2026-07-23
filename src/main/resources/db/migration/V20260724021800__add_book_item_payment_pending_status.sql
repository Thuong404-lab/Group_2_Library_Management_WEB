/*
 * Keep a copy reserved while its borrowing-fee payment is still pending.
 * Waiting_Pickup remains reserved for loans that have already been approved
 * and are waiting for the member to collect the physical copy.
 */
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_BookItems_status')
    ALTER TABLE dbo.BookItems DROP CONSTRAINT CK_BookItems_status;

UPDATE bi
SET bi.status = 'Payment_Pending'
FROM dbo.BookItems bi
INNER JOIN dbo.BorrowDetails bd ON bd.book_item_id = bi.book_item_id
INNER JOIN dbo.Borrows b ON b.borrow_id = bd.borrow_id
WHERE b.status = 'Payment_Pending'
  AND bd.status = 'Payment_Pending'
  AND bi.status = 'Waiting_Pickup';

ALTER TABLE dbo.BookItems ADD CONSTRAINT CK_BookItems_status
CHECK (status IN ('Available', 'Borrowed', 'Payment_Pending', 'Waiting_Pickup', 'Unavailable'));
