/*
 * Drop foreign keys referencing LocalizationLanguages from active tables
 * so we can drop the LocalizationLanguages table itself.
 */
IF EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = N'FK_Member_Accounts_language'
      AND parent_object_id = OBJECT_ID(N'dbo.Member_Accounts')
)
    ALTER TABLE dbo.Member_Accounts
    DROP CONSTRAINT FK_Member_Accounts_language;

IF EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = N'FK_Notifications_language'
      AND parent_object_id = OBJECT_ID(N'dbo.Notifications')
)
    ALTER TABLE dbo.Notifications
    DROP CONSTRAINT FK_Notifications_language;

IF EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = N'FK_MembershipTierTranslations_language'
      AND parent_object_id = OBJECT_ID(N'dbo.MembershipTierTranslations')
)
    ALTER TABLE dbo.MembershipTierTranslations
    DROP CONSTRAINT FK_MembershipTierTranslations_language;

/*
* Drop unused translation tables that have no Java Entities
* and are not used by the application.
*/
DROP TABLE IF EXISTS dbo.CategoryTranslations;

DROP TABLE IF EXISTS dbo.GenreTranslations;

DROP TABLE IF EXISTS dbo.BookTranslations;

DROP TABLE IF EXISTS dbo.ShelfTranslations;

DROP TABLE IF EXISTS dbo.NotificationTranslations;

DROP TABLE IF EXISTS dbo.SystemSettingTranslations;

/* Finally drop LocalizationLanguages */
DROP TABLE IF EXISTS dbo.LocalizationLanguages;
