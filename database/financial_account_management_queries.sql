/*
  DATABASE QUERIES
  Financial and Account Management

  Scope: UC-8.1 to UC-8.4, UC-11, UC-14.1 to UC-14.3,
         UC-18.1, UC-21.1, and UC-21.2.

  SQL Server syntax. Replace the sample parameter values before execution.
  Each GO-separated use-case section can be executed independently.
*/

USE [tes];
GO

/* 1. View Transaction History - Member (UC-8.4) */
DECLARE @MemberId_84 INT = 1;
DECLARE @Type_84 VARCHAR(50) = NULL;
DECLARE @Page_84 INT = 0;
DECLARE @PageSize_84 INT = 10;

SELECT t.transaction_id, t.transaction_type, t.amount,
       t.transaction_date, t.status, t.borrow_id
FROM dbo.Transactions t
JOIN dbo.Wallets w ON w.wallet_id = t.wallet_id
WHERE w.member_id = @MemberId_84
  AND (@Type_84 IS NULL OR @Type_84 = ''
       OR UPPER(t.transaction_type) LIKE '%' + UPPER(@Type_84) + '%')
ORDER BY t.transaction_date DESC
OFFSET (@Page_84 * @PageSize_84) ROWS
FETCH NEXT @PageSize_84 ROWS ONLY;
GO

/* 2. View Member List (UC-18.1) */
DECLARE @Keyword_181 NVARCHAR(255) = N'';
DECLARE @Page_181 INT = 0;
DECLARE @PageSize_181 INT = 10;

SELECT ma.id AS account_id, ma.username, ma.status AS account_status,
       m.member_id, u.user_id, u.full_name, u.email, u.phone,
       mt.tier_id, mt.tier_name
FROM dbo.Member_Accounts ma
JOIN dbo.Members m ON m.member_id = ma.member_id
JOIN dbo.Users u ON u.user_id = m.user_id
LEFT JOIN dbo.MembershipTiers mt ON mt.tier_id = m.tier_id
WHERE @Keyword_181 = N''
   OR LOWER(ma.username) LIKE '%' + LOWER(@Keyword_181) + '%'
   OR LOWER(u.full_name) LIKE '%' + LOWER(@Keyword_181) + '%'
   OR LOWER(u.email) LIKE '%' + LOWER(@Keyword_181) + '%'
   OR u.phone LIKE '%' + @Keyword_181 + '%'
ORDER BY ma.id
OFFSET (@Page_181 * @PageSize_181) ROWS
FETCH NEXT @PageSize_181 ROWS ONLY;
GO

/* 3. Forgot Password (UC-11)
   a. Find the user by email.
   b. Create or update the password reset token. */
DECLARE @Email_11 VARCHAR(255) = 'member@example.com';
DECLARE @Token_11 VARCHAR(36) = CONVERT(VARCHAR(36), NEWID());
DECLARE @UserId_11 INT;

SELECT @UserId_11 = user_id
FROM dbo.Users
WHERE email = LTRIM(RTRIM(@Email_11));

IF @UserId_11 IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.PasswordResetTokens WHERE user_id = @UserId_11)
        UPDATE dbo.PasswordResetTokens
        SET token = @Token_11, expiry_date = DATEADD(HOUR, 24, SYSDATETIME())
        WHERE user_id = @UserId_11;
    ELSE
        INSERT INTO dbo.PasswordResetTokens(token, user_id, expiry_date)
        VALUES (@Token_11, @UserId_11, DATEADD(HOUR, 24, SYSDATETIME()));

    SELECT @Token_11 AS reset_token, DATEADD(HOUR, 24, SYSDATETIME()) AS expiry_date;
END;
GO

/* 4. Reset Password (UC-21.2)
   a. Validate the reset token.
   b. Update the BCrypt-encoded account password.
   c. Delete the used token. */
DECLARE @Token_212 VARCHAR(36) = 'replace-with-valid-token';
DECLARE @PasswordHash_212 VARCHAR(255) = '$2a$10$replaceWithEncodedPasswordHash';
DECLARE @UserId_212 INT;
DECLARE @UpdatedAccounts_212 INT = 0;

BEGIN TRY
    BEGIN TRANSACTION;

    SELECT @UserId_212 = user_id
    FROM dbo.PasswordResetTokens WITH (UPDLOCK, HOLDLOCK)
    WHERE token = @Token_212 AND expiry_date > SYSDATETIME();

    IF @UserId_212 IS NULL
        THROW 50001, 'Reset token is invalid or expired.', 1;

    UPDATE ma
    SET password_hash = @PasswordHash_212
    FROM dbo.Member_Accounts ma
    JOIN dbo.Members m ON m.member_id = ma.member_id
    WHERE m.user_id = @UserId_212;
    SET @UpdatedAccounts_212 = @@ROWCOUNT;

    IF @UpdatedAccounts_212 = 0
    BEGIN
        UPDATE sa
        SET password_hash = @PasswordHash_212
        FROM dbo.Staff_Accounts sa
        JOIN dbo.Staff s ON s.staff_id = sa.staff_id
        WHERE s.user_id = @UserId_212;
        SET @UpdatedAccounts_212 = @@ROWCOUNT;
    END;

    IF @UpdatedAccounts_212 = 0
        THROW 50002, 'No account is linked to this user.', 1;

    DELETE FROM dbo.PasswordResetTokens WHERE token = @Token_212;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* 5. View Transaction History - Librarian (UC-14.2) */
DECLARE @Type_142 VARCHAR(50) = NULL;
DECLARE @Page_142 INT = 0;
DECLARE @PageSize_142 INT = 12;

SELECT t.transaction_id, t.transaction_type, t.amount,
       t.transaction_date, t.status, t.borrow_id,
       m.member_id, u.full_name, u.email
FROM dbo.Transactions t
LEFT JOIN dbo.Wallets w ON w.wallet_id = t.wallet_id
LEFT JOIN dbo.Members m ON m.member_id = w.member_id
LEFT JOIN dbo.Users u ON u.user_id = m.user_id
WHERE @Type_142 IS NULL OR @Type_142 = ''
   OR UPPER(t.transaction_type) LIKE '%' + UPPER(@Type_142) + '%'
ORDER BY t.transaction_date DESC
OFFSET (@Page_142 * @PageSize_142) ROWS
FETCH NEXT @PageSize_142 ROWS ONLY;
GO

/* 6. Change Account Status (UC-21.1)
   a. Update the member account status.
   b. Synchronize the linked user status. */
DECLARE @AccountId_211 INT = 1;
DECLARE @Status_211 VARCHAR(50) = 'Blocked';

IF @Status_211 NOT IN ('Active', 'Inactive', 'Blocked')
    THROW 50003, 'Invalid account status.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    UPDATE dbo.Member_Accounts
    SET status = @Status_211
    WHERE id = @AccountId_211;

    IF @@ROWCOUNT = 0 THROW 50004, 'Member account not found.', 1;

    UPDATE u
    SET status = @Status_211
    FROM dbo.Users u
    JOIN dbo.Members m ON m.user_id = u.user_id
    JOIN dbo.Member_Accounts ma ON ma.member_id = m.member_id
    WHERE ma.id = @AccountId_211;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* 7. Pay Overdue Fines (UC-8.1)
   a. Find the unpaid fine.
   b. Deduct the amount from the wallet.
   c. Mark the fine transaction as completed. */
DECLARE @MemberId_81 INT = 1;
DECLARE @FineId_81 INT = 1;
DECLARE @FineAmount_81 DECIMAL(18,2);
DECLARE @WalletId_81 INT;

BEGIN TRY
    BEGIN TRANSACTION;
    SELECT @FineAmount_81 = ABS(t.amount), @WalletId_81 = t.wallet_id
    FROM dbo.Transactions t WITH (UPDLOCK, HOLDLOCK)
    JOIN dbo.Wallets w ON w.wallet_id = t.wallet_id
    WHERE t.transaction_id = @FineId_81
      AND w.member_id = @MemberId_81
      AND UPPER(t.transaction_type) IN ('FINE', 'DAMAGE_FEE')
      AND UPPER(ISNULL(t.status, '')) NOT IN ('COMPLETED', 'PAID');

    IF @FineAmount_81 IS NULL THROW 50005, 'Fine is invalid or already paid.', 1;

    UPDATE dbo.Wallets
    SET balance = balance - @FineAmount_81
    WHERE wallet_id = @WalletId_81 AND balance >= @FineAmount_81;

    IF @@ROWCOUNT = 0 THROW 50006, 'Insufficient wallet balance.', 1;

    UPDATE dbo.Transactions
    SET amount = -@FineAmount_81, status = 'Completed', transaction_date = GETDATE()
    WHERE transaction_id = @FineId_81;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* 8. Pay Borrowing Fees (UC-8.2)
   a. Calculate the borrowing fee.
   b. Check for a previous payment.
   c. Deduct the wallet balance and create a transaction. */
DECLARE @MemberId_82 INT = 1;
DECLARE @BorrowId_82 INT = 1;
DECLARE @Rate_82 DECIMAL(18,2);
DECLARE @Fee_82 DECIMAL(18,2);
DECLARE @WalletId_82 INT;

SELECT @Rate_82 = COALESCE(
    TRY_CONVERT(DECIMAL(18,2), setting_value), 5000)
FROM dbo.SystemSettings WHERE UPPER(setting_key) = 'BORROW_FEE_PER_BOOK';
SET @Rate_82 = COALESCE(NULLIF(@Rate_82, 0), 5000);

SELECT @Fee_82 = SUM(
    CASE WHEN bd.due_date > b.borrow_date
         THEN CEILING(DATEDIFF(HOUR, b.borrow_date, bd.due_date) / 24.0)
         ELSE 10 END * @Rate_82)
FROM dbo.Borrows b
JOIN dbo.BorrowDetails bd ON bd.borrow_id = b.borrow_id
WHERE b.borrow_id = @BorrowId_82 AND b.member_id = @MemberId_82
  AND UPPER(b.status) IN ('ACTIVE', 'BORROWING', 'OVERDUE');

BEGIN TRY
    BEGIN TRANSACTION;
    IF @Fee_82 IS NULL OR @Fee_82 <= 0 THROW 50007, 'Borrow fee is invalid.', 1;
    IF EXISTS (
        SELECT 1 FROM dbo.Transactions t JOIN dbo.Wallets w ON w.wallet_id = t.wallet_id
        WHERE w.member_id = @MemberId_82 AND t.borrow_id = @BorrowId_82
          AND UPPER(t.transaction_type) = 'BORROW_FEE'
          AND UPPER(t.status) IN ('COMPLETED', 'PAID'))
        THROW 50008, 'Borrow fee was already paid.', 1;

    SELECT @WalletId_82 = wallet_id FROM dbo.Wallets WITH (UPDLOCK, HOLDLOCK)
    WHERE member_id = @MemberId_82;

    UPDATE dbo.Wallets SET balance = balance - @Fee_82
    WHERE wallet_id = @WalletId_82 AND balance >= @Fee_82;
    IF @@ROWCOUNT = 0 THROW 50009, 'Insufficient wallet balance.', 1;

    INSERT INTO dbo.Transactions(wallet_id, borrow_id, transaction_type, amount, transaction_date, status)
    VALUES (@WalletId_82, @BorrowId_82, 'BORROW_FEE', -@Fee_82, GETDATE(), 'Completed');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* 9. Deposit Payment (UC-8.3)
   a. Validate the reservation.
   b. Deduct the deposit from the wallet.
   c. Update the reservation and notify the member. */
DECLARE @MemberId_83 INT = 1;
DECLARE @ReservationId_83 INT = 1;
DECLARE @Deposit_83 DECIMAL(18,2);
DECLARE @WalletId_83 INT;
DECLARE @NotificationId_83 INT;

SELECT @Deposit_83 = TRY_CONVERT(DECIMAL(18,2), setting_value)
FROM dbo.SystemSettings WHERE UPPER(setting_key) = 'DEPOSIT_AMOUNT';
SET @Deposit_83 = COALESCE(NULLIF(@Deposit_83, 0), 50000);

BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (
        SELECT 1 FROM dbo.Reservations WITH (UPDLOCK, HOLDLOCK)
        WHERE reservation_id = @ReservationId_83 AND member_id = @MemberId_83
          AND UPPER(ISNULL(status, '')) NOT IN ('DEPOSIT_PAID', 'PAID', 'COMPLETED', 'CANCELED', 'CANCELLED'))
        THROW 50010, 'Reservation cannot receive a deposit.', 1;

    SELECT @WalletId_83 = wallet_id FROM dbo.Wallets WITH (UPDLOCK, HOLDLOCK)
    WHERE member_id = @MemberId_83;
    UPDATE dbo.Wallets SET balance = balance - @Deposit_83
    WHERE wallet_id = @WalletId_83 AND balance >= @Deposit_83;
    IF @@ROWCOUNT = 0 THROW 50011, 'Insufficient wallet balance.', 1;

    INSERT INTO dbo.Transactions(wallet_id, transaction_type, amount, transaction_date, status)
    VALUES (@WalletId_83, 'DEPOSIT', -@Deposit_83, GETDATE(), 'Completed');
    UPDATE dbo.Reservations SET status = 'Deposit_Paid'
    WHERE reservation_id = @ReservationId_83;

    INSERT INTO dbo.Notifications(title, content, created_date, status)
    VALUES (N'Thanh toán tiền cọc thành công',
            N'Thư viện đã ghi nhận tiền cọc đặt trước.', GETDATE(), 'Active');
    SET @NotificationId_83 = SCOPE_IDENTITY();
    INSERT INTO dbo.MemberNotifications(member_id, notification_id, is_read, read_date)
    VALUES (@MemberId_83, @NotificationId_83, 0, NULL);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* 10. Top Up Member Account (UC-14.3)
   a. Find the member by ID, phone, email, or username.
   b. Add money to the wallet and create a transaction.
   c. Notify the member. */
DECLARE @Lookup_143 VARCHAR(255) = '1';
DECLARE @Amount_143 DECIMAL(18,2) = 100000;
DECLARE @MemberId_143 INT;
DECLARE @WalletId_143 INT;
DECLARE @NotificationId_143 INT;

SELECT TOP 1 @MemberId_143 = m.member_id
FROM dbo.Members m
JOIN dbo.Users u ON u.user_id = m.user_id
LEFT JOIN dbo.Member_Accounts ma ON ma.member_id = m.member_id
WHERE CONVERT(VARCHAR(20), m.member_id) = @Lookup_143
   OR u.phone = @Lookup_143 OR u.email = @Lookup_143 OR ma.username = @Lookup_143;

BEGIN TRY
    BEGIN TRANSACTION;
    IF @MemberId_143 IS NULL OR @Amount_143 <= 0 THROW 50012, 'Member or amount is invalid.', 1;

    SELECT @WalletId_143 = wallet_id FROM dbo.Wallets WITH (UPDLOCK, HOLDLOCK)
    WHERE member_id = @MemberId_143;
    IF @WalletId_143 IS NULL
    BEGIN
        INSERT INTO dbo.Wallets(member_id, balance) VALUES (@MemberId_143, 0);
        SET @WalletId_143 = SCOPE_IDENTITY();
    END;

    UPDATE dbo.Wallets SET balance = COALESCE(balance, 0) + @Amount_143
    WHERE wallet_id = @WalletId_143;
    INSERT INTO dbo.Transactions(wallet_id, transaction_type, amount, transaction_date, status)
    VALUES (@WalletId_143, 'TOP_UP', @Amount_143, GETDATE(), 'Completed');

    INSERT INTO dbo.Notifications(title, content, created_date, status)
    VALUES (N'Nạp tiền thành công',
            N'Tài khoản ví của bạn vừa được nạp tiền.', GETDATE(), 'Active');
    SET @NotificationId_143 = SCOPE_IDENTITY();
    INSERT INTO dbo.MemberNotifications(member_id, notification_id, is_read, read_date)
    VALUES (@MemberId_143, @NotificationId_143, 0, NULL);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* 11. Manage Fines and Violations (UC-14.1)
   a. Validate the member and fine amount.
   b. Create a pending fine transaction.
   c. Notify the member. */
DECLARE @MemberId_141 INT = 1;
DECLARE @Amount_141 DECIMAL(18,2) = 50000;
DECLARE @Reason_141 NVARCHAR(255) = N'Vi phạm quy định thư viện';
DECLARE @WalletId_141 INT;
DECLARE @NotificationId_141 INT;

BEGIN TRY
    BEGIN TRANSACTION;
    IF @Amount_141 <= 0 THROW 50013, 'Fine amount must be positive.', 1;
    SELECT @WalletId_141 = wallet_id FROM dbo.Wallets WHERE member_id = @MemberId_141;
    IF @WalletId_141 IS NULL THROW 50014, 'Member wallet not found.', 1;

    INSERT INTO dbo.Transactions(wallet_id, transaction_type, amount, transaction_date, status)
    VALUES (@WalletId_141, 'FINE', -ABS(@Amount_141), GETDATE(), 'Pending');

    INSERT INTO dbo.Notifications(title, content, created_date, status)
    VALUES (N'Phí phạt mới',
            N'Thư viện đã ghi nhận khoản phạt. Lý do: ' + @Reason_141,
            GETDATE(), 'Active');
    SET @NotificationId_141 = SCOPE_IDENTITY();

    INSERT INTO dbo.MemberNotifications(member_id, notification_id, is_read, read_date)
    VALUES (@MemberId_141, @NotificationId_141, 0, NULL);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
