USE LibraryManagementWeb;
GO

IF OBJECT_ID('PasswordResetTokens', 'U') IS NULL
BEGIN
    CREATE TABLE PasswordResetTokens (
        reset_token_id INT IDENTITY (1, 1) PRIMARY KEY,
        token VARCHAR(36) UNIQUE NOT NULL,
        user_id INT UNIQUE NOT NULL
            FOREIGN KEY REFERENCES Users (user_id) ON DELETE CASCADE,
        expiry_date DATETIME2 NOT NULL
    );
END
GO
