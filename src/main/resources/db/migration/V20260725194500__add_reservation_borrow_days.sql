IF COL_LENGTH('dbo.Reservations', 'number_of_days') IS NULL
    ALTER TABLE dbo.Reservations
    ADD number_of_days INT NOT NULL
        CONSTRAINT DF_Reservations_number_of_days DEFAULT (14);

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'CK_Reservations_number_of_days'
      AND parent_object_id = OBJECT_ID(N'dbo.Reservations')
)
    EXEC(N'ALTER TABLE dbo.Reservations
           ADD CONSTRAINT CK_Reservations_number_of_days
           CHECK (number_of_days BETWEEN 1 AND 365)');
