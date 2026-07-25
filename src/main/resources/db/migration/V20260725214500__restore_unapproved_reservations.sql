/*
 * Ready was previously assigned automatically when a copy became available,
 * although no librarian had approved the reservation yet. Approval creates an
 * Active reservation, so remaining Ready rows are unapproved deposits.
 */
UPDATE Reservations
SET status = 'Deposit_Paid'
WHERE status = 'Ready';
