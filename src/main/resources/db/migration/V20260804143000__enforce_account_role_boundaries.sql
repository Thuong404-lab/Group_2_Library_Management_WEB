/*
 * Keep authentication domains separate even when data is written outside the
 * application. Member accounts may only be members; staff authority follows
 * Staff.staff_type.
 */
DECLARE @MemberRoleId INT = (SELECT role_id FROM dbo.Roles WHERE name = 'ROLE_MEMBER');
DECLARE @AdminRoleId INT = (SELECT role_id FROM dbo.Roles WHERE name = 'ROLE_ADMIN');
DECLARE @LibrarianRoleId INT = (SELECT role_id FROM dbo.Roles WHERE name = 'ROLE_LIBRARIAN');

IF @MemberRoleId IS NULL OR @AdminRoleId IS NULL OR @LibrarianRoleId IS NULL
    THROW 51000, 'ROLE_MEMBER, ROLE_ADMIN and ROLE_LIBRARIAN must exist before enforcing role boundaries.', 1;

/* Repair legacy mappings before constraints are enabled. */
DELETE mapping
FROM dbo.Member_Account_Roles mapping
WHERE mapping.role_id <> @MemberRoleId;

INSERT INTO dbo.Member_Account_Roles(member_account_id, role_id)
SELECT account.id, @MemberRoleId
FROM dbo.Member_Accounts account
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.Member_Account_Roles mapping
    WHERE mapping.member_account_id = account.id
      AND mapping.role_id = @MemberRoleId
);

DELETE mapping
FROM dbo.Staff_Account_Roles mapping
INNER JOIN dbo.Staff_Accounts account ON account.id = mapping.staff_account_id
INNER JOIN dbo.Staff staff ON staff.staff_id = account.staff_id
WHERE mapping.role_id <> CASE
    WHEN staff.staff_type = 'Admin' THEN @AdminRoleId
    ELSE @LibrarianRoleId
END;

INSERT INTO dbo.Staff_Account_Roles(staff_account_id, role_id)
SELECT account.id,
       CASE WHEN staff.staff_type = 'Admin' THEN @AdminRoleId ELSE @LibrarianRoleId END
FROM dbo.Staff_Accounts account
INNER JOIN dbo.Staff staff ON staff.staff_id = account.staff_id
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.Staff_Account_Roles mapping
    WHERE mapping.staff_account_id = account.id
      AND mapping.role_id = CASE
          WHEN staff.staff_type = 'Admin' THEN @AdminRoleId
          ELSE @LibrarianRoleId
      END
);

IF EXISTS (
    SELECT 1 FROM dbo.Members member
    INNER JOIN dbo.Staff staff ON staff.user_id = member.user_id
)
    THROW 51000, 'A user cannot be both a member and a staff profile.', 1;

IF EXISTS (
    SELECT 1 FROM dbo.Member_Accounts member_account
    INNER JOIN dbo.Staff_Accounts staff_account
        ON LOWER(member_account.username) = LOWER(staff_account.username)
)
    THROW 51000, 'Member and staff usernames must be unique across account domains.', 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Roles_supported_name'
)
    ALTER TABLE dbo.Roles
        ADD CONSTRAINT CK_Roles_supported_name
        CHECK (name IN ('ROLE_ADMIN', 'ROLE_LIBRARIAN', 'ROLE_MEMBER'));

EXEC(N'
CREATE OR ALTER TRIGGER dbo.TR_MemberAccountRoles_MemberOnly
ON dbo.Member_Account_Roles
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM inserted changed
        INNER JOIN dbo.Roles role ON role.role_id = changed.role_id
        WHERE role.name <> ''ROLE_MEMBER''
    )
        THROW 51001, ''Member accounts may only have ROLE_MEMBER.'', 1;
END
');

EXEC(N'
CREATE OR ALTER TRIGGER dbo.TR_StaffAccountRoles_MatchStaffType
ON dbo.Staff_Account_Roles
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM inserted changed
        INNER JOIN dbo.Staff_Accounts account ON account.id = changed.staff_account_id
        INNER JOIN dbo.Staff staff ON staff.staff_id = account.staff_id
        INNER JOIN dbo.Roles role ON role.role_id = changed.role_id
        WHERE role.name <> CASE
            WHEN staff.staff_type = ''Admin'' THEN ''ROLE_ADMIN''
            WHEN staff.staff_type = ''Librarian'' THEN ''ROLE_LIBRARIAN''
            ELSE ''__INVALID_STAFF_TYPE__''
        END
    )
        THROW 51002, ''Staff account role must match Staff.staff_type.'', 1;
END
');

EXEC(N'
CREATE OR ALTER TRIGGER dbo.TR_Members_SeparateFromStaff
ON dbo.Members
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted changed
        INNER JOIN dbo.Staff staff ON staff.user_id = changed.user_id
    )
        THROW 51003, ''A user cannot be both member and staff.'', 1;
END
');

EXEC(N'
CREATE OR ALTER TRIGGER dbo.TR_Staff_SeparateFromMembers
ON dbo.Staff
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted changed
        INNER JOIN dbo.Members member ON member.user_id = changed.user_id
    )
        THROW 51003, ''A user cannot be both staff and member.'', 1;
END
');

EXEC(N'
CREATE OR ALTER TRIGGER dbo.TR_MemberAccounts_UniqueStaffUsername
ON dbo.Member_Accounts
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted changed
        INNER JOIN dbo.Staff_Accounts account
            ON LOWER(account.username) = LOWER(changed.username)
    )
        THROW 51004, ''Username already belongs to a staff account.'', 1;
END
');

EXEC(N'
CREATE OR ALTER TRIGGER dbo.TR_StaffAccounts_UniqueMemberUsername
ON dbo.Staff_Accounts
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1 FROM inserted changed
        INNER JOIN dbo.Member_Accounts account
            ON LOWER(account.username) = LOWER(changed.username)
    )
        THROW 51004, ''Username already belongs to a member account.'', 1;
END
');
