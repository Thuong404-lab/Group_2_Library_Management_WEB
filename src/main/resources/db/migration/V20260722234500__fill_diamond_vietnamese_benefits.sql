SET NOCOUNT ON;

UPDATE translation
SET benefits = N'Giảm 15% phí mượn; được mượn đồng thời tối đa 15 cuốn.'
FROM dbo.MembershipTierTranslations translation
JOIN dbo.MembershipTiers tier ON tier.tier_id = translation.tier_id
WHERE tier.tier_name = N'Diamond'
  AND translation.language_code = N'vi'
  AND (translation.benefits IS NULL OR LTRIM(RTRIM(translation.benefits)) = N'');
