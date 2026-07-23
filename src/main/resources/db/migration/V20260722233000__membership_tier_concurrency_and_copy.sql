SET NOCOUNT ON;

IF COL_LENGTH('dbo.MembershipTiers', 'row_version') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipTiers
        ADD row_version BIGINT NOT NULL
            CONSTRAINT DF_MembershipTiers_row_version DEFAULT (0);
END;

UPDATE tier
SET benefits = CASE tier.tier_name
    WHEN N'Member' THEN N'Borrow up to 5 books concurrently.'
    WHEN N'Silver' THEN N'5% borrowing-fee discount and a 7-book concurrent limit.'
    WHEN N'Gold' THEN N'10% borrowing-fee discount and a 10-book concurrent limit.'
    WHEN N'Diamond' THEN N'15% borrowing-fee discount and a 15-book concurrent limit.'
    ELSE tier.benefits
END
FROM dbo.MembershipTiers tier
WHERE tier.tier_name IN (N'Member', N'Silver', N'Gold', N'Diamond');

UPDATE translation
SET benefits = CASE
    WHEN translation.language_code = N'vi' AND tier.tier_name = N'Member'
        THEN N'Được mượn đồng thời tối đa 5 cuốn.'
    WHEN translation.language_code = N'vi' AND tier.tier_name = N'Silver'
        THEN N'Giảm 5% phí mượn; được mượn đồng thời tối đa 7 cuốn.'
    WHEN translation.language_code = N'vi' AND tier.tier_name = N'Gold'
        THEN N'Giảm 10% phí mượn; được mượn đồng thời tối đa 10 cuốn.'
    WHEN translation.language_code = N'vi' AND tier.tier_name = N'Diamond'
        THEN N'Giảm 15% phí mượn; được mượn đồng thời tối đa 15 cuốn.'
    WHEN translation.language_code = N'en' AND tier.tier_name = N'Member'
        THEN N'Borrow up to 5 books concurrently.'
    WHEN translation.language_code = N'en' AND tier.tier_name = N'Silver'
        THEN N'5% borrowing-fee discount and a 7-book concurrent limit.'
    WHEN translation.language_code = N'en' AND tier.tier_name = N'Gold'
        THEN N'10% borrowing-fee discount and a 10-book concurrent limit.'
    WHEN translation.language_code = N'en' AND tier.tier_name = N'Diamond'
        THEN N'15% borrowing-fee discount and a 15-book concurrent limit.'
    ELSE translation.benefits
END
FROM dbo.MembershipTierTranslations translation
JOIN dbo.MembershipTiers tier ON tier.tier_id = translation.tier_id
WHERE tier.tier_name IN (N'Member', N'Silver', N'Gold', N'Diamond')
  AND translation.language_code IN (N'en', N'vi');
