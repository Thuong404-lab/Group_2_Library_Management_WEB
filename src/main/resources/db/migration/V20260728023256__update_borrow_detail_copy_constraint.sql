/*
 * Some shared databases contain legacy active borrow details without a
 * physical-copy id. Keep those existing rows, while requiring a BookItem for
 * every newly inserted or updated active circulation row.
 */
IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_BorrowDetails_copy_required'
      AND parent_object_id = OBJECT_ID(N'dbo.BorrowDetails')
)
    ALTER TABLE dbo.BorrowDetails DROP CONSTRAINT CK_BorrowDetails_copy_required;

ALTER TABLE dbo.BorrowDetails WITH NOCHECK
ADD CONSTRAINT CK_BorrowDetails_copy_required
CHECK (
    [status] NOT IN (
        'Waiting_Pickup',
        'Payment_Pending',
        'Borrowed',
        'Overdue',
        'Return_Pending',
        'Renew_Pending'
    )
    OR book_item_id IS NOT NULL
);
