SET QUOTED_IDENTIFIER ON;
GO
-- Backfill display-only refund rows for renewal holds refunded before separate REFUND records existed.
INSERT INTO dbo.Transactions (wallet_id, borrow_id, borrow_detail_id, renewal_days, transaction_type, amount, transaction_date, status)
SELECT t.wallet_id, t.borrow_id, t.borrow_detail_id, t.renewal_days, 'REFUND', ABS(t.amount), t.transaction_date, 'Completed'
FROM dbo.Transactions t
WHERE t.transaction_type = 'RENEWAL_FEE'
  AND UPPER(t.status) = 'REFUNDED'
  AND NOT EXISTS (
      SELECT 1 FROM dbo.Transactions r
      WHERE r.transaction_type = 'REFUND'
        AND r.borrow_detail_id = t.borrow_detail_id
        AND r.amount = ABS(t.amount)
  );