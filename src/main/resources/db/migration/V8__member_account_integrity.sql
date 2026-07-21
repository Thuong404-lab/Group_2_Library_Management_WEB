IF COL_LENGTH('dbo.Users', 'row_version') IS NULL
BEGIN
    ALTER TABLE dbo.Users
        ADD row_version BIGINT NOT NULL
            CONSTRAINT DF_Users_row_version DEFAULT (0);
END;

IF COL_LENGTH('dbo.Member_Accounts', 'row_version') IS NULL
BEGIN
    ALTER TABLE dbo.Member_Accounts
        ADD row_version BIGINT NOT NULL
            CONSTRAINT DF_Member_Accounts_row_version DEFAULT (0);
END;

INSERT dbo.Wallets(member_id, balance)
SELECT m.member_id, 0
FROM dbo.Members m
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Wallets w WHERE w.member_id = m.member_id
);

DECLARE @MemberRoleId INT = (
    SELECT TOP (1) role_id
    FROM dbo.Roles
    WHERE UPPER(name) = 'ROLE_MEMBER'
    ORDER BY role_id
);

IF @MemberRoleId IS NOT NULL
BEGIN
    INSERT dbo.Member_Account_Roles(member_account_id, role_id)
    SELECT ma.id, @MemberRoleId
    FROM dbo.Member_Accounts ma
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.Member_Account_Roles mar
        WHERE mar.member_account_id = ma.id
          AND mar.role_id = @MemberRoleId
    );
END;
