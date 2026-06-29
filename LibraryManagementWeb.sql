USE master;
GO

IF DB_ID ('LibraryManagementWeb') IS NOT NULL BEGIN
ALTER DATABASE LibraryManagementWeb SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
DROP DATABASE LibraryManagementWeb;
END
GO

CREATE DATABASE LibraryManagementWeb;
GO
USE LibraryManagementWeb;
GO

-- =========================================================================
-- 1. SYSTEM SETTINGS
-- =========================================================================
CREATE TABLE SystemSettings (
    setting_id INT IDENTITY (1, 1) PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value NVARCHAR (MAX),
    description NVARCHAR (255)
);

-- =========================================================================
-- 2. USERS, ACCOUNTS & ROLES
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

CREATE TABLE Roles (
    role_id INT IDENTITY (1, 1) PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE Account_Roles (
    account_id INT FOREIGN KEY REFERENCES Accounts (account_id) ON DELETE CASCADE,
    role_id INT FOREIGN KEY REFERENCES Roles (role_id) ON DELETE CASCADE,
    PRIMARY KEY (account_id, role_id)
);

CREATE TABLE MembershipTiers (
    tier_id INT IDENTITY (1, 1) PRIMARY KEY,
    tier_name NVARCHAR (100) NOT NULL,
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    borrow_limit INT DEFAULT 5,
    condition DECIMAL(18, 2) DEFAULT 0,
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
    staff_type VARCHAR(50) NOT NULL
);

-- =========================================================================
-- 3. FINANCIALS
-- =========================================================================
CREATE TABLE Wallets (
    wallet_id INT IDENTITY (1, 1) PRIMARY KEY,
    member_id INT FOREIGN KEY REFERENCES Members (member_id) ON DELETE CASCADE,
    balance DECIMAL(18, 2) DEFAULT 0
);

-- =========================================================================
-- 4. BOOK CATALOG
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
    status VARCHAR(50) DEFAULT 'Available'
);

-- =========================================================================
-- 5. LIBRARY SERVICES
-- =========================================================================
CREATE TABLE Reservations (
    reservation_id INT IDENTITY (1, 1) PRIMARY KEY,
    member_id INT FOREIGN KEY REFERENCES Members (member_id) ON DELETE CASCADE,
    book_id INT FOREIGN KEY REFERENCES Books (book_id) ON DELETE CASCADE,
    reservation_date DATETIME DEFAULT GETDATE (),
    status VARCHAR(50) DEFAULT 'Pending'
);

CREATE TABLE Borrows (
    borrow_id INT IDENTITY (1, 1) PRIMARY KEY,
    member_id INT FOREIGN KEY REFERENCES Members (member_id),
    staff_id INT NULL FOREIGN KEY REFERENCES Staff (staff_id),
    borrow_date DATETIME DEFAULT GETDATE (),
    status VARCHAR(50) DEFAULT 'Active'
);

CREATE TABLE BorrowDetails (
    borrow_detail_id INT IDENTITY (1, 1) PRIMARY KEY,
    borrow_id INT FOREIGN KEY REFERENCES Borrows (borrow_id) ON DELETE CASCADE,
    book_id INT FOREIGN KEY REFERENCES Books (book_id),
    book_item_id INT NULL FOREIGN KEY REFERENCES BookItems (book_item_id),
    due_date DATETIME NOT NULL,
    return_date DATETIME NULL,
    renew_count INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'Borrowed'
);

-- =========================================================================
-- 6. TRANSACTIONS
-- =========================================================================
CREATE TABLE Transactions (
    transaction_id INT IDENTITY (1, 1) PRIMARY KEY,
    wallet_id INT FOREIGN KEY REFERENCES Wallets (wallet_id),
    borrow_id INT NULL FOREIGN KEY REFERENCES Borrows (borrow_id),
    transaction_type VARCHAR(50) NOT NULL,
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
    action_type VARCHAR(100) NOT NULL,
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
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment NVARCHAR (MAX),
    created_date DATETIME DEFAULT GETDATE ()
);

-- =========================================================================
-- 8. OTHERS
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
-- SEED DATA - GENERATED
-- Password mac dinh cho Account: Test@1234
-- =========================================================================
INSERT INTO SystemSettings (setting_key, setting_value, description)
VALUES
    ('Fine_Per_Day', '5000', N'Phí phạt trễ hạn tính theo ngày (VND)'),
    ('Max_Borrow_Days', '14', N'Số ngày mượn sách tối đa tiêu chuẩn'),
    ('Deposit_Amount', '50000', N'Tiền cọc đặt trước một quyển sách (VND)');

INSERT INTO MembershipTiers (tier_name, discount_percent, borrow_limit, condition, benefits)
VALUES
    (N'Regular', 0, 3, 0, N'Mượn tối đa 3 sách'),
    (N'Silver', 10, 5, 500000, N'Giảm 10% phí mượn, mượn tối đa 5 sách'),
    (N'Gold', 20, 10, 1000000, N'Giảm 20% phí mượn, mượn tối đa 10 sách'),
    (N'Diamond', 50, 20, 5000000, N'Giảm 50% phí mượn, mượn tối đa 20 sách');

INSERT INTO Roles (name) VALUES ('ADMIN'), ('LIBRARIAN'), ('MEMBER');

INSERT INTO Users (full_name, email, phone, status)
VALUES
    (N'Admin Tổng', 'admin@library.vn', '0901000001', 'Active'),
    (N'Nguyễn Thị Lan', 'lib01@library.vn', '0902000001', 'Active'),
    (N'Trần Văn Minh', 'lib02@library.vn', '0902000002', 'Active'),
    (N'Lê Thị Hoa', 'member01@gmail.com', '0903000001', 'Active'),
    (N'Phạm Minh Tuấn', 'member02@gmail.com', '0903000002', 'Active'),
    (N'Nguyễn Văn An', 'member03@gmail.com', '0903000003', 'Active'),
    (N'Trần Thị Mai', 'member04@gmail.com', '0903000004', 'Active'),
    (N'Hoàng Văn Dũng', 'member05@gmail.com', '0903000005', 'Active'),
    (N'Đặng Thu Hà', 'member06@gmail.com', '0903000006', 'Active'),
    (N'Vũ Đức Cường', 'member07@gmail.com', '0903000007', 'Active'),
    (N'Bùi Thị Thanh', 'member08@gmail.com', '0903000008', 'Active'),
    (N'Đỗ Xuân Hùng', 'member09@gmail.com', '0903000009', 'Active'),
    (N'Ngô Bảo Châu', 'member10@gmail.com', '0903000010', 'Active'),
    (N'Lý Hải Yến', 'member11@gmail.com', '0903000011', 'Active'),
    (N'Dương Tấn Sang', 'member12@gmail.com', '0903000012', 'Active');

INSERT INTO Accounts (user_id, username, password_hash, status)
VALUES
    (1, 'admin', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (2, 'librarian01', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (3, 'librarian02', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (4, 'member01', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (5, 'member02', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (6, 'member03', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (7, 'member04', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (8, 'member05', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (9, 'member06', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (10, 'member07', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (11, 'member08', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (12, 'member09', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (13, 'member10', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (14, 'member11', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active'),
    (15, 'member12', '$2a$10$bJLIrjlLH.3QENfIE03./eqJDJKTF.xaMoUS4IjQU1zPzNlJK0cxu', 'Active');

INSERT INTO Account_Roles (account_id, role_id)
VALUES
    (1, 1), (2, 2), (3, 2), (4, 3), (5, 3), (6, 3), (7, 3), (8, 3), 
    (9, 3), (10, 3), (11, 3), (12, 3), (13, 3), (14, 3), (15, 3);

INSERT INTO Staff (user_id, staff_type)
VALUES (1, 'Admin'), (2, 'Librarian'), (3, 'Librarian');

INSERT INTO Members (user_id, tier_id)
VALUES
    (4, 1), (5, 1), (6, 2), (7, 1), (8, 3), (9, 4), (10, 1), (11, 2), 
    (12, 1), (13, 1), (14, 2), (15, 3);

INSERT INTO Wallets (member_id, balance)
VALUES
    (1, 150000), (2, 50000), (3, 300000), (4, 0), (5, 500000), 
    (6, 1200000), (7, 10000), (8, 250000), (9, 0), (10, 80000), 
    (11, 15000), (12, 1000000);

-- CATALOG
INSERT INTO Categories (category_name)
VALUES
    (N'Văn học'), (N'Khoa học - Công nghệ'), (N'Lịch sử - Địa lý'),
    (N'Kinh tế - Tài chính'), (N'Kỹ năng sống'), (N'Tiểu thuyết - Hư cấu');

INSERT INTO Genres (category_id, genre_name)
VALUES
    (1, N'Tiểu thuyết văn học'), (1, N'Truyện ngắn'),
    (2, N'Công nghệ thông tin'), (2, N'Khoa học vũ trụ'),
    (3, N'Lịch sử thế giới'), (3, N'Lịch sử Việt Nam'),
    (4, N'Đầu tư tài chính'), (4, N'Kinh doanh khởi nghiệp'),
    (5, N'Tâm lý học'), (5, N'Phát triển bản thân'),
    (6, N'Hành động - Kỳ ảo'), (6, N'Trinh thám');

INSERT INTO Authors (author_name)
VALUES
    (N'Nam Cao'), (N'Robert C. Martin'), (N'Yuval Noah Harari'),
    (N'J.K. Rowling'), (N'Dale Carnegie'), (N'Paulo Coelho'),
    (N'Higashino Keigo'), (N'Nguyễn Nhật Ánh'), (N'George S. Clason'),
    (N'Tô Hoài');

INSERT INTO Books (genre_id, title, isbn, description, status)
VALUES
    (1, N'Chí Phèo', '9786040100003', N'Truyện ngắn kinh điển Việt Nam.', 'Active'),
    (3, N'Clean Code', '9786040100014', N'Sách gối đầu giường của lập trình viên.', 'Active'),
    (5, N'Sapiens: Lược Sử Loài Người', '9786040100010', N'Lịch sử tiến hóa nhân loại.', 'Active'),
    (11, N'Harry Potter và Hòn Đá Phù Thủy', '9786040100007', N'Tiểu thuyết phép thuật nổi tiếng.', 'Active'),
    (10, N'Đắc Nhân Tâm', '9786040100021', N'Nghệ thuật thu phục lòng người.', 'Active'),
    (1, N'Nhà Giả Kim', '9786040100038', N'Hành trình đi tìm kho báu.', 'Active'),
    (12, N'Phía Sau Nghi Can X', '9786040100045', N'Trinh thám Nhật Bản hấp dẫn.', 'Active'),
    (1, N'Mắt Biếc', '9786040100052', N'Tình yêu tuổi học trò buồn.', 'Active'),
    (7, N'Người Giàu Có Nhất Thành Babylon', '9786040100069', N'Bí quyết làm giàu từ xa xưa.', 'Active'),
    (2, N'Dế Mèn Phiêu Lưu Ký', '9786040100076', N'Truyện thiếu nhi kinh điển.', 'Active'),
    (3, N'Clean Architecture', '9786040100083', N'Kiến trúc phần mềm sạch.', 'Active'),
    (5, N'21 Bài Học Cho Thế Kỷ 21', '9786040100090', N'Góc nhìn sâu sắc về hiện tại.', 'Active'),
    (11, N'Harry Potter và Phòng Chứa Bí Mật', '9786040100106', N'Tập 2 của Harry Potter.', 'Active'),
    (1, N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', '9786040100113', N'Ký ức tuổi thơ tươi đẹp.', 'Active'),
    (12, N'Bạch Dạ Hành', '9786040100120', N'Tiểu thuyết trinh thám ám ảnh.', 'Active');

INSERT INTO BookAuthors (book_id, author_id)
VALUES
    (1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7),
    (8, 8), (9, 9), (10, 10), (11, 2), (12, 3), (13, 4), (14, 8), (15, 7);

INSERT INTO Shelves (shelf_name, location)
VALUES
    (N'Kệ A1 - Văn học VN', N'Tầng 1 - Khu A'),
    (N'Kệ A2 - Văn học NN', N'Tầng 1 - Khu A'),
    (N'Kệ B1 - IT', N'Tầng 2 - Khu B'),
    (N'Kệ C1 - Kỹ năng', N'Tầng 3 - Khu C'),
    (N'Kệ D1 - Trinh thám', N'Tầng 2 - Khu D');

INSERT INTO BookItems (book_id, shelf_id, barcode, status)
VALUES
    (1, 1, 'BC001-001', 'Available'), (1, 1, 'BC001-002', 'Borrowed'),
    (2, 3, 'BC002-001', 'Available'), (2, 3, 'BC002-002', 'Available'), (2, 3, 'BC002-003', 'Available'),
    (3, 2, 'BC003-001', 'Borrowed'), (3, 2, 'BC003-002', 'Available'),
    (4, 2, 'BC004-001', 'Available'), (4, 2, 'BC004-002', 'Available'),
    (5, 4, 'BC005-001', 'Available'), (5, 4, 'BC005-002', 'Available'), (5, 4, 'BC005-003', 'Available'),
    (6, 2, 'BC006-001', 'Available'), (6, 2, 'BC006-002', 'Available'),
    (7, 5, 'BC007-001', 'Borrowed'), (7, 5, 'BC007-002', 'Available'),
    (8, 1, 'BC008-001', 'Available'), (8, 1, 'BC008-002', 'Available'),
    (9, 4, 'BC009-001', 'Borrowed'), (9, 4, 'BC009-002', 'Available'),
    (10, 1, 'BC010-001', 'Available'),
    (11, 3, 'BC011-001', 'Available'),
    (12, 2, 'BC012-001', 'Available'),
    (13, 2, 'BC013-001', 'Available'),
    (14, 1, 'BC014-001', 'Available'),
    (15, 5, 'BC015-001', 'Available');

INSERT INTO Borrows (member_id, staff_id, borrow_date, status)
VALUES
    (1, 2, DATEADD(DAY, -10, GETDATE()), 'Active'),
    (3, 3, DATEADD(DAY, -20, GETDATE()), 'Overdue'),
    (5, 2, DATEADD(DAY, -5, GETDATE()), 'Active'),
    (7, 3, DATEADD(DAY, -2, GETDATE()), 'Active'),
    (2, 2, DATEADD(DAY, -30, GETDATE()), 'Returned'),
    (9, 3, DATEADD(DAY, -15, GETDATE()), 'Overdue');

INSERT INTO BorrowDetails (borrow_id, book_id, book_item_id, due_date, return_date, renew_count, status)
VALUES
    (1, 1, 2, DATEADD(DAY, 4, GETDATE()), NULL, 0, 'Borrowed'),
    (2, 3, 6, DATEADD(DAY, -6, GETDATE()), NULL, 0, 'Overdue'),
    (3, 7, 15, DATEADD(DAY, 9, GETDATE()), NULL, 0, 'Borrowed'),
    (4, 9, 19, DATEADD(DAY, 12, GETDATE()), NULL, 0, 'Borrowed'),
    (5, 5, 10, DATEADD(DAY, -16, GETDATE()), DATEADD(DAY, -18, GETDATE()), 0, 'Returned'),
    (6, 6, 13, DATEADD(DAY, -1, GETDATE()), NULL, 0, 'Overdue');

INSERT INTO Transactions (wallet_id, borrow_id, transaction_type, amount, transaction_date, status)
VALUES
    (1, NULL, 'TOP_UP', 200000, DATEADD(DAY, -15, GETDATE()), 'Completed'),
    (1, 1, 'BORROW_FEE', -5000, DATEADD(DAY, -10, GETDATE()), 'Completed'),
    (3, NULL, 'TOP_UP', 500000, DATEADD(DAY, -30, GETDATE()), 'Completed'),
    (3, 2, 'BORROW_FEE', -2500, DATEADD(DAY, -20, GETDATE()), 'Completed'),
    (5, NULL, 'TOP_UP', 1000000, DATEADD(DAY, -10, GETDATE()), 'Completed'),
    (2, 5, 'BORROW_FEE', -5000, DATEADD(DAY, -30, GETDATE()), 'Completed');

INSERT INTO Feedbacks (member_id, book_id, rating, comment, created_date)
VALUES
    (1, 1, 5, N'Tác phẩm kinh điển, rất ý nghĩa.', DATEADD(DAY, -5, GETDATE())),
    (2, 5, 5, N'Đọc xong thấy bản thân thay đổi nhiều.', DATEADD(DAY, -2, GETDATE())),
    (4, 2, 4, N'Khá hay nhưng hơi khó đọc cho người mới.', DATEADD(DAY, -1, GETDATE())),
    (6, 7, 5, N'Cốt truyện quá đỉnh, không đoán được cái kết.', GETDATE()),
    (8, 10, 5, N'Sách gắn liền với tuổi thơ.', GETDATE());

INSERT INTO Favorites (member_id, book_id)
VALUES
    (1, 2), (1, 4), (1, 7), (2, 5), (2, 8),
    (3, 3), (3, 11), (4, 1), (5, 9), (6, 7);

GO 
PRINT '============================================';
PRINT 'NEW DATABASE GENERATED SUCCESSFULLY!';
PRINT 'INCLUDES ROLES SCHEMA AND EXTENDED SEED DATA.';
PRINT '============================================';
GO