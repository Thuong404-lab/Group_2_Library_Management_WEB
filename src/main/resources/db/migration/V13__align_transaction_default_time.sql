/*
 * Java writes LocalDateTime in the library/server local time. Keep SQL-created
 * transactions on the same convention. Historical values are intentionally
 * left untouched because their original source timezone cannot be inferred.
 */
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Transactions_date')
    ALTER TABLE dbo.Transactions DROP CONSTRAINT DF_Transactions_date;
GO

ALTER TABLE dbo.Transactions ADD CONSTRAINT DF_Transactions_date
    DEFAULT (SYSDATETIME()) FOR transaction_date;
