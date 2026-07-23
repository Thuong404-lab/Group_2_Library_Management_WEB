/*
 * Drop foreign keys referencing LocalizationLanguages from active tables 
 * so we can drop the LocalizationLanguages table itself.
 */
ALTER TABLE dbo.Member_Accounts
DROP CONSTRAINT FK_Member_Accounts_language;

ALTER TABLE dbo.Notifications
DROP CONSTRAINT FK_Notifications_language;

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