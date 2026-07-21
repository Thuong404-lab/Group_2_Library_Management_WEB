/* Make acquisition requests auditable, deduplicated and concurrency-safe. */
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

ALTER TABLE dbo.BookAcquisitionRequests ADD
    dedup_key             NVARCHAR(600) NULL,
    processed_by_staff_id INT NULL,
    version               BIGINT NOT NULL CONSTRAINT DF_Acquisition_version DEFAULT (0);

EXEC(N'UPDATE dbo.BookAcquisitionRequests
SET author = COALESCE(NULLIF(LTRIM(RTRIM(author)), N''''), N''Chưa xác định''),
    request_reason = COALESCE(NULLIF(LTRIM(RTRIM(request_reason)), N''''), N''Dữ liệu đề xuất cũ chưa có lý do.''),
    dedup_key = CASE
        WHEN NULLIF(LTRIM(RTRIM(isbn)), '''') IS NOT NULL
            THEN N''ISBN:'' + UPPER(REPLACE(REPLACE(LTRIM(RTRIM(isbn)), ''-'', ''''), '' '', ''''))
        ELSE N''TEXT:'' + LOWER(LTRIM(RTRIM(title))) + N''|'' +
             LOWER(COALESCE(NULLIF(LTRIM(RTRIM(author)), N''''), N''Chưa xác định''))
    END');

UPDATE dbo.BookAcquisitionRequests
SET decision_note = N'Chưa có nhà cung cấp phù hợp.'
WHERE decision_note = N'Chua có nhà cung c?p phù h?p.';

EXEC(N'ALTER TABLE dbo.BookAcquisitionRequests ALTER COLUMN author NVARCHAR(255) NOT NULL');
EXEC(N'ALTER TABLE dbo.BookAcquisitionRequests ALTER COLUMN request_reason NVARCHAR(1000) NOT NULL');
EXEC(N'ALTER TABLE dbo.BookAcquisitionRequests ALTER COLUMN dedup_key NVARCHAR(600) NOT NULL');

ALTER TABLE dbo.BookAcquisitionRequests DROP CONSTRAINT CK_BookAcquisitionRequests_status;
ALTER TABLE dbo.BookAcquisitionRequests DROP CONSTRAINT CK_BookAcquisitionRequests_year;
ALTER TABLE dbo.BookAcquisitionRequests DROP CONSTRAINT CK_BookAcquisitionRequests_decision;

EXEC(N'ALTER TABLE dbo.BookAcquisitionRequests ADD
    CONSTRAINT CK_Acquisition_status
        CHECK (status IN (N''PENDING'', N''APPROVED'', N''REJECTED'', N''CANCELLED'')),
    CONSTRAINT CK_Acquisition_year
        CHECK (publication_year IS NULL OR publication_year BETWEEN 1000 AND 9999),
    CONSTRAINT CK_Acquisition_decision
        CHECK (
            (status = N''PENDING'' AND processed_date IS NULL AND decision_note IS NULL AND processed_by_staff_id IS NULL)
            OR (status = N''APPROVED'' AND processed_date IS NOT NULL
                AND decision_note IS NOT NULL AND LEN(decision_note) BETWEEN 5 AND 500)
            OR (status = N''REJECTED'' AND processed_date IS NOT NULL
                AND decision_note IS NOT NULL AND LEN(decision_note) BETWEEN 5 AND 500)
            OR (status = N''CANCELLED'' AND processed_date IS NOT NULL AND decision_note IS NULL
                AND processed_by_staff_id IS NULL)
        ),
    CONSTRAINT FK_Acquisition_processed_staff
        FOREIGN KEY (processed_by_staff_id) REFERENCES dbo.Staff(staff_id)');

EXEC(N'ALTER TABLE dbo.BookAcquisitionRequests ADD active_dedup_key AS
    (CONVERT(NVARCHAR(650), CASE
        WHEN status IN (N''PENDING'', N''APPROVED'') THEN dedup_key
        ELSE dedup_key + N''|HISTORY:'' + CONVERT(NVARCHAR(20), request_id)
    END)) PERSISTED');

EXEC(N'CREATE UNIQUE INDEX UX_Acquisition_member_active_key
    ON dbo.BookAcquisitionRequests(member_id, active_dedup_key)');

CREATE INDEX IX_Acquisition_search
    ON dbo.BookAcquisitionRequests(status, created_date DESC)
    INCLUDE(title, author, isbn, member_id, processed_date);
