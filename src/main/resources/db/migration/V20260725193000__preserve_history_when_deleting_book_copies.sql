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

ALTER TABLE dbo.BorrowDetails ADD CONSTRAINT CK_BorrowDetails_copy_required
CHECK (
    [status] IN ('Pending', 'Returned', 'Rejected', 'Canceled', 'Cancelled')
    OR book_item_id IS NOT NULL
);
