/*
 * Flyway V1 baseline for Library Management Web on SQL Server 2022+.
 * The target database must already exist. This migration is append-only:
 * never edit it after release; add V2, V3... for future schema changes.
 */
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

CREATE TABLE dbo.Users (
    user_id     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Users PRIMARY KEY,
    full_name   NVARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    phone       VARCHAR(20) NULL,
    status      VARCHAR(50) NOT NULL CONSTRAINT DF_Users_status DEFAULT ('Active'),
    avatar      NVARCHAR(500) NULL,
    CONSTRAINT UQ_Users_email UNIQUE (email),
    CONSTRAINT CK_Users_name CHECK (LEN(LTRIM(RTRIM(full_name))) >= 2),
    CONSTRAINT CK_Users_email CHECK (email LIKE '%_@_%._%'),
    CONSTRAINT CK_Users_status CHECK (status IN ('Active','Inactive','Blocked')),
    CONSTRAINT CK_Users_phone CHECK (phone IS NULL OR (LEN(phone) BETWEEN 9 AND 15 AND phone NOT LIKE '%[^0-9+]%'))
);
CREATE UNIQUE INDEX UX_Users_phone ON dbo.Users(phone) WHERE phone IS NOT NULL;

CREATE TABLE dbo.Roles (
    role_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Roles PRIMARY KEY,
    name    VARCHAR(50) NOT NULL CONSTRAINT UQ_Roles_name UNIQUE,
    CONSTRAINT CK_Roles_name CHECK (name IN ('ROLE_ADMIN','ROLE_LIBRARIAN','ROLE_MEMBER'))
);

CREATE TABLE dbo.LocalizationLanguages (
    language_code NVARCHAR(5) NOT NULL CONSTRAINT PK_LocalizationLanguages PRIMARY KEY,
    english_name  NVARCHAR(100) NOT NULL,
    native_name   NVARCHAR(100) NOT NULL,
    is_default    BIT NOT NULL CONSTRAINT DF_LocalizationLanguages_default DEFAULT (0),
    is_active     BIT NOT NULL CONSTRAINT DF_LocalizationLanguages_active DEFAULT (1),
    CONSTRAINT CK_LocalizationLanguages_code CHECK (language_code IN (N'en',N'vi'))
);
CREATE UNIQUE INDEX UX_LocalizationLanguages_default ON dbo.LocalizationLanguages(is_default) WHERE is_default=1;

CREATE TABLE dbo.MembershipTiers (
    tier_id          INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MembershipTiers PRIMARY KEY,
    tier_name        NVARCHAR(100) NOT NULL CONSTRAINT UQ_MembershipTiers_name UNIQUE,
    discount_percent DECIMAL(5,2) NOT NULL CONSTRAINT DF_MembershipTiers_discount DEFAULT (0),
    borrow_limit     INT NOT NULL CONSTRAINT DF_MembershipTiers_limit DEFAULT (5),
    [condition]      DECIMAL(18,2) NOT NULL CONSTRAINT DF_MembershipTiers_condition DEFAULT (0),
    benefits         NVARCHAR(MAX) NULL,
    CONSTRAINT CK_MembershipTiers_discount CHECK (discount_percent BETWEEN 0 AND 100),
    CONSTRAINT CK_MembershipTiers_limit CHECK (borrow_limit BETWEEN 1 AND 100),
    CONSTRAINT CK_MembershipTiers_condition CHECK ([condition] >= 0)
);

CREATE TABLE dbo.Staff (
    staff_id  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Staff PRIMARY KEY,
    user_id   INT NOT NULL,
    staff_type VARCHAR(50) NOT NULL,
    CONSTRAINT UQ_Staff_user UNIQUE (user_id),
    CONSTRAINT FK_Staff_user FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id),
    CONSTRAINT CK_Staff_type CHECK (staff_type IN ('Admin','Librarian'))
);

CREATE TABLE dbo.Members (
    member_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Members PRIMARY KEY,
    user_id   INT NOT NULL,
    tier_id   INT NULL,
    CONSTRAINT UQ_Members_user UNIQUE (user_id),
    CONSTRAINT FK_Members_user FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id),
    CONSTRAINT FK_Members_tier FOREIGN KEY (tier_id) REFERENCES dbo.MembershipTiers(tier_id)
);
CREATE INDEX IX_Members_tier ON dbo.Members(tier_id);

CREATE TABLE dbo.Staff_Accounts (
    id            INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Staff_Accounts PRIMARY KEY,
    staff_id      INT NOT NULL,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(50) NOT NULL CONSTRAINT DF_Staff_Accounts_status DEFAULT ('Active'),
    CONSTRAINT UQ_Staff_Accounts_staff UNIQUE (staff_id),
    CONSTRAINT UQ_Staff_Accounts_username UNIQUE (username),
    CONSTRAINT FK_Staff_Accounts_staff FOREIGN KEY (staff_id) REFERENCES dbo.Staff(staff_id),
    CONSTRAINT CK_Staff_Accounts_password CHECK (LEN(password_hash) >= 59),
    CONSTRAINT CK_Staff_Accounts_status CHECK (status IN ('Active','Inactive','Blocked'))
);

CREATE TABLE dbo.Member_Accounts (
    id                 INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Member_Accounts PRIMARY KEY,
    member_id          INT NOT NULL,
    username           VARCHAR(100) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    status             VARCHAR(50) NOT NULL CONSTRAINT DF_Member_Accounts_status DEFAULT ('Active'),
    preferred_language NVARCHAR(5) NOT NULL CONSTRAINT DF_Member_Accounts_language DEFAULT (N'en'),
    CONSTRAINT UQ_Member_Accounts_member UNIQUE (member_id),
    CONSTRAINT UQ_Member_Accounts_username UNIQUE (username),
    CONSTRAINT FK_Member_Accounts_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id),
    CONSTRAINT FK_Member_Accounts_language FOREIGN KEY (preferred_language) REFERENCES dbo.LocalizationLanguages(language_code),
    CONSTRAINT CK_Member_Accounts_password CHECK (LEN(password_hash) >= 59),
    CONSTRAINT CK_Member_Accounts_status CHECK (status IN ('Active','Inactive','Blocked')),
    CONSTRAINT CK_Member_Accounts_language CHECK (preferred_language IN (N'en',N'vi'))
);

CREATE TABLE dbo.Staff_Account_Roles (
    staff_account_id INT NOT NULL,
    role_id           INT NOT NULL,
    CONSTRAINT PK_Staff_Account_Roles PRIMARY KEY (staff_account_id, role_id),
    CONSTRAINT FK_StaffAccountRoles_account FOREIGN KEY (staff_account_id) REFERENCES dbo.Staff_Accounts(id) ON DELETE CASCADE,
    CONSTRAINT FK_StaffAccountRoles_role FOREIGN KEY (role_id) REFERENCES dbo.Roles(role_id)
);

CREATE TABLE dbo.Member_Account_Roles (
    member_account_id INT NOT NULL,
    role_id            INT NOT NULL,
    CONSTRAINT PK_Member_Account_Roles PRIMARY KEY (member_account_id, role_id),
    CONSTRAINT FK_MemberAccountRoles_account FOREIGN KEY (member_account_id) REFERENCES dbo.Member_Accounts(id) ON DELETE CASCADE,
    CONSTRAINT FK_MemberAccountRoles_role FOREIGN KEY (role_id) REFERENCES dbo.Roles(role_id)
);

CREATE TABLE dbo.Wallets (
    wallet_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Wallets PRIMARY KEY,
    member_id INT NOT NULL,
    balance   DECIMAL(18,2) NOT NULL CONSTRAINT DF_Wallets_balance DEFAULT (0),
    CONSTRAINT UQ_Wallets_member UNIQUE (member_id),
    CONSTRAINT FK_Wallets_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id),
    CONSTRAINT CK_Wallets_balance CHECK (balance BETWEEN 0 AND 1000000000)
);

CREATE TABLE dbo.Categories (
    category_id   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Categories PRIMARY KEY,
    category_name NVARCHAR(255) NOT NULL CONSTRAINT UQ_Categories_name UNIQUE
);

CREATE TABLE dbo.Genres (
    genre_id   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Genres PRIMARY KEY,
    category_id INT NULL,
    genre_name NVARCHAR(255) NOT NULL,
    CONSTRAINT FK_Genres_category FOREIGN KEY (category_id) REFERENCES dbo.Categories(category_id),
    CONSTRAINT UQ_Genres_category_name UNIQUE (category_id, genre_name)
);

CREATE TABLE dbo.Authors (
    author_id   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Authors PRIMARY KEY,
    author_name NVARCHAR(255) NOT NULL CONSTRAINT UQ_Authors_name UNIQUE
);

CREATE TABLE dbo.Shelves (
    shelf_id   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Shelves PRIMARY KEY,
    shelf_name NVARCHAR(100) NOT NULL CONSTRAINT UQ_Shelves_name UNIQUE,
    location   NVARCHAR(255) NULL
);

CREATE TABLE dbo.Books (
    book_id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Books PRIMARY KEY,
    genre_id        INT NULL,
    title           NVARCHAR(255) NOT NULL,
    isbn            VARCHAR(20) NULL,
    description     NVARCHAR(MAX) NULL,
    status          VARCHAR(50) NOT NULL CONSTRAINT DF_Books_status DEFAULT ('Active'),
    cover_image_url VARCHAR(500) NULL,
    CONSTRAINT FK_Books_genre FOREIGN KEY (genre_id) REFERENCES dbo.Genres(genre_id),
    CONSTRAINT CK_Books_status CHECK (status IN ('Active','Inactive'))
);
CREATE UNIQUE INDEX UX_Books_isbn ON dbo.Books(isbn) WHERE isbn IS NOT NULL;
CREATE INDEX IX_Books_genre_status ON dbo.Books(genre_id, status) INCLUDE(title, isbn);

CREATE TABLE dbo.BookAuthors (
    book_id   INT NOT NULL,
    author_id INT NOT NULL,
    CONSTRAINT PK_BookAuthors PRIMARY KEY (book_id, author_id),
    CONSTRAINT FK_BookAuthors_book FOREIGN KEY (book_id) REFERENCES dbo.Books(book_id) ON DELETE CASCADE,
    CONSTRAINT FK_BookAuthors_author FOREIGN KEY (author_id) REFERENCES dbo.Authors(author_id)
);
CREATE INDEX IX_BookAuthors_author ON dbo.BookAuthors(author_id, book_id);

CREATE TABLE dbo.BookItems (
    book_item_id  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookItems PRIMARY KEY,
    book_id       INT NOT NULL,
    shelf_id      INT NULL,
    barcode       VARCHAR(50) NOT NULL CONSTRAINT UQ_BookItems_barcode UNIQUE,
    status        VARCHAR(50) NOT NULL CONSTRAINT DF_BookItems_status DEFAULT ('Available'),
    book_condition NVARCHAR(50) NULL CONSTRAINT DF_BookItems_condition DEFAULT (N'Mới'),
    damage_note   NVARCHAR(255) NULL,
    added_date    DATE NOT NULL CONSTRAINT DF_BookItems_added DEFAULT (CONVERT(date,SYSUTCDATETIME())),
    CONSTRAINT FK_BookItems_book FOREIGN KEY (book_id) REFERENCES dbo.Books(book_id),
    CONSTRAINT FK_BookItems_shelf FOREIGN KEY (shelf_id) REFERENCES dbo.Shelves(shelf_id),
    CONSTRAINT UQ_BookItems_id_book UNIQUE (book_item_id, book_id),
    CONSTRAINT CK_BookItems_status CHECK (status IN ('Available','Borrowed','Waiting_Pickup','Payment_Pending','Lost','Damaged','Disposed')),
    CONSTRAINT CK_BookItems_damage_note CHECK (status NOT IN ('Lost','Damaged') OR damage_note IS NOT NULL)
);
CREATE INDEX IX_BookItems_book_status ON dbo.BookItems(book_id, status, book_item_id) INCLUDE(barcode, shelf_id);
CREATE INDEX IX_BookItems_shelf ON dbo.BookItems(shelf_id);

CREATE TABLE dbo.BookDisposals (
    disposal_id  INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookDisposals PRIMARY KEY,
    book_item_id INT NOT NULL,
    staff_id     INT NOT NULL,
    reason       NVARCHAR(MAX) NULL,
    disposal_date DATETIME2(6) NOT NULL CONSTRAINT DF_BookDisposals_date DEFAULT (SYSUTCDATETIME()),
    status       VARCHAR(50) NOT NULL CONSTRAINT DF_BookDisposals_status DEFAULT ('Completed'),
    CONSTRAINT FK_BookDisposals_item FOREIGN KEY (book_item_id) REFERENCES dbo.BookItems(book_item_id),
    CONSTRAINT FK_BookDisposals_staff FOREIGN KEY (staff_id) REFERENCES dbo.Staff(staff_id),
    CONSTRAINT CK_BookDisposals_status CHECK (status IN ('Pending','Completed','Cancelled'))
);
CREATE INDEX IX_BookDisposals_item ON dbo.BookDisposals(book_item_id);

CREATE TABLE dbo.Borrows (
    borrow_id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Borrows PRIMARY KEY,
    member_id        INT NOT NULL,
    staff_id         INT NULL,
    borrow_date      DATETIME2(6) NOT NULL CONSTRAINT DF_Borrows_date DEFAULT (SYSUTCDATETIME()),
    status           VARCHAR(50) NOT NULL CONSTRAINT DF_Borrows_status DEFAULT ('Active'),
    rejection_code   VARCHAR(50) NULL,
    rejection_reason NVARCHAR(500) NULL,
    CONSTRAINT FK_Borrows_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id),
    CONSTRAINT FK_Borrows_staff FOREIGN KEY (staff_id) REFERENCES dbo.Staff(staff_id),
    CONSTRAINT CK_Borrows_status CHECK (status IN ('Pending','Waiting_Pickup','Payment_Pending','Payment_Cancelled','Payment_Expired','Active','Return_Pending','Returned','Overdue','Rejected','Canceled','Cancelled')),
    CONSTRAINT CK_Borrows_rejection_code CHECK (rejection_code IS NULL OR rejection_code IN ('NO_COPY','LIMIT_EXCEEDED','ACCOUNT_RESTRICTED','OUTSTANDING_OBLIGATION','INVALID_INFORMATION','APPROVAL_EXPIRED','OTHER')),
    CONSTRAINT CK_Borrows_rejection_reason CHECK (rejection_reason IS NULL OR LEN(LTRIM(RTRIM(rejection_reason))) > 0)
);
CREATE INDEX IX_Borrows_member_date ON dbo.Borrows(member_id, borrow_date DESC) INCLUDE(status, staff_id);
CREATE INDEX IX_Borrows_status_date ON dbo.Borrows(status, borrow_date DESC) INCLUDE(member_id);

CREATE TABLE dbo.BorrowDetails (
    borrow_detail_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BorrowDetails PRIMARY KEY,
    borrow_id         INT NOT NULL,
    book_id           INT NOT NULL,
    book_item_id      INT NULL,
    due_date          DATETIME2(6) NOT NULL,
    return_date       DATETIME2(6) NULL,
    renew_count       INT NOT NULL CONSTRAINT DF_BorrowDetails_renew DEFAULT (0),
    status            VARCHAR(50) NOT NULL CONSTRAINT DF_BorrowDetails_status DEFAULT ('Borrowed'),
    condition_note    NVARCHAR(255) NULL,
    rejection_code    VARCHAR(50) NULL,
    rejection_reason  NVARCHAR(500) NULL,
    condition_code    VARCHAR(20) NULL,
    CONSTRAINT FK_BorrowDetails_borrow FOREIGN KEY (borrow_id) REFERENCES dbo.Borrows(borrow_id),
    CONSTRAINT FK_BorrowDetails_book FOREIGN KEY (book_id) REFERENCES dbo.Books(book_id),
    CONSTRAINT FK_BorrowDetails_item_book FOREIGN KEY (book_item_id, book_id) REFERENCES dbo.BookItems(book_item_id, book_id),
    CONSTRAINT CK_BorrowDetails_renew CHECK (renew_count BETWEEN 0 AND 20),
    CONSTRAINT CK_BorrowDetails_status CHECK (status IN ('Pending','Waiting_Pickup','Payment_Pending','Borrowed','Overdue','Return_Pending','Renew_Pending','Returned','Rejected','Canceled','Cancelled')),
    CONSTRAINT CK_BorrowDetails_copy_required CHECK (status IN ('Pending','Rejected','Canceled','Cancelled') OR book_item_id IS NOT NULL),
    CONSTRAINT CK_BorrowDetails_return CHECK ((status = 'Returned' AND return_date IS NOT NULL) OR status <> 'Returned'),
    CONSTRAINT CK_BorrowDetails_condition CHECK (condition_code IS NULL OR condition_code IN ('GOOD','MINOR_DAMAGE','DAMAGED','LOST')),
    CONSTRAINT CK_BorrowDetails_rejection_code CHECK (rejection_code IS NULL OR rejection_code IN ('NO_COPY','LIMIT_EXCEEDED','OUTSTANDING_OBLIGATION','INVALID_INFORMATION','RESERVED_BY_OTHER','OVERDUE','RENEWAL_LIMIT_REACHED','ACCOUNT_RESTRICTED','BOOK_RECALL','APPROVAL_EXPIRED','RETURNED_BEFORE_APPROVAL','OTHER')),
    CONSTRAINT CK_BorrowDetails_rejection_reason CHECK (rejection_reason IS NULL OR LEN(LTRIM(RTRIM(rejection_reason))) > 0)
);
CREATE INDEX IX_BorrowDetails_borrow ON dbo.BorrowDetails(borrow_id);
CREATE INDEX IX_BorrowDetails_status_due ON dbo.BorrowDetails(status, due_date) INCLUDE(borrow_id, book_id, book_item_id);
CREATE INDEX IX_BorrowDetails_item_status ON dbo.BorrowDetails(book_item_id, status) WHERE book_item_id IS NOT NULL;
CREATE UNIQUE INDEX UX_BorrowDetails_open_copy ON dbo.BorrowDetails(book_item_id)
WHERE book_item_id IS NOT NULL AND status IN ('Waiting_Pickup','Payment_Pending','Borrowed','Overdue','Return_Pending','Renew_Pending');
CREATE INDEX IX_BorrowDetails_book ON dbo.BorrowDetails(book_id);

CREATE TABLE dbo.Reservations (
    reservation_id   INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Reservations PRIMARY KEY,
    member_id         INT NOT NULL,
    book_id           INT NOT NULL,
    reservation_date  DATETIME2(6) NOT NULL CONSTRAINT DF_Reservations_date DEFAULT (SYSUTCDATETIME()),
    status             VARCHAR(50) NOT NULL CONSTRAINT DF_Reservations_status DEFAULT ('Pending'),
    rejection_code     VARCHAR(50) NULL,
    rejection_reason   NVARCHAR(500) NULL,
    CONSTRAINT FK_Reservations_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id),
    CONSTRAINT FK_Reservations_book FOREIGN KEY (book_id) REFERENCES dbo.Books(book_id),
    CONSTRAINT CK_Reservations_status CHECK (status IN ('Pending','Ready','Active','Deposit_Paid','Refund_Pending','Refunded','Completed','Rejected','Canceled','Cancelled')),
    CONSTRAINT CK_Reservations_rejection_code CHECK (rejection_code IS NULL OR rejection_code IN ('COPY_AVAILABLE','UNFULFILLABLE','DUPLICATE_REQUEST','ACCOUNT_RESTRICTED','INVALID_DEPOSIT','OTHER')),
    CONSTRAINT CK_Reservations_rejection_reason CHECK (rejection_reason IS NULL OR LEN(LTRIM(RTRIM(rejection_reason))) > 0)
);
CREATE INDEX IX_Reservations_member_date ON dbo.Reservations(member_id, reservation_date DESC) INCLUDE(status, book_id);
CREATE INDEX IX_Reservations_status_date ON dbo.Reservations(status, reservation_date) INCLUDE(member_id, book_id);
CREATE INDEX IX_Reservations_book_status ON dbo.Reservations(book_id, status) INCLUDE(member_id);
CREATE UNIQUE INDEX UX_Reservations_active_member_book ON dbo.Reservations(member_id, book_id)
WHERE status IN ('Pending','Ready','Active','Deposit_Paid','Refund_Pending');

CREATE TABLE dbo.Favorites (
    member_id INT NOT NULL,
    book_id   INT NOT NULL,
    CONSTRAINT PK_Favorites PRIMARY KEY (member_id, book_id),
    CONSTRAINT FK_Favorites_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id) ON DELETE CASCADE,
    CONSTRAINT FK_Favorites_book FOREIGN KEY (book_id) REFERENCES dbo.Books(book_id) ON DELETE CASCADE
);
CREATE INDEX IX_Favorites_book ON dbo.Favorites(book_id);

CREATE TABLE dbo.Feedbacks (
    feedback_id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Feedbacks PRIMARY KEY,
    member_id          INT NOT NULL,
    book_id            INT NOT NULL,
    rating             INT NOT NULL,
    comment            NVARCHAR(MAX) NULL,
    created_date       DATETIME2(6) NOT NULL CONSTRAINT DF_Feedbacks_date DEFAULT (SYSUTCDATETIME()),
    status             VARCHAR(255) NOT NULL CONSTRAINT DF_Feedbacks_status DEFAULT ('PENDING'),
    librarian_response NVARCHAR(MAX) NULL,
    response_date      DATETIME2(6) NULL,
    CONSTRAINT FK_Feedbacks_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id),
    CONSTRAINT FK_Feedbacks_book FOREIGN KEY (book_id) REFERENCES dbo.Books(book_id),
    CONSTRAINT CK_Feedbacks_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT CK_Feedbacks_status CHECK (status IN ('PENDING','APPROVED','REJECTED','DELETED_BY_MEMBER')),
    CONSTRAINT CK_Feedbacks_response CHECK ((librarian_response IS NULL AND response_date IS NULL) OR (librarian_response IS NOT NULL AND response_date IS NOT NULL))
);
CREATE INDEX IX_Feedbacks_book_status_date ON dbo.Feedbacks(book_id, status, created_date DESC) INCLUDE(rating, member_id);
CREATE INDEX IX_Feedbacks_member_status_date ON dbo.Feedbacks(member_id, status, created_date DESC) INCLUDE(book_id, rating);

CREATE TABLE dbo.BookAcquisitionRequests (
    request_id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookAcquisitionRequests PRIMARY KEY,
    member_id        INT NOT NULL,
    title            NVARCHAR(255) NOT NULL,
    created_date     DATETIME2(6) NOT NULL CONSTRAINT DF_BookAcquisitionRequests_date DEFAULT (SYSUTCDATETIME()),
    author           NVARCHAR(255) NULL,
    isbn             VARCHAR(20) NULL,
    publisher        NVARCHAR(255) NULL,
    publication_year INT NULL,
    request_reason   NVARCHAR(1000) NULL,
    reference_url    VARCHAR(500) NULL,
    status           NVARCHAR(20) NOT NULL CONSTRAINT DF_BookAcquisitionRequests_status DEFAULT (N'PENDING'),
    decision_note    NVARCHAR(500) NULL,
    processed_date   DATETIME2(6) NULL,
    CONSTRAINT FK_BookAcquisitionRequests_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id),
    CONSTRAINT CK_BookAcquisitionRequests_status CHECK (status IN (N'PENDING',N'APPROVED',N'REJECTED')),
    CONSTRAINT CK_BookAcquisitionRequests_year CHECK (publication_year IS NULL OR publication_year BETWEEN 1000 AND 2200),
    CONSTRAINT CK_BookAcquisitionRequests_decision CHECK ((status = N'PENDING' AND processed_date IS NULL) OR status <> N'PENDING')
);
CREATE INDEX IX_Acquisition_member_date ON dbo.BookAcquisitionRequests(member_id, created_date DESC) INCLUDE(status, title);
CREATE INDEX IX_Acquisition_status_date ON dbo.BookAcquisitionRequests(status, created_date);

CREATE TABLE dbo.SystemSettings (
    setting_id    INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SystemSettings PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL CONSTRAINT UQ_SystemSettings_key UNIQUE,
    setting_value NVARCHAR(MAX) NULL,
    description   NVARCHAR(255) NULL,
    CONSTRAINT CK_SystemSettings_key CHECK (LEN(LTRIM(RTRIM(setting_key))) > 0)
);

CREATE TABLE dbo.Transactions (
    transaction_id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Transactions PRIMARY KEY,
    wallet_id             INT NOT NULL,
    borrow_id             INT NULL,
    transaction_type      VARCHAR(50) NOT NULL,
    amount                DECIMAL(18,2) NOT NULL,
    transaction_date      DATETIME2(6) NOT NULL CONSTRAINT DF_Transactions_date DEFAULT (SYSUTCDATETIME()),
    status                VARCHAR(50) NOT NULL CONSTRAINT DF_Transactions_status DEFAULT ('Completed'),
    borrow_detail_id      INT NULL,
    renewal_days          INT NULL,
    reference_code        VARCHAR(64) NULL,
    performed_by_staff_id INT NULL,
    channel               VARCHAR(20) NULL,
    balance_before        DECIMAL(18,2) NULL,
    balance_after         DECIMAL(18,2) NULL,
    CONSTRAINT FK_Transactions_wallet FOREIGN KEY (wallet_id) REFERENCES dbo.Wallets(wallet_id),
    CONSTRAINT FK_Transactions_borrow FOREIGN KEY (borrow_id) REFERENCES dbo.Borrows(borrow_id),
    CONSTRAINT FK_Transactions_detail FOREIGN KEY (borrow_detail_id) REFERENCES dbo.BorrowDetails(borrow_detail_id),
    CONSTRAINT FK_Transactions_staff FOREIGN KEY (performed_by_staff_id) REFERENCES dbo.Staff(staff_id),
    CONSTRAINT CK_Transactions_type CHECK (transaction_type IN ('TOP_UP','BORROW_FEE','DEPOSIT','FINE','DAMAGE_FEE','RENEWAL_FEE','REFUND')),
    CONSTRAINT CK_Transactions_amount CHECK (amount <> 0),
    CONSTRAINT CK_Transactions_status CHECK (status IN ('Pending','Completed','Failed','Cancelled','Expired','Refunded')),
    CONSTRAINT CK_Transactions_renewal_days CHECK (renewal_days IS NULL OR renewal_days BETWEEN 1 AND 365),
    CONSTRAINT CK_Transactions_channel CHECK (channel IS NULL OR channel IN ('WALLET','CASH','PAYOS','SYSTEM')),
    CONSTRAINT CK_Transactions_balances CHECK ((balance_before IS NULL AND balance_after IS NULL) OR (balance_before >= 0 AND balance_after >= 0))
);
CREATE UNIQUE INDEX UX_Transactions_reference ON dbo.Transactions(reference_code) WHERE reference_code IS NOT NULL;
CREATE UNIQUE INDEX UX_Transactions_pending_renewal ON dbo.Transactions(borrow_detail_id)
WHERE borrow_detail_id IS NOT NULL AND transaction_type = 'RENEWAL_FEE' AND status = 'Pending';
CREATE INDEX IX_Transactions_wallet_date ON dbo.Transactions(wallet_id, transaction_date DESC) INCLUDE(transaction_type,status,amount,borrow_id);
CREATE INDEX IX_Transactions_borrow_date ON dbo.Transactions(borrow_id, transaction_date DESC) WHERE borrow_id IS NOT NULL;
CREATE INDEX IX_Transactions_detail_type_status ON dbo.Transactions(borrow_detail_id, transaction_type, status) WHERE borrow_detail_id IS NOT NULL;
CREATE INDEX IX_Transactions_type_date ON dbo.Transactions(transaction_type, transaction_date DESC) INCLUDE(status,amount);

CREATE TABLE dbo.Notifications (
    notification_id     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Notifications PRIMARY KEY,
    staff_id             INT NULL,
    title                NVARCHAR(255) NOT NULL,
    content              NVARCHAR(MAX) NOT NULL,
    created_date         DATETIME2(6) NOT NULL CONSTRAINT DF_Notifications_date DEFAULT (SYSUTCDATETIME()),
    status               VARCHAR(50) NOT NULL CONSTRAINT DF_Notifications_status DEFAULT ('Active'),
    notification_type    NVARCHAR(30) NOT NULL CONSTRAINT DF_Notifications_type DEFAULT (N'GENERAL'),
    notification_source  NVARCHAR(20) NOT NULL CONSTRAINT DF_Notifications_source DEFAULT (N'SYSTEM'),
    event_type            NVARCHAR(50) NOT NULL CONSTRAINT DF_Notifications_event DEFAULT (N'GENERAL'),
    title_key             NVARCHAR(255) NULL,
    content_key           NVARCHAR(255) NULL,
    message_arguments     NVARCHAR(MAX) NULL,
    language_code         NVARCHAR(5) NOT NULL CONSTRAINT DF_Notifications_language DEFAULT (N'en'),
    CONSTRAINT FK_Notifications_staff FOREIGN KEY (staff_id) REFERENCES dbo.Staff(staff_id),
    CONSTRAINT FK_Notifications_language FOREIGN KEY (language_code) REFERENCES dbo.LocalizationLanguages(language_code),
    CONSTRAINT CK_Notifications_status CHECK (status IN ('Active','Inactive')),
    CONSTRAINT CK_Notifications_type CHECK (notification_type IN (N'GENERAL',N'ANNOUNCEMENT',N'MAINTENANCE',N'EVENT',N'REMINDER',N'LOAN',N'RESERVATION',N'FINANCE',N'REVIEW',N'ACQUISITION')),
    CONSTRAINT CK_Notifications_source CHECK (notification_source IN (N'SYSTEM',N'LIBRARIAN'))
);
CREATE INDEX IX_Notifications_date ON dbo.Notifications(created_date DESC) INCLUDE(status,notification_type,event_type);

CREATE TABLE dbo.MemberNotifications (
    member_id      INT NOT NULL,
    notification_id INT NOT NULL,
    is_read        BIT NOT NULL CONSTRAINT DF_MemberNotifications_read DEFAULT (0),
    read_date      DATETIME2(6) NULL,
    CONSTRAINT PK_MemberNotifications PRIMARY KEY (member_id, notification_id),
    CONSTRAINT FK_MemberNotifications_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id) ON DELETE CASCADE,
    CONSTRAINT FK_MemberNotifications_notification FOREIGN KEY (notification_id) REFERENCES dbo.Notifications(notification_id) ON DELETE CASCADE,
    CONSTRAINT CK_MemberNotifications_read CHECK ((is_read = 0 AND read_date IS NULL) OR is_read = 1)
);
CREATE INDEX IX_MemberNotifications_unread ON dbo.MemberNotifications(member_id, is_read, notification_id DESC);
CREATE INDEX IX_MemberNotifications_notification ON dbo.MemberNotifications(notification_id);

/* Translation tables keep canonical English data stable while allowing more locales later. */
CREATE TABLE dbo.CategoryTranslations (
    category_id   INT NOT NULL,
    language_code NVARCHAR(5) NOT NULL,
    category_name NVARCHAR(255) NOT NULL,
    CONSTRAINT PK_CategoryTranslations PRIMARY KEY(category_id,language_code),
    CONSTRAINT FK_CategoryTranslations_category FOREIGN KEY(category_id) REFERENCES dbo.Categories(category_id) ON DELETE CASCADE,
    CONSTRAINT FK_CategoryTranslations_language FOREIGN KEY(language_code) REFERENCES dbo.LocalizationLanguages(language_code)
);

CREATE TABLE dbo.GenreTranslations (
    genre_id      INT NOT NULL,
    language_code NVARCHAR(5) NOT NULL,
    genre_name    NVARCHAR(255) NOT NULL,
    CONSTRAINT PK_GenreTranslations PRIMARY KEY(genre_id,language_code),
    CONSTRAINT FK_GenreTranslations_genre FOREIGN KEY(genre_id) REFERENCES dbo.Genres(genre_id) ON DELETE CASCADE,
    CONSTRAINT FK_GenreTranslations_language FOREIGN KEY(language_code) REFERENCES dbo.LocalizationLanguages(language_code)
);

CREATE TABLE dbo.BookTranslations (
    book_id       INT NOT NULL,
    language_code NVARCHAR(5) NOT NULL,
    title          NVARCHAR(255) NOT NULL,
    description    NVARCHAR(MAX) NULL,
    CONSTRAINT PK_BookTranslations PRIMARY KEY(book_id,language_code),
    CONSTRAINT FK_BookTranslations_book FOREIGN KEY(book_id) REFERENCES dbo.Books(book_id) ON DELETE CASCADE,
    CONSTRAINT FK_BookTranslations_language FOREIGN KEY(language_code) REFERENCES dbo.LocalizationLanguages(language_code)
);
CREATE INDEX IX_BookTranslations_language_title ON dbo.BookTranslations(language_code,title) INCLUDE(book_id);

CREATE TABLE dbo.ShelfTranslations (
    shelf_id      INT NOT NULL,
    language_code NVARCHAR(5) NOT NULL,
    shelf_name     NVARCHAR(100) NOT NULL,
    location       NVARCHAR(255) NULL,
    CONSTRAINT PK_ShelfTranslations PRIMARY KEY(shelf_id,language_code),
    CONSTRAINT FK_ShelfTranslations_shelf FOREIGN KEY(shelf_id) REFERENCES dbo.Shelves(shelf_id) ON DELETE CASCADE,
    CONSTRAINT FK_ShelfTranslations_language FOREIGN KEY(language_code) REFERENCES dbo.LocalizationLanguages(language_code)
);

CREATE TABLE dbo.MembershipTierTranslations (
    tier_id       INT NOT NULL,
    language_code NVARCHAR(5) NOT NULL,
    tier_name      NVARCHAR(100) NOT NULL,
    benefits       NVARCHAR(MAX) NULL,
    CONSTRAINT PK_MembershipTierTranslations PRIMARY KEY(tier_id,language_code),
    CONSTRAINT FK_MembershipTierTranslations_tier FOREIGN KEY(tier_id) REFERENCES dbo.MembershipTiers(tier_id) ON DELETE CASCADE,
    CONSTRAINT FK_MembershipTierTranslations_language FOREIGN KEY(language_code) REFERENCES dbo.LocalizationLanguages(language_code)
);

CREATE TABLE dbo.NotificationTranslations (
    notification_id INT NOT NULL,
    language_code    NVARCHAR(5) NOT NULL,
    title             NVARCHAR(255) NOT NULL,
    content           NVARCHAR(MAX) NOT NULL,
    CONSTRAINT PK_NotificationTranslations PRIMARY KEY(notification_id,language_code),
    CONSTRAINT FK_NotificationTranslations_notification FOREIGN KEY(notification_id) REFERENCES dbo.Notifications(notification_id) ON DELETE CASCADE,
    CONSTRAINT FK_NotificationTranslations_language FOREIGN KEY(language_code) REFERENCES dbo.LocalizationLanguages(language_code)
);

CREATE TABLE dbo.SystemSettingTranslations (
    setting_id    INT NOT NULL,
    language_code NVARCHAR(5) NOT NULL,
    description   NVARCHAR(255) NOT NULL,
    CONSTRAINT PK_SystemSettingTranslations PRIMARY KEY(setting_id,language_code),
    CONSTRAINT FK_SystemSettingTranslations_setting FOREIGN KEY(setting_id) REFERENCES dbo.SystemSettings(setting_id) ON DELETE CASCADE,
    CONSTRAINT FK_SystemSettingTranslations_language FOREIGN KEY(language_code) REFERENCES dbo.LocalizationLanguages(language_code)
);

CREATE TABLE dbo.PasswordResetTokens (
    reset_token_id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PasswordResetTokens PRIMARY KEY,
    token          VARCHAR(36) NOT NULL,
    user_id        INT NOT NULL,
    expiry_date    DATETIME2(7) NOT NULL,
    CONSTRAINT UQ_PasswordResetTokens_token UNIQUE (token),
    CONSTRAINT UQ_PasswordResetTokens_user UNIQUE (user_id),
    CONSTRAINT FK_PasswordResetTokens_user FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id) ON DELETE CASCADE
);
CREATE INDEX IX_PasswordResetTokens_expiry ON dbo.PasswordResetTokens(expiry_date);

CREATE TABLE dbo.SystemLogs (
    log_id       INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SystemLogs PRIMARY KEY,
    action_type  VARCHAR(100) NOT NULL,
    ip_address   VARCHAR(50) NULL,
    user_agent   NVARCHAR(MAX) NULL,
    description  NVARCHAR(MAX) NULL,
    created_at   DATETIME2(6) NOT NULL CONSTRAINT DF_SystemLogs_date DEFAULT (SYSUTCDATETIME()),
    user_id      INT NULL,
    CONSTRAINT FK_SystemLogs_user FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id)
);
CREATE INDEX IX_SystemLogs_date ON dbo.SystemLogs(created_at DESC) INCLUDE(action_type,user_id);
CREATE INDEX IX_SystemLogs_user_date ON dbo.SystemLogs(user_id, created_at DESC) WHERE user_id IS NOT NULL;

CREATE TABLE dbo.PayOSPayments (
    payment_id      BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PayOSPayments PRIMARY KEY,
    member_id       INT NOT NULL,
    transaction_id  INT NULL,
    purpose          VARCHAR(30) NOT NULL,
    reference_id     INT NULL,
    amount           DECIMAL(18,2) NOT NULL,
    order_code       BIGINT NOT NULL CONSTRAINT UQ_PayOSPayments_order UNIQUE,
    payment_link_id  VARCHAR(100) NULL,
    checkout_url     VARCHAR(1000) NULL,
    qr_code          NVARCHAR(MAX) NULL,
    status           VARCHAR(30) NOT NULL,
    bank_reference   VARCHAR(100) NULL,
    created_at       DATETIME2(7) NOT NULL,
    paid_at          DATETIME2(7) NULL,
    CONSTRAINT FK_PayOSPayments_member FOREIGN KEY (member_id) REFERENCES dbo.Members(member_id),
    CONSTRAINT FK_PayOSPayments_transaction FOREIGN KEY (transaction_id) REFERENCES dbo.Transactions(transaction_id),
    CONSTRAINT CK_PayOSPayments_purpose CHECK (purpose IN ('TOP_UP','BORROW_FEE','FINE','FINE_BATCH','DEPOSIT')),
    CONSTRAINT CK_PayOSPayments_amount CHECK (amount > 0),
    CONSTRAINT CK_PayOSPayments_status CHECK (status IN ('PENDING','PAID','CANCELLED','EXPIRED','FAILED')),
    CONSTRAINT CK_PayOSPayments_paid CHECK ((status = 'PAID' AND paid_at IS NOT NULL) OR status <> 'PAID'),
    CONSTRAINT CK_PayOSPayments_dates CHECK (paid_at IS NULL OR paid_at >= created_at)
);
CREATE UNIQUE INDEX UX_PayOSPayments_transaction ON dbo.PayOSPayments(transaction_id) WHERE transaction_id IS NOT NULL;
CREATE INDEX IX_PayOSPayments_member_created ON dbo.PayOSPayments(member_id, created_at DESC) INCLUDE(status,purpose,amount);
CREATE INDEX IX_PayOSPayments_reference ON dbo.PayOSPayments(purpose, reference_id, status) INCLUDE(member_id,created_at);

CREATE TABLE dbo.PayOSPaymentFineItems (
    item_id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PayOSPaymentFineItems PRIMARY KEY,
    payment_id          BIGINT NOT NULL,
    fine_transaction_id INT NOT NULL,
    amount_snapshot     DECIMAL(18,2) NOT NULL,
    CONSTRAINT UQ_PayOSPaymentFineItems UNIQUE (payment_id,fine_transaction_id),
    CONSTRAINT FK_PayOSPaymentFineItems_payment FOREIGN KEY (payment_id) REFERENCES dbo.PayOSPayments(payment_id) ON DELETE CASCADE,
    CONSTRAINT FK_PayOSPaymentFineItems_transaction FOREIGN KEY (fine_transaction_id) REFERENCES dbo.Transactions(transaction_id),
    CONSTRAINT CK_PayOSPaymentFineItems_amount CHECK (amount_snapshot > 0)
);
CREATE INDEX IX_PayOSPaymentFineItems_transaction ON dbo.PayOSPaymentFineItems(fine_transaction_id);

CREATE TABLE dbo.PayOSPaymentAuditLogs (
    audit_id      BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PayOSPaymentAuditLogs PRIMARY KEY,
    created_at    DATETIME2(6) NOT NULL CONSTRAINT DF_PayOSPaymentAuditLogs_date DEFAULT (SYSUTCDATETIME()),
    event_type    VARCHAR(50) NOT NULL,
    message       NVARCHAR(MAX) NULL,
    new_status    VARCHAR(30) NULL,
    old_status    VARCHAR(30) NULL,
    source        VARCHAR(30) NOT NULL,
    successful    BIT NOT NULL,
    actor_user_id INT NULL,
    payment_id    BIGINT NOT NULL,
    CONSTRAINT FK_PayOSPaymentAuditLogs_actor FOREIGN KEY (actor_user_id) REFERENCES dbo.Users(user_id),
    CONSTRAINT FK_PayOSPaymentAuditLogs_payment FOREIGN KEY (payment_id) REFERENCES dbo.PayOSPayments(payment_id) ON DELETE CASCADE
);
CREATE INDEX IX_PayOSPaymentAuditLogs_payment_date ON dbo.PayOSPaymentAuditLogs(payment_id, created_at DESC);

CREATE TABLE dbo.PayOSReconciliationIssues (
    issue_id        BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PayOSReconciliationIssues PRIMARY KEY,
    attempt_count   INT NOT NULL CONSTRAINT DF_PayOSReconciliationIssues_attempt DEFAULT (0),
    error_message   NVARCHAR(MAX) NULL,
    first_seen_at   DATETIME2(6) NOT NULL,
    last_attempt_at DATETIME2(6) NOT NULL,
    resolved_at     DATETIME2(6) NULL,
    status          VARCHAR(20) NOT NULL,
    payment_id      BIGINT NOT NULL,
    CONSTRAINT FK_PayOSReconciliationIssues_payment FOREIGN KEY (payment_id) REFERENCES dbo.PayOSPayments(payment_id) ON DELETE CASCADE,
    CONSTRAINT CK_PayOSReconciliationIssues_attempt CHECK (attempt_count >= 0),
    CONSTRAINT CK_PayOSReconciliationIssues_status CHECK (status IN ('OPEN','RESOLVED')),
    CONSTRAINT CK_PayOSReconciliationIssues_dates CHECK (last_attempt_at >= first_seen_at AND (resolved_at IS NULL OR resolved_at >= first_seen_at))
);
CREATE INDEX IX_PayOSReconciliationIssues_status_date ON dbo.PayOSReconciliationIssues(status,last_attempt_at DESC) INCLUDE(payment_id,attempt_count);
GO

PRINT 'Flyway baseline schema created successfully.';
GO
