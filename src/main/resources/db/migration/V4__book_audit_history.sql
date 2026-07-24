/*
 * Store the latest book update time and the complete Hibernate Envers history.
 * REVTSTMP is Unix epoch milliseconds, as required by Envers.
 */
ALTER TABLE dbo.Books
    ADD updated_at DATETIME2(6) NOT NULL
        CONSTRAINT DF_Books_updated_at DEFAULT (SYSUTCDATETIME());

CREATE TABLE dbo.REVINFO (
    REV      INT IDENTITY(1,1) NOT NULL,
    REVTSTMP BIGINT NULL,
    CONSTRAINT PK_REVINFO PRIMARY KEY (REV)
);

CREATE TABLE dbo.Books_AUD (
    book_id         INT NOT NULL,
    REV             INT NOT NULL,
    REVTYPE         TINYINT NULL,
    title           NVARCHAR(255) NULL,
    isbn            VARCHAR(20) NULL,
    [description]   NVARCHAR(MAX) NULL,
    cover_image_url VARCHAR(500) NULL,
    [status]        VARCHAR(50) NULL,
    updated_at      DATETIME2(6) NULL,
    CONSTRAINT PK_Books_AUD PRIMARY KEY (book_id, REV),
    CONSTRAINT FK_Books_AUD_REVINFO FOREIGN KEY (REV) REFERENCES dbo.REVINFO(REV),
    CONSTRAINT CK_Books_AUD_REVTYPE CHECK (REVTYPE IN (0, 1, 2))
);

CREATE INDEX IX_Books_AUD_REV ON dbo.Books_AUD(REV);
