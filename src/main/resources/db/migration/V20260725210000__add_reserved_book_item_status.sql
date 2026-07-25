ALTER TABLE BookItems DROP CONSTRAINT CK_BookItems_status;

ALTER TABLE BookItems ADD CONSTRAINT CK_BookItems_status
CHECK (status IN ('Available', 'Reserved', 'Borrowed', 'Payment_Pending', 'Waiting_Pickup', 'Unavailable'));
