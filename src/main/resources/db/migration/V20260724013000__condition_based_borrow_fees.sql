IF NOT EXISTS (SELECT 1 FROM dbo.SystemSettings WHERE UPPER(setting_key) = 'MINOR_DAMAGE_BORROW_FEE')
BEGIN
    INSERT INTO dbo.SystemSettings(setting_key, setting_value, description)
    VALUES ('Minor_Damage_Borrow_Fee', '4000', N'Daily borrowing fee for a copy with minor damage.');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.SystemSettings WHERE UPPER(setting_key) = 'SEVERE_DAMAGE_BORROW_FEE')
BEGIN
    INSERT INTO dbo.SystemSettings(setting_key, setting_value, description)
    VALUES ('Severe_Damage_Borrow_Fee', '3000', N'Daily borrowing fee for a severely damaged copy.');
END;
