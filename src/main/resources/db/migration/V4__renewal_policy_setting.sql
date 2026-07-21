IF NOT EXISTS (SELECT 1 FROM dbo.SystemSettings WHERE UPPER(setting_key) = 'RENEWAL_APPROVAL_TIMEOUT_HOURS')
BEGIN
    INSERT INTO dbo.SystemSettings(setting_key, setting_value, description)
    VALUES ('RENEWAL_APPROVAL_TIMEOUT_HOURS', '12', N'Số giờ tối đa để thủ thư xử lý yêu cầu gia hạn.');
END;
