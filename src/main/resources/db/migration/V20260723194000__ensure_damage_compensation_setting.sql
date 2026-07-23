/*
 * The return desk always renders the compensation preview, including when the
 * selected book is in good condition. Databases created from Flyway migrations
 * do not execute db/seed/data.sql, so this required setting can be absent even
 * though the seed database contains it.
 *
 * Keep the configured value when it is a valid positive amount. Repair only a
 * missing, blank, non-numeric, zero, or negative value with the project default
 * already used by the seed data and the public policy page.
 */
IF NOT EXISTS (
    SELECT 1
    FROM dbo.SystemSettings
    WHERE UPPER(setting_key) = 'DAMAGE_COMPENSATION_AMOUNT'
)
BEGIN
    INSERT INTO dbo.SystemSettings(setting_key, setting_value, description)
    VALUES (
        'Damage_Compensation_Amount',
        N'120000',
        N'Default compensation for lost or severely damaged books.'
    );
END;

UPDATE dbo.SystemSettings
SET setting_value = N'120000',
    description = COALESCE(
        NULLIF(LTRIM(RTRIM(description)), N''),
        N'Default compensation for lost or severely damaged books.'
    )
WHERE UPPER(setting_key) = 'DAMAGE_COMPENSATION_AMOUNT'
  AND (
      TRY_CONVERT(DECIMAL(18, 2), LTRIM(RTRIM(setting_value))) IS NULL
      OR TRY_CONVERT(DECIMAL(18, 2), LTRIM(RTRIM(setting_value))) <= 0
  );
