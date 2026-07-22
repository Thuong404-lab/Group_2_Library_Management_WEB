/* Preserve review history, support moderation feedback, and detect concurrent edits. */
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

ALTER TABLE dbo.Feedbacks ADD
    moderation_reason NVARCHAR(500) NULL,
    moderated_date    DATETIME2(6) NULL,
    version           BIGINT NOT NULL CONSTRAINT DF_Feedbacks_version DEFAULT (0);

EXEC(N'ALTER TABLE dbo.Feedbacks ADD CONSTRAINT CK_Feedbacks_moderation_reason
    CHECK (moderation_reason IS NULL OR LEN(moderation_reason) BETWEEN 5 AND 500)');

/* Only one non-deleted review may exist for a member/book pair. */
CREATE UNIQUE INDEX UX_Feedbacks_member_book_active
    ON dbo.Feedbacks(member_id, book_id)
    WHERE status <> 'DELETED_BY_MEMBER';
