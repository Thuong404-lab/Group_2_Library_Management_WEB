SET NOCOUNT ON;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.MembershipTiers')
      AND name = N'UX_MembershipTiers_condition'
)
BEGIN
    CREATE UNIQUE INDEX UX_MembershipTiers_condition
        ON dbo.MembershipTiers([condition]);
END;

/* Remove a benefit claim that has no implementation in the reservation queue. */
UPDATE dbo.MembershipTiers
SET benefits = N'15% borrowing-fee discount and a 15-book concurrent borrowing limit.'
WHERE tier_name = N'Diamond'
  AND benefits LIKE N'%reservation priority%';

UPDATE translation
SET benefits = CASE translation.language_code
    WHEN N'vi' THEN N'Giảm 15% phí mượn; được mượn đồng thời tối đa 15 cuốn.'
    ELSE N'15% borrowing-fee discount and a 15-book concurrent borrowing limit.'
END
FROM dbo.MembershipTierTranslations translation
JOIN dbo.MembershipTiers tier ON tier.tier_id = translation.tier_id
WHERE tier.tier_name = N'Diamond';

/* Rebuild persisted membership tiers from eligible, completed service spending. */
WITH MemberSpend AS (
    SELECT member.member_id,
           COALESCE(SUM(CASE
               WHEN UPPER(transaction_row.transaction_type) IN ('BORROW_FEE', 'RENEWAL_FEE')
                AND LOWER(transaction_row.status) IN ('completed', 'paid')
               THEN ABS(transaction_row.amount)
               ELSE 0
           END), 0) AS accumulated_spending
    FROM dbo.Members member
    LEFT JOIN dbo.Wallets wallet ON wallet.member_id = member.member_id
    LEFT JOIN dbo.Transactions transaction_row ON transaction_row.wallet_id = wallet.wallet_id
    GROUP BY member.member_id
)
UPDATE member
SET tier_id = achieved.tier_id
FROM dbo.Members member
JOIN MemberSpend spending ON spending.member_id = member.member_id
CROSS APPLY (
    SELECT TOP (1) tier.tier_id
    FROM dbo.MembershipTiers tier
    WHERE tier.[condition] <= spending.accumulated_spending
    ORDER BY tier.[condition] DESC, tier.tier_id DESC
) achieved
WHERE member.tier_id IS NULL OR member.tier_id <> achieved.tier_id;
