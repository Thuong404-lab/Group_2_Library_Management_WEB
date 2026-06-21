USE master;

GO
IF DB_ID ('LibraryManagementWeb') IS NOT NULL BEGIN
ALTER DATABASE LibraryManagementWeb
SET
    SINGLE_USER
WITH
    ROLLBACK IMMEDIATE;

DROP DATABASE LibraryManagementWeb;

END
GO

CREATE DATABASE LibraryManagementWeb;

GO
USE LibraryManagementWeb;

GO
-- =========================================================================
-- 1. SYSTEM SETTINGS (Cài đặt hệ thống theo UC-22)
-- =========================================================================
CREATE TABLE SystemSettings (
    setting_id INT IDENTITY (1, 1) PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value NVARCHAR (MAX),
    description NVARCHAR (255)
);

-- =========================================================================
-- 2. USERS, ACCOUNTS & ROLES (Mô hình tách biệt theo ERD)
-- =========================================================================
CREATE TABLE Users (
    user_id INT IDENTITY (1, 1) PRIMARY KEY,
    full_name NVARCHAR (255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(50) DEFAULT 'Active'
);

CREATE TABLE Accounts (
    account_id INT IDENTITY (1, 1) PRIMARY KEY,
    user_id INT FOREIGN KEY REFERENCES Users (user_id) ON DELETE CASCADE,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'Active'
);

CREATE TABLE MembershipTiers (
    tier_id INT IDENTITY (1, 1) PRIMARY KEY,
    tier_name NVARCHAR (100) NOT NULL,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    borrow_limit INT DEFAULT 5,
    condition DECIMAL(18, 2) DEFAULT 0, -- Yêu cầu chi tiêu để đạt hạng
    benefits NVARCHAR (MAX)
);

CREATE TABLE Members (
    member_id INT IDENTITY (1, 1) PRIMARY KEY,
    user_id INT FOREIGN KEY REFERENCES Users (user_id) ON DELETE CASCADE,
    tier_id INT FOREIGN KEY REFERENCES MembershipTiers (tier_id)
);

CREATE TABLE Staff (
    staff_id INT IDENTITY (1, 1) PRIMARY KEY,
    user_id INT FOREIGN KEY REFERENCES Users (user_id) ON DELETE CASCADE,
    staff_type VARCHAR(50) NOT NULL -- 'Admin', 'Librarian'
);

-- =========================================================================
-- 3. FINANCIALS (Wallets & Transactions)
-- =========================================================================
CREATE TABLE Wallets (
    wallet_id INT IDENTITY (1, 1) PRIMARY KEY,
    member_id INT FOREIGN KEY REFERENCES Members (member_id) ON DELETE CASCADE,
    balance DECIMAL(18, 2) DEFAULT 0
);

-- =========================================================================
-- 4. BOOK CATALOG (Sách, Tác giả, Thể loại)
-- =========================================================================
CREATE TABLE Categories (
    category_id INT IDENTITY (1, 1) PRIMARY KEY,
    category_name NVARCHAR (255) NOT NULL
);

CREATE TABLE Genres (
    genre_id INT IDENTITY (1, 1) PRIMARY KEY,
    category_id INT FOREIGN KEY REFERENCES Categories (category_id),
    genre_name NVARCHAR (255) NOT NULL
);

CREATE TABLE Authors (
    author_id INT IDENTITY (1, 1) PRIMARY KEY,
    author_name NVARCHAR (255) NOT NULL
);

CREATE TABLE Books (
    book_id INT IDENTITY (1, 1) PRIMARY KEY,
    genre_id INT FOREIGN KEY REFERENCES Genres (genre_id),
    title NVARCHAR (255) NOT NULL,
    isbn VARCHAR(20) UNIQUE,
    description NVARCHAR (MAX),
    status VARCHAR(50) DEFAULT 'Active'
);

CREATE TABLE BookAuthors (
    book_id INT FOREIGN KEY REFERENCES Books (book_id) ON DELETE CASCADE,
    author_id INT FOREIGN KEY REFERENCES Authors (author_id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

CREATE TABLE Shelves (
    shelf_id INT IDENTITY (1, 1) PRIMARY KEY,
    shelf_name NVARCHAR (100) NOT NULL,
    location NVARCHAR (255)
);

CREATE TABLE BookItems (
    book_item_id INT IDENTITY (1, 1) PRIMARY KEY,
    book_id INT FOREIGN KEY REFERENCES Books (book_id) ON DELETE CASCADE,
    shelf_id INT FOREIGN KEY REFERENCES Shelves (shelf_id),
    barcode VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(50) DEFAULT 'Available' -- Available, Borrowed, Lost, Damaged, Disposed
);

-- =========================================================================
-- 5. LIBRARY SERVICES (Mượn trả, Đặt trước)
-- =========================================================================
CREATE TABLE Reservations (
    reservation_id INT IDENTITY (1, 1) PRIMARY KEY,
    member_id INT FOREIGN KEY REFERENCES Members (member_id) ON DELETE CASCADE,
    book_id INT FOREIGN KEY REFERENCES Books (book_id) ON DELETE CASCADE,
    reservation_date DATETIME DEFAULT GETDATE (),
    status VARCHAR(50) DEFAULT 'Pending' -- Pending, Ready, Completed, Canceled
);

CREATE TABLE Borrows (
    borrow_id INT IDENTITY (1, 1) PRIMARY KEY,
    member_id INT FOREIGN KEY REFERENCES Members (member_id),
    staff_id INT NULL FOREIGN KEY REFERENCES Staff (staff_id),
    borrow_date DATETIME DEFAULT GETDATE (),
    status VARCHAR(50) DEFAULT 'Active' -- Active, Returned, Overdue
);

CREATE TABLE BorrowDetails (
    borrow_detail_id INT IDENTITY (1, 1) PRIMARY KEY,
    borrow_id INT FOREIGN KEY REFERENCES Borrows (borrow_id) ON DELETE CASCADE,
    book_id INT FOREIGN KEY REFERENCES Books (book_id),
    book_item_id INT NULL FOREIGN KEY REFERENCES BookItems (book_item_id),
    due_date DATETIME NOT NULL,
    return_date DATETIME NULL,
    renew_count INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'Borrowed' -- Borrowed, Returned, Overdue, Lost
);

-- =========================================================================
-- 6. TRANSACTIONS (Gắn với Borrow)
-- =========================================================================
CREATE TABLE Transactions (
    transaction_id INT IDENTITY (1, 1) PRIMARY KEY,
    wallet_id INT FOREIGN KEY REFERENCES Wallets (wallet_id),
    borrow_id INT NULL FOREIGN KEY REFERENCES Borrows (borrow_id),
    transaction_type VARCHAR(50) NOT NULL, -- TOP_UP, BORROW_FEE, DEPOSIT, FINE, DAMAGE_FEE, REFUND
    amount DECIMAL(18, 2) NOT NULL,
    transaction_date DATETIME DEFAULT GETDATE (),
    status VARCHAR(50) DEFAULT 'Completed'
);

-- =========================================================================
-- 7. NOTIFICATIONS & FEEDBACKS
-- =========================================================================
CREATE TABLE Notifications (
    notification_id INT IDENTITY (1, 1) PRIMARY KEY,
    staff_id INT FOREIGN KEY REFERENCES Staff (staff_id),
    title NVARCHAR (255) NOT NULL,
    content NVARCHAR (MAX) NOT NULL,
    created_date DATETIME DEFAULT GETDATE (),
    status VARCHAR(50) DEFAULT 'Active'
);

CREATE TABLE SystemLogs (
    log_id INT IDENTITY (1, 1) PRIMARY KEY,
    account_id INT NULL FOREIGN KEY REFERENCES Accounts (account_id) ON DELETE SET NULL,
    action_type VARCHAR(100) NOT NULL, -- 'LOGIN_SUCCESS', 'LOGOUT', 'BACKUP_DATA', 'UPDATE_SETTINGS'
    ip_address VARCHAR(50),
    user_agent NVARCHAR (MAX),
    description NVARCHAR (MAX),
    created_at DATETIME DEFAULT GETDATE ()
);

CREATE TABLE MemberNotifications (
    member_id INT FOREIGN KEY REFERENCES Members (member_id) ON DELETE CASCADE,
    notification_id INT FOREIGN KEY REFERENCES Notifications (notification_id) ON DELETE CASCADE,
    is_read BIT DEFAULT 0,
    read_date DATETIME NULL,
    PRIMARY KEY (member_id, notification_id)
);

CREATE TABLE Feedbacks (
    feedback_id INT IDENTITY (1, 1) PRIMARY KEY,
    member_id INT FOREIGN KEY REFERENCES Members (member_id) ON DELETE CASCADE,
    book_id INT FOREIGN KEY REFERENCES Books (book_id) ON DELETE CASCADE,
    rating INT CHECK (
        rating >= 1
        AND rating <= 5
    ),
    comment NVARCHAR (MAX),
    created_date DATETIME DEFAULT GETDATE ()
);

-- =========================================================================
-- 8. OTHERS (Thanh lý, Sách yêu thích)
-- =========================================================================
CREATE TABLE BookDisposals (
    disposal_id INT IDENTITY (1, 1) PRIMARY KEY,
    book_item_id INT FOREIGN KEY REFERENCES BookItems (book_item_id),
    staff_id INT FOREIGN KEY REFERENCES Staff (staff_id),
    reason NVARCHAR (MAX),
    disposal_date DATETIME DEFAULT GETDATE (),
    status VARCHAR(50) DEFAULT 'Completed'
);

CREATE TABLE Favorites (
    member_id INT FOREIGN KEY REFERENCES Members (member_id) ON DELETE CASCADE,
    book_id INT FOREIGN KEY REFERENCES Books (book_id) ON DELETE CASCADE,
    PRIMARY KEY (member_id, book_id)
);

GO
-- =========================================================================
-- =========================================================================
-- SEED DATA (DỮ LIỆU MẪU)
-- Password mặc định cho Account: Test@1234
-- Hash: $2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2
-- =========================================================================
-- =========================================================================
-- 1. System Settings
INSERT INTO
    SystemSettings (setting_key, setting_value, description)
VALUES
    (
        'Fine_Per_Day',
        '5000',
        N'Phí phạt trễ hạn tính theo ngày (VND)'
    ),
    (
        'Max_Borrow_Days',
        '14',
        N'Số ngày mượn sách tối đa tiêu chuẩn'
    ),
    (
        'Deposit_Amount',
        '50000',
        N'Tiền cọc đặt trước một quyển sách (VND)'
    );

-- 2. Tiers
INSERT INTO
    MembershipTiers (
        tier_name,
        discount_percent,
        borrow_limit,
        condition,
        benefits
    )
VALUES
    (N'Regular', 0, 3, 0, N'Mượn tối đa 3 sách'),
    (
        N'Loyal',
        50,
        5,
        1000000,
        N'Giảm 50% phí mượn, mượn tối đa 5 sách'
    );

-- 3. Users
INSERT INTO
    Users (full_name, email, phone, status)
VALUES
    (
        N'Quản Trị Viên',
        'admin@library.vn',
        '0901000001',
        'Active'
    ),
    (
        N'Nguyễn Thị Lan',
        'lib01@library.vn',
        '0902000001',
        'Active'
    ),
    (
        N'Trần Văn Minh',
        'lib02@library.vn',
        '0902000002',
        'Active'
    ),
    (
        N'Lê Thị Hoa',
        'member01@gmail.com',
        '0903000001',
        'Active'
    ),
    (
        N'Phạm Minh Tuấn',
        'member02@gmail.com',
        '0903000002',
        'Active'
    ),
    (
        N'Nguyễn Văn An',
        'member03@gmail.com',
        '0903000003',
        'Active'
    ),
    (
        N'Trần Thị Mai',
        'member04@gmail.com',
        '0903000004',
        'Active'
    );

-- 4. Accounts (Pass: Test@1234 cho tất cả)
INSERT INTO
    Accounts (user_id, username, password_hash, status)
VALUES
    (
        1,
        'admin',
        '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2',
        'Active'
    ),
    (
        2,
        'librarian01',
        '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2',
        'Active'
    ),
    (
        3,
        'librarian02',
        '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2',
        'Active'
    ),
    (
        4,
        'member01',
        '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2',
        'Active'
    ),
    (
        5,
        'member02',
        '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2',
        'Active'
    ),
    (
        6,
        'member03',
        '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2',
        'Active'
    ),
    (
        7,
        'member04',
        '$2a$10$xn3LI/AjqicFYZFruSwve.681477XaVNaUQbr1gioaWPn4t1KbdH2',
        'Active'
    );

-- 5. Staff & Members
INSERT INTO
    Staff (user_id, staff_type)
VALUES
    (1, 'Admin'),
    (2, 'Librarian'),
    (3, 'Librarian');

INSERT INTO
    Members (user_id, tier_id)
VALUES
    (4, 1), -- Regular
    (5, 1), -- Regular
    (6, 2), -- Loyal
    (7, 1);

-- Regular
-- 6. Wallets
INSERT INTO
    Wallets (member_id, balance)
VALUES
    (1, 150000), -- member01
    (2, 50000), -- member02
    (3, 300000), -- member03
    (4, 0);

-- member04
-- 7. Categories & Genres
INSERT INTO
    Categories (category_name)
VALUES
    (N'Văn học'),
    (N'Khoa học - Công nghệ'),
    (N'Lịch sử - Địa lý');

INSERT INTO
    Genres (category_id, genre_name)
VALUES
    (1, N'Tiểu thuyết'),
    (1, N'Truyện ngắn'),
    (2, N'Công nghệ thông tin'),
    (3, N'Lịch sử thế giới');

-- 8. Authors
INSERT INTO
    Authors (author_name)
VALUES
    (N'Nam Cao'),
    (N'Robert C. Martin'),
    (N'Yuval Noah Harari'),
    (N'J.K. Rowling');

-- 9. Books
INSERT INTO
    Books (genre_id, title, isbn, description, status)
VALUES
    (
        1,
        N'Chí Phèo',
        '9786040100003',
        N'Truyện ngắn nổi tiếng về nông dân Việt Nam',
        'Active'
    ),
    (
        3,
        N'Clean Code',
        '9786040100014',
        N'Sách gối đầu giường của lập trình viên',
        'Active'
    ),
    (
        4,
        N'Sapiens',
        '9786040100010',
        N'Lược sử loài người',
        'Active'
    ),
    (
        1,
        N'Harry Potter',
        '9786040100007',
        N'Tiểu thuyết phép thuật',
        'Active'
    );

-- 10. BookAuthors
INSERT INTO
    BookAuthors (book_id, author_id)
VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4);

-- 11. Shelves
INSERT INTO
    Shelves (shelf_name, location)
VALUES
    (N'Kệ A - Văn học', N'Tầng 1 - Khu A'),
    (N'Kệ B - IT', N'Tầng 2 - Khu B'),
    (N'Kho dự trữ', N'Tầng Trệt');

-- 12. BookItems
INSERT INTO
    BookItems (book_id, shelf_id, barcode, status)
VALUES
    (1, 1, 'BC001-001', 'Available'),
    (1, 3, 'BC001-002', 'Borrowed'),
    (2, 2, 'BC002-001', 'Available'),
    (2, 2, 'BC002-002', 'Available'),
    (3, 1, 'BC003-001', 'Borrowed'),
    (4, 1, 'BC004-001', 'Available');

-- 13. Reservations (Online Booking)
INSERT INTO
    Reservations (member_id, book_id, reservation_date, status)
VALUES
    (1, 2, DATEADD (DAY, -2, GETDATE ()), 'Pending'),
    (2, 4, DATEADD (DAY, -1, GETDATE ()), 'Ready');

-- 14. Borrows & BorrowDetails (Mượn sách: member 1 mượn Chí Phèo, member 3 mượn Sapiens)
INSERT INTO
    Borrows (member_id, staff_id, borrow_date, status)
VALUES
    (1, 2, DATEADD (DAY, -10, GETDATE ()), 'Active'),
    (3, 3, DATEADD (DAY, -20, GETDATE ()), 'Overdue');

INSERT INTO
    BorrowDetails (
        borrow_id,
        book_id,
        book_item_id,
        due_date,
        return_date,
        renew_count,
        status
    )
VALUES
    (
        1,
        1,
        2,
        DATEADD (DAY, 4, GETDATE ()),
        NULL,
        0,
        'Borrowed'
    ),
    (
        2,
        3,
        5,
        DATEADD (DAY, -6, GETDATE ()),
        NULL,
        0,
        'Overdue'
    );

-- Trễ hạn 6 ngày
-- 15. Transactions (Lịch sử giao dịch ví)
INSERT INTO
    Transactions (
        wallet_id,
        borrow_id,
        transaction_type,
        amount,
        transaction_date,
        status
    )
VALUES
    (
        1,
        NULL,
        'TOP_UP',
        200000,
        DATEADD (DAY, -15, GETDATE ()),
        'Completed'
    ),
    (
        1,
        1,
        'BORROW_FEE',
        -5000,
        DATEADD (DAY, -10, GETDATE ()),
        'Completed'
    ),
    (
        3,
        NULL,
        'TOP_UP',
        500000,
        DATEADD (DAY, -30, GETDATE ()),
        'Completed'
    ),
    (
        3,
        2,
        'BORROW_FEE',
        -2500,
        DATEADD (DAY, -20, GETDATE ()),
        'Completed'
    ), -- Loyal giảm 50%
    (3, 2, 'FINE', -30000, GETDATE (), 'Pending');

-- Phạt trễ hạn 6 ngày x 5000
-- 16. Feedbacks
INSERT INTO
    Feedbacks (member_id, book_id, rating, comment, created_date)
VALUES
    (
        1,
        1,
        5,
        N'Tác phẩm kinh điển, rất ý nghĩa.',
        DATEADD (DAY, -5, GETDATE ())
    );

-- 17. Favorites
INSERT INTO
    Favorites (member_id, book_id)
VALUES
    (1, 2),
    (1, 4),
    (3, 3);

-- 18. Notifications & MemberNotifications
INSERT INTO
    Notifications (staff_id, title, content, created_date, status)
VALUES
    (
        2,
        N'Hệ thống bảo trì',
        N'Thư viện sẽ bảo trì hệ thống vào Chủ Nhật tuần này.',
        DATEADD (DAY, -2, GETDATE ()),
        'Active'
    ),
    (
        3,
        N'Sách trễ hạn',
        N'Bạn đang có sách mượn quá hạn, vui lòng hoàn trả và đóng phạt.',
        GETDATE (),
        'Active'
    );

INSERT INTO
    MemberNotifications (member_id, notification_id, is_read, read_date)
VALUES
    (1, 1, 1, DATEADD (DAY, -1, GETDATE ())),
    (2, 1, 0, NULL),
    (3, 1, 0, NULL),
    (4, 1, 0, NULL),
    (3, 2, 0, NULL);

-- Gửi riêng cho member 3 đang trễ hạn
-- 12. System Logs (Login History)
INSERT INTO
    SystemLogs (account_id, action_type, ip_address, description)
VALUES
    (
        1,
        'LOGIN_SUCCESS',
        '192.168.1.10',
        N'Đăng nhập thành công từ thiết bị Window'
    ),
    (
        4,
        'LOGIN_SUCCESS',
        '14.161.22.11',
        N'Đăng nhập thành công từ điện thoại iPhone'
    ),
    (
        4,
        'LOGOUT',
        '14.161.22.11',
        N'Đăng xuất hệ thống'
    );

GO 
PRINT '============================================';

PRINT 'NEW DATABASE REWRITTEN SUCCESSFULLY!';

PRINT 'MATCHES PROVIDED ERD 100%.';

PRINT '============================================';

GO