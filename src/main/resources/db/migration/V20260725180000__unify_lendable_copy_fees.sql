UPDATE dbo.SystemSettings
SET description = N'Standard daily borrowing fee for new and minor-damage copies.'
WHERE UPPER(setting_key) = 'BORROW_FEE_PER_BOOK';

UPDATE overdue_fee
SET setting_value = CONVERT(varchar(100),
        TRY_CONVERT(decimal(18, 2), standard_fee.setting_value) * 2),
    description = N'Standard daily overdue fine for new and minor-damage copies.'
FROM dbo.SystemSettings overdue_fee
CROSS JOIN dbo.SystemSettings standard_fee
WHERE UPPER(overdue_fee.setting_key) = 'NEW_BOOK_OVERDUE_FINE'
  AND UPPER(standard_fee.setting_key) = 'BORROW_FEE_PER_BOOK'
  AND TRY_CONVERT(decimal(18, 2), standard_fee.setting_value) IS NOT NULL;

DELETE FROM dbo.SystemSettings
WHERE UPPER(setting_key) IN ('MINOR_DAMAGE_BORROW_FEE', 'MINOR_DAMAGE_OVERDUE_FINE');
