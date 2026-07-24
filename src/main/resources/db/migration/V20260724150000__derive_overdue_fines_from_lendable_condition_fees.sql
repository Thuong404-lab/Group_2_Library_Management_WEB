IF NOT EXISTS (
    SELECT 1 FROM dbo.SystemSettings
    WHERE UPPER(setting_key) = 'NEW_BOOK_OVERDUE_FINE'
)
BEGIN
    INSERT dbo.SystemSettings(setting_key, setting_value, description)
    SELECT 'New_Book_Overdue_Fine',
           CONVERT(NVARCHAR(100), CONVERT(DECIMAL(18,2), setting_value) * 2),
           N'Overdue fine per day for a new-condition copy; derived as twice its borrowing fee.'
    FROM dbo.SystemSettings
    WHERE UPPER(setting_key) = 'BORROW_FEE_PER_BOOK';
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.SystemSettings
    WHERE UPPER(setting_key) = 'MINOR_DAMAGE_OVERDUE_FINE'
)
BEGIN
    INSERT dbo.SystemSettings(setting_key, setting_value, description)
    SELECT 'Minor_Damage_Overdue_Fine',
           CONVERT(NVARCHAR(100), CONVERT(DECIMAL(18,2), setting_value) * 2),
           N'Overdue fine per day for a minor-damage copy; derived as twice its borrowing fee.'
    FROM dbo.SystemSettings
    WHERE UPPER(setting_key) = 'MINOR_DAMAGE_BORROW_FEE';
END;

DELETE FROM dbo.SystemSettings
WHERE UPPER(setting_key) = 'SEVERE_DAMAGE_BORROW_FEE';
