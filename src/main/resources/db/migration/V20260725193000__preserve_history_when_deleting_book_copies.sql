/*
 * Completed borrowing history belongs to the title and member even after a
 * physical copy is removed from inventory. Active circulation states still
 * require a concrete BookItem.
 */
IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_BorrowDetails_copy_required'
      AND parent_object_id = OBJECT_ID(N'dbo.BorrowDetails')
)
    ALTER TABLE dbo.BorrowDetails DROP CONSTRAINT CK_BorrowDetails_copy_required;

/*
 * Legacy data contains historical Borrowed rows without a physical-copy id.
 * NOCHECK preserves that history while the CHECK constraint is still enforced
 * for every new or updated borrowing detail.
 */
ALTER TABLE dbo.BorrowDetails WITH NOCHECK ADD CONSTRAINT CK_BorrowDetails_copy_required
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
