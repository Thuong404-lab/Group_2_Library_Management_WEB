/*
 * Repair reservations approved under the old pickup flow. Once an active loan
 * exists for the same member and title, the reservation has been fulfilled.
 */
UPDATE r
SET r.status = 'Completed'
FROM Reservations r
WHERE r.status = 'Active'
  AND EXISTS (
      SELECT 1
      FROM Borrows b
      INNER JOIN BorrowDetails bd ON bd.borrow_id = b.borrow_id
      WHERE b.member_id = r.member_id
        AND bd.book_id = r.book_id
        AND b.status = 'Active'
        AND bd.status = 'Borrowed'
  );
