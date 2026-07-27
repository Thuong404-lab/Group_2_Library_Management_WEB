-- Start the configurable renewal fee at the current borrowing fee.
-- Administrators can change it independently afterward.
IF EXISTS (
    SELECT 1
    FROM dbo.SystemSettings
    WHERE UPPER(setting_key) = 'RENEWAL_FEE_PER_DAY'
)
BEGIN
    UPDATE renewal
    SET renewal.setting_value = borrowing.setting_value,
        renewal.description = N'Daily renewal fee per book'
    FROM dbo.SystemSettings renewal
    CROSS JOIN dbo.SystemSettings borrowing
    WHERE UPPER(renewal.setting_key) = 'RENEWAL_FEE_PER_DAY'
      AND UPPER(borrowing.setting_key) = 'BORROW_FEE_PER_BOOK';
END
ELSE
BEGIN
    INSERT INTO dbo.SystemSettings (setting_key, setting_value, description)
    SELECT N'RENEWAL_FEE_PER_DAY',
           borrowing.setting_value,
           N'Daily renewal fee per book'
    FROM dbo.SystemSettings borrowing
    WHERE UPPER(borrowing.setting_key) = 'BORROW_FEE_PER_BOOK';
END;
