-- =====================================================
-- Library Management System Database
-- Version FIXED: 4 Critical Bugs Fixed + BR-1 to BR-41
-- =====================================================

USE master;
GO

IF EXISTS (SELECT * FROM sys.databases WHERE name = 'LibraryManagementSystem')
BEGIN
    ALTER DATABASE LibraryManagementSystem SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE LibraryManagementSystem;
END
GO

CREATE DATABASE LibraryManagementSystem;
GO
USE LibraryManagementSystem;
GO

-- =====================================================
-- ROLES
-- =====================================================
CREATE TABLE Roles (
    role_id   INT IDENTITY(1,1) PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);
GO

-- =====================================================
-- MEMBERSHIP TIERS
-- BR-34: 2 hang: Standard Member va Loyal Member
-- BR-32: Loyal = 50% phi muon (borrow_fee_rate = 0.5)
-- =====================================================
CREATE TABLE MembershipTiers (
    tier_id          INT IDENTITY(1,1) PRIMARY KEY,
    tier_name        NVARCHAR(50)  NOT NULL UNIQUE,
    max_borrow_books INT           NOT NULL DEFAULT 5,
    borrow_fee_rate  DECIMAL(5,2)  NOT NULL DEFAULT 1.0,
    -- BR-32: Loyal Member phi muon = 50% tieu chuan (rate = 0.5)
    description      NVARCHAR(500)
);
GO

-- =====================================================
-- USERS
-- BR-11: Khoa TK sau 3 lan vi pham qua han (overdue_violations)
-- BR-24: Chi Admin tao/chinh sua/tat tai khoan Thu Thu
-- =====================================================
CREATE TABLE Users (
    user_id            INT IDENTITY(1,1) PRIMARY KEY,
    username           VARCHAR(50)   NOT NULL UNIQUE,
    password_hash      VARCHAR(255)  NOT NULL,
    email              VARCHAR(100)  NOT NULL UNIQUE,
    full_name          NVARCHAR(100) NOT NULL,
    phone              VARCHAR(20),
    role_id            INT           NOT NULL,
    status             VARCHAR(30)   DEFAULT 'Active',
    overdue_violations INT           DEFAULT 0,
    -- BR-11: Dem so lan vi pham, khoa sau khi >= Max_Overdue_Violations
    last_login         DATETIME      NULL,
    created_at         DATETIME      DEFAULT GETDATE(),
    CONSTRAINT FK_Users_Roles    FOREIGN KEY(role_id) REFERENCES Roles(role_id),
    CONSTRAINT CHK_User_Status   CHECK(status IN ('Active','Disabled','Blocked'))
);
GO

-- =====================================================
-- MEMBER DETAILS
-- BR-31: Nang hang khi total_spending >= 1,000,000 VND
-- BR-35: Ha hang Loyal -> Standard neu khong muon sach 6 thang
-- BR-37: Chi nap tien qua Thu Thu tai quay (khong online)
-- =====================================================
CREATE TABLE MemberDetails (
    member_id        INT           PRIMARY KEY,
    tier_id          INT           NOT NULL,
    balance          DECIMAL(12,2) DEFAULT 0,
    total_spending   DECIMAL(12,2) DEFAULT 0,
    -- BR-31: Tong chi tieu (chi tinh giao dich, khong tinh so du)
    last_borrow_date DATE          NULL,
    -- BR-35: Ngay muon sach gan nhat (kiem tra inactive 6 thang)
    join_date        DATE          DEFAULT GETDATE(),
    FOREIGN KEY(member_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY(tier_id)   REFERENCES MembershipTiers(tier_id)
);
GO

-- =====================================================
-- CATEGORIES
-- BR-21: Sach phai phan loai theo the loai
-- =====================================================
CREATE TABLE Categories (
    category_id   INT IDENTITY(1,1) PRIMARY KEY,
    category_name NVARCHAR(100) NOT NULL UNIQUE,
    description   NVARCHAR(255)
);
GO

-- =====================================================
-- STORAGE LOCATIONS (UC-11)
-- =====================================================
CREATE TABLE BookStorages (
    storage_id    INT IDENTITY(1,1) PRIMARY KEY,
    location_name NVARCHAR(100) NOT NULL UNIQUE,
    description   NVARCHAR(255)
);
GO

-- =====================================================
-- BOOKS
-- FIX LOI 1: + cover_image_url (Cloudinary URL)
-- FIX LOI 4: + publisher, publication_year, page_count
-- BR-21, BR-29: + language (loc/tim kiem theo ngon ngu)
-- =====================================================
CREATE TABLE Books (
    book_id          INT IDENTITY(1,1) PRIMARY KEY,
    title            NVARCHAR(255) NOT NULL,
    author           NVARCHAR(255) NOT NULL,
    isbn             VARCHAR(20)   UNIQUE NOT NULL,
    publisher        NVARCHAR(255) NULL,         -- FIX LOI 4
    publication_year INT           NULL,          -- FIX LOI 4
    page_count       INT           NULL,          -- FIX LOI 4
    language         NVARCHAR(50)  DEFAULT N'Tieng Viet', -- BR-21, BR-29
    summary          NVARCHAR(MAX),
    cover_image_url  NVARCHAR(500) NULL,          -- FIX LOI 1: Cloudinary
    created_at       DATETIME      DEFAULT GETDATE()
);
GO

-- =====================================================
-- BOOK CATEGORIES (Many-to-Many)
-- =====================================================
CREATE TABLE BookCategories (
    book_id     INT NOT NULL,
    category_id INT NOT NULL,
    PRIMARY KEY(book_id, category_id),
    FOREIGN KEY(book_id)     REFERENCES Books(book_id)      ON DELETE CASCADE,
    FOREIGN KEY(category_id) REFERENCES Categories(category_id) ON DELETE CASCADE
);
GO

-- =====================================================
-- BOOK COPIES
-- BR-5:  Can co ban sao 'Available' moi cho muon
-- BR-22: Thanh ly sach khi condition_pct < 40%
-- =====================================================
CREATE TABLE BookCopies (
    copy_id       INT IDENTITY(1,1) PRIMARY KEY,
    book_id       INT         NOT NULL,
    barcode       VARCHAR(50) UNIQUE NOT NULL,
    storage_id    INT,
    status        VARCHAR(30) DEFAULT 'Available',
    condition_pct INT         DEFAULT 100,
    -- BR-22: Phan tram tinh trang, thanh ly khi < 40%
    FOREIGN KEY(book_id)    REFERENCES Books(book_id) ON DELETE CASCADE,
    FOREIGN KEY(storage_id) REFERENCES BookStorages(storage_id),
    CONSTRAINT CHK_Copy_Status
        CHECK(status IN ('Available','Borrowed','Reserved','Lost','Damaged'))
);
GO

-- =====================================================
-- INVENTORY AUDITS
-- FIX VAN DE 7: + total_copies_checked, discrepancies_found, status
-- BR-22: Ghi nhan bien ban kiem ke
-- =====================================================
CREATE TABLE InventoryAudits (
    audit_id             INT IDENTITY(1,1) PRIMARY KEY,
    librarian_id         INT,
    audit_date           DATETIME    DEFAULT GETDATE(),
    total_copies_checked INT         DEFAULT 0,
    discrepancies_found  INT         DEFAULT 0,
    status               VARCHAR(20) DEFAULT 'Completed',
    notes                NVARCHAR(MAX),
    FOREIGN KEY(librarian_id) REFERENCES Users(user_id)
);
GO

-- =====================================================
-- FAVORITES (UC-7.1, UC-4.4)
-- =====================================================
CREATE TABLE Favorites (
    member_id INT      NOT NULL,
    book_id   INT      NOT NULL,
    added_at  DATETIME DEFAULT GETDATE(),
    PRIMARY KEY(member_id, book_id),
    FOREIGN KEY(member_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY(book_id)   REFERENCES Books(book_id) ON DELETE CASCADE
);
GO

-- =====================================================
-- NOTIFICATIONS
-- BR-15: Thong bao khi sach dat truoc san sang
-- BR-27: Thong bao qua han, dat truoc, sach moi, sap het han
-- =====================================================
CREATE TABLE Notifications (
    notification_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id         INT           NOT NULL,
    title           NVARCHAR(255) NOT NULL,
    message         NVARCHAR(MAX) NOT NULL,
    type            VARCHAR(50)   DEFAULT 'General',
    -- Overdue / ReservationReady / TopUp / NewBook / System
    is_read         BIT           DEFAULT 0,
    created_at      DATETIME      DEFAULT GETDATE(),
    FOREIGN KEY(user_id) REFERENCES Users(user_id)
);
GO

-- =====================================================
-- RESERVATIONS
-- BR-13: Dat truoc sach chua co san
-- BR-14: FIFO - xu ly theo thu tu danh sach cho
-- FIX LOI 3: + expiry_date (reserve_date + Reservation_Days)
-- BR-40: + approved_by (sach quy hiem / tham khao can thu thu duyet)
-- BR-41: + deposit_amount (50% gia tri sach)
-- Status: Pending -> Approved -> Ready -> Completed / Canceled / Expired
-- =====================================================
CREATE TABLE Reservations (
    reservation_id INT IDENTITY(1,1) PRIMARY KEY,
    member_id      INT           NOT NULL,
    book_id        INT           NOT NULL,
    reserve_date   DATETIME      DEFAULT GETDATE(),
    expiry_date    DATETIME      NULL,        -- FIX LOI 3
    deposit_amount DECIMAL(12,2) DEFAULT 0,  -- BR-41
    status         VARCHAR(25)   DEFAULT 'Pending',
    approved_by    INT           NULL,        -- BR-40
    FOREIGN KEY(member_id)   REFERENCES Users(user_id),
    FOREIGN KEY(book_id)     REFERENCES Books(book_id),
    FOREIGN KEY(approved_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Reservation_Status
        CHECK(status IN ('Pending','Approved','Ready','Completed','Canceled','Expired'))
    -- Ready = sach ve, cho member den lay trong expiry_date
    -- Expired = qua expiry_date khong den, tu dong huy
);
GO

-- =====================================================
-- BORROW RECORDS
-- BR-3:  Chi member 'Active' moi muon duoc
-- BR-4:  Phai thanh toan phi muon truoc giao dich
-- BR-5:  Can co ban sao kha dung
-- FIX VAN DE 6: + renewal_count (kiem tra voi Max_Renewals)
-- =====================================================
CREATE TABLE Borrows (
    borrow_id     INT IDENTITY(1,1) PRIMARY KEY,
    member_id     INT           NOT NULL,
    copy_id       INT           NOT NULL,
    borrow_date   DATETIME      DEFAULT GETDATE(),
    due_date      DATETIME      NOT NULL,
    return_date   DATETIME      NULL,
    renewal_count INT           DEFAULT 0,   -- FIX VAN DE 6
    borrow_fee    DECIMAL(12,2) DEFAULT 0,   -- BR-4, BR-32
    status        VARCHAR(30)   DEFAULT 'Borrowed',
    processed_by  INT           NULL,        -- Thu Thu xu ly phieu muon
    FOREIGN KEY(member_id)    REFERENCES Users(user_id),
    FOREIGN KEY(copy_id)      REFERENCES BookCopies(copy_id),
    FOREIGN KEY(processed_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Borrow_Status
        CHECK(status IN ('Borrowed','Returned','Overdue','Lost'))
);
GO

-- =====================================================
-- BOOK REVIEWS
-- BR-16: Chi review sach sau khi da muon cuon sach do
-- BR-17: Chi cham diem 1 lan (UNIQUE member_id + book_id)
-- UC-15.3: Thu Thu kiem duyet review
-- =====================================================
CREATE TABLE BookReviews (
    review_id    INT IDENTITY(1,1) PRIMARY KEY,
    member_id    INT           NOT NULL,
    book_id      INT           NOT NULL,
    borrow_id    INT           NOT NULL,   -- BR-16: phai tham chieu phieu muon
    moderated_by INT           NULL,
    moderated_at DATETIME      NULL,
    rating       INT           CHECK(rating BETWEEN 1 AND 5),
    review_text  NVARCHAR(MAX),
    status       VARCHAR(20)   DEFAULT 'Pending',
    -- Pending / Approved / Rejected
    created_at   DATETIME      DEFAULT GETDATE(),
    FOREIGN KEY(member_id)    REFERENCES Users(user_id),
    FOREIGN KEY(book_id)      REFERENCES Books(book_id),
    FOREIGN KEY(borrow_id)    REFERENCES Borrows(borrow_id),
    FOREIGN KEY(moderated_by) REFERENCES Users(user_id),
    CONSTRAINT UQ_Review_Member_Book UNIQUE(member_id, book_id) -- BR-17
);
GO

-- =====================================================
-- LOAN RENEWALS
-- BR-6:  Chi gia han khi khong co Reservation dang Pending tren sach do
-- FIX LOI 2: + new_due_date (ngay tra moi sau khi gia han duoc duyet)
-- =====================================================
CREATE TABLE LoanRenewals (
    renewal_id    INT IDENTITY(1,1) PRIMARY KEY,
    borrow_id     INT           NOT NULL,
    request_date  DATETIME      DEFAULT GETDATE(),
    new_due_date  DATETIME      NULL,         -- FIX LOI 2
    approved_by   INT           NULL,
    approved_at   DATETIME      NULL,
    status        VARCHAR(20)   DEFAULT 'Pending',
    reject_reason NVARCHAR(255) NULL,
    -- BR-6: Ly do tu choi (VD: co nguoi dang dat truoc cuon sach nay)
    FOREIGN KEY(borrow_id)   REFERENCES Borrows(borrow_id),
    FOREIGN KEY(approved_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Renewal_Status CHECK(status IN ('Pending','Approved','Rejected'))
);
GO

-- =====================================================
-- VIOLATIONS
-- BR-9:  Boi thuong khi sach hu hong
-- BR-10: Phi boi thuong = 120,000 VND khi hu > 50%
-- BR-11: Vi pham qua han tang Users.overdue_violations
-- =====================================================
CREATE TABLE Violations (
    violation_id   INT IDENTITY(1,1) PRIMARY KEY,
    member_id      INT           NOT NULL,
    borrow_id      INT           NULL,
    violation_type VARCHAR(50)   DEFAULT 'Overdue',
    -- Overdue / Damaged / Lost
    description    NVARCHAR(500) NOT NULL,
    penalty_amount DECIMAL(12,2) DEFAULT 0,
    violation_date DATETIME      DEFAULT GETDATE(),
    recorded_by    INT           NULL,
    FOREIGN KEY(member_id)   REFERENCES Users(user_id),
    FOREIGN KEY(borrow_id)   REFERENCES Borrows(borrow_id),
    FOREIGN KEY(recorded_by) REFERENCES Users(user_id)
);
GO

-- =====================================================
-- FINES
-- BR-8: Phat = overdue_days x Fine_Rate_Per_Day
-- =====================================================
CREATE TABLE Fines (
    fine_id    INT IDENTITY(1,1) PRIMARY KEY,
    borrow_id  INT           NOT NULL,
    member_id  INT           NOT NULL,
    amount     DECIMAL(12,2) NOT NULL,
    fine_type  VARCHAR(30)   DEFAULT 'Overdue',
    -- Overdue / Damaged / Lost
    status     VARCHAR(20)   DEFAULT 'Unpaid',
    created_at DATETIME      DEFAULT GETDATE(),
    paid_at    DATETIME      NULL,
    FOREIGN KEY(borrow_id) REFERENCES Borrows(borrow_id),
    FOREIGN KEY(member_id) REFERENCES Users(user_id),
    CONSTRAINT CHK_Fine_Status CHECK(status IN ('Unpaid','Paid'))
);
GO

-- =====================================================
-- FINANCIAL TRANSACTIONS
-- BR-37: Chi nap tien qua Thu Thu tai quay
-- BR-38: Thu Thu cap nhat thu cong so du
-- BR-39: So du dung de thanh toan dat coc, phat, phi dich vu
-- =====================================================
CREATE TABLE FinancialTransactions (
    transaction_id   INT IDENTITY(1,1) PRIMARY KEY,
    member_id        INT           NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    transaction_type VARCHAR(50)   NOT NULL,
    payment_method   VARCHAR(50),
    -- Cash / QR
    reference_id     VARCHAR(100),
    processed_by     INT           NULL,
    -- Thu Thu thuc hien giao dich
    status           VARCHAR(20)   DEFAULT 'Completed',
    transaction_date DATETIME      DEFAULT GETDATE(),
    note             NVARCHAR(255) NULL,
    FOREIGN KEY(member_id)    REFERENCES Users(user_id),
    FOREIGN KEY(processed_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Transaction_Type
        CHECK(transaction_type IN ('Top_Up','Pay_Fine','Deposit','Borrow_Fee','Refund'))
    -- Refund: Hoan tra dat coc khi Reservation hoan thanh hoac huy
);
GO

-- =====================================================
-- BOOK ACQUISITION REQUESTS
-- BR-18: Thanh vien de xuat mua sach moi
-- UC-15.2: Thu Thu phan hoi de xuat
-- =====================================================
CREATE TABLE BookAcquisitionRequests (
    request_id    INT IDENTITY(1,1) PRIMARY KEY,
    member_id     INT           NOT NULL,
    book_title    NVARCHAR(255) NOT NULL,
    author        NVARCHAR(255),
    isbn          VARCHAR(20)   NULL,
    reason        NVARCHAR(500) NULL,
    status        VARCHAR(20)   DEFAULT 'Pending',
    processed_by  INT           NULL,
    processed_at  DATETIME      NULL,
    reject_reason NVARCHAR(255) NULL,
    created_at    DATETIME      DEFAULT GETDATE(),
    FOREIGN KEY(member_id)    REFERENCES Users(user_id),
    FOREIGN KEY(processed_by) REFERENCES Users(user_id),
    CONSTRAINT CHK_Acquisition_Status
        CHECK(status IN ('Pending','Approved','Rejected'))
);
GO

-- =====================================================
-- REPORTS
-- BR-28: Bao cao hang thang
-- UC-22: Xuat PDF (file_url)
-- =====================================================
CREATE TABLE Reports (
    report_id    INT IDENTITY(1,1) PRIMARY KEY,
    report_name  NVARCHAR(255) NOT NULL,
    report_type  VARCHAR(50),
    -- Monthly / Revenue / Inventory / Members
    generated_by INT,
    generated_at DATETIME      DEFAULT GETDATE(),
    file_url     NVARCHAR(500) NULL,
    -- URL file PDF (luu local hoac Cloudinary)
    FOREIGN KEY(generated_by) REFERENCES Users(user_id)
);
GO

-- =====================================================
-- SYSTEM SETTINGS
-- Luu tat ca gia tri tham so Business Rules
-- =====================================================
CREATE TABLE SystemSettings (
    setting_id    INT IDENTITY(1,1) PRIMARY KEY,
    setting_key   VARCHAR(100)  NOT NULL UNIQUE,
    setting_value NVARCHAR(255) NOT NULL,
    description   NVARCHAR(500)
);
GO

-- =====================================================
-- SYSTEM LOGS
-- BR-26: Ghi lai moi hoat dong muon, tra, phat, cap nhat TK
-- =====================================================
CREATE TABLE SystemLogs (
    log_id      INT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT           NULL,
    action_type VARCHAR(100)  NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    ip_address  VARCHAR(50)   NULL,
    created_at  DATETIME      DEFAULT GETDATE(),
    FOREIGN KEY(user_id) REFERENCES Users(user_id) ON DELETE SET NULL
);
GO

-- =====================================================
-- SEED DATA: ROLES
-- =====================================================
INSERT INTO Roles(role_name) VALUES ('Admin'), ('Librarian'), ('Member');
GO

-- =====================================================
-- SEED DATA: MEMBERSHIP TIERS
-- BR-34: 2 hang thanh vien
-- BR-32: Loyal phi muon = 50% (borrow_fee_rate = 0.5)
-- =====================================================
INSERT INTO MembershipTiers(tier_name, max_borrow_books, borrow_fee_rate, description)
VALUES
(N'Standard Member', 5,  1.0, N'Thanh vien thuong, phi muon tieu chuan'),
(N'Loyal Member',    10, 0.5, N'Thanh vien than thiet, phi muon = 50% chuan (BR-32)');
GO

-- =====================================================
-- SEED DATA: SYSTEM SETTINGS (tuong ung Business Rules)
-- =====================================================
INSERT INTO SystemSettings(setting_key, setting_value, description)
VALUES
-- BR-7: So ngay muon
('Borrow_Days',              '14',       N'So ngay muon mac dinh (BR-7)'),
-- BR-8: Phi phat qua han
('Fine_Rate_Per_Day',        '5000',     N'Tien phat moi ngay qua han - VND (BR-8)'),
-- BR-6: Gia han
('Max_Renewals',             '2',        N'So lan gia han toi da moi phieu muon (BR-6)'),
('Renewal_Days',             '7',        N'So ngay gia han them moi lan'),
-- BR-15: Giu sach dat truoc
('Reservation_Days',         '3',        N'So ngay giu sach sau khi sach ve (BR-15)'),
-- BR-10: Phi hu hong
('Damage_Fee_Threshold',     '50',       N'Nguong hu hong (%) de tinh boi thuong (BR-10)'),
('Damage_Fee_Amount',        '120000',   N'Phi boi thuong khi sach hu > 50% - VND (BR-10)'),
-- BR-4: Phi muon sach
('Borrow_Fee_Standard',      '5000',     N'Phi muon sach tieu chuan - VND (BR-4)'),
-- BR-31, BR-35: Nang/ha hang
('Loyal_Spend_Threshold',    '1000000',  N'Nguong chi tieu nang hang Loyal Member - VND (BR-31)'),
('Loyal_Inactive_Months',    '6',        N'So thang inactive de ha hang Loyal (BR-35)'),
-- BR-41: Dat coc dat truoc
('Reservation_Deposit_Rate', '0.5',      N'Ti le dat coc = 50% gia tri sach (BR-41)'),
-- BR-11: Khoa TK
('Max_Overdue_Violations',   '3',        N'So lan vi pham qua han de khoa tai khoan (BR-11)'),
-- General
('Max_Balance',              '5000000',  N'So du toi da tai khoan member - VND');
GO
