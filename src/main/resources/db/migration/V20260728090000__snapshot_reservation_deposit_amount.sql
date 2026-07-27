/*
 * Reservation deposits are calculated from the daily rate at payment time.
 * Store that charged total so later configuration changes cannot alter refunds.
 */
IF COL_LENGTH('dbo.Reservations', 'deposit_amount') IS NULL
BEGIN
    ALTER TABLE dbo.Reservations
        ADD deposit_amount DECIMAL(18, 2) NULL;
END;
GO

/*
 * Backfill historical reservations by pairing each member's paid reservation
 * records with their DEPOSIT ledger entries in chronological order. The ledger
 * amount is authoritative because it is the amount actually deducted.
 */
;WITH PaidReservations AS (
    SELECT r.reservation_id,
           r.member_id,
           ROW_NUMBER() OVER (
               PARTITION BY r.member_id
               ORDER BY r.reservation_date, r.reservation_id
           ) AS payment_sequence
    FROM dbo.Reservations r
    WHERE r.deposit_amount IS NULL
      AND UPPER(r.status) IN ('DEPOSIT_PAID', 'REFUND_PENDING', 'REFUNDED', 'READY', 'ACTIVE', 'COMPLETED')
), PaidDeposits AS (
    SELECT w.member_id,
           ABS(t.amount) AS deposit_amount,
           ROW_NUMBER() OVER (
               PARTITION BY w.member_id
               ORDER BY t.transaction_date, t.transaction_id
           ) AS payment_sequence
    FROM dbo.Transactions t
    INNER JOIN dbo.Wallets w ON w.wallet_id = t.wallet_id
    WHERE UPPER(t.transaction_type) = 'DEPOSIT'
      AND LOWER(t.status) IN ('completed', 'paid')
)
UPDATE r
SET r.deposit_amount = d.deposit_amount
FROM dbo.Reservations r
INNER JOIN PaidReservations pr ON pr.reservation_id = r.reservation_id
INNER JOIN PaidDeposits d ON d.member_id = pr.member_id
                        AND d.payment_sequence = pr.payment_sequence;
GO

/* A snapshot is mandatory for all future paid reservations. */
IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_Reservations_deposit_amount_non_negative'
      AND parent_object_id = OBJECT_ID(N'dbo.Reservations')
)
BEGIN
    ALTER TABLE dbo.Reservations WITH NOCHECK
        ADD CONSTRAINT CK_Reservations_deposit_amount_non_negative
        CHECK (deposit_amount IS NULL OR deposit_amount >= 0);
END;
GO
