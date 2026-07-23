/*
 * Library Management Web - safe demonstration data
 * Run manually after Flyway has applied every migration. Safe to rerun after a complete application.
 * All people, contacts and payment references are fictitious.
 * Demo password for every seeded account: Demo@123
 */
USE [LibraryManagementWeb];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.DemoSeedHistory', N'U') IS NULL
        THROW 51000, 'Run all Flyway migrations before applying the demonstration seed.', 1;

    DECLARE @PasswordHash VARCHAR(255) = '$2a$10$0ZPpDwDviBhxvqCU0rl46uMDpgIrZ93eBGJXDmJMXqlYmUTBuoTlW';
    DECLARE @Now DATETIME2(6) = SYSUTCDATETIME();
    DECLARE @SeedKey VARCHAR(100) = 'library-demo-v1';

    IF EXISTS (SELECT 1 FROM dbo.DemoSeedHistory WHERE seed_key = @SeedKey)
    BEGIN
        PRINT 'Demonstration seed already applied; no changes were made.';
        COMMIT TRANSACTION;
        RETURN;
    END;

    /* Adopt a complete seed created before DemoSeedHistory existed. */
    IF EXISTS (SELECT 1 FROM dbo.Staff_Accounts WHERE username = 'admin')
       AND EXISTS (SELECT 1 FROM dbo.Staff_Accounts WHERE username = 'librarian01')
       AND EXISTS (SELECT 1 FROM dbo.Member_Accounts WHERE username = 'member20')
       AND EXISTS (SELECT 1 FROM dbo.Books WHERE book_id = 30)
       AND EXISTS (SELECT 1 FROM dbo.Transactions WHERE transaction_id = 20)
       AND EXISTS (SELECT 1 FROM dbo.Notifications WHERE notification_id = 12)
    BEGIN
        INSERT dbo.DemoSeedHistory(seed_key) VALUES (@SeedKey);
        PRINT 'Existing complete demonstration seed registered; no duplicate rows were inserted.';
        COMMIT TRANSACTION;
        RETURN;
    END;

    /* Refuse to mix fixed-ID demo rows with existing or partially seeded data. */
    IF EXISTS (SELECT 1 FROM dbo.Roles)
       OR EXISTS (SELECT 1 FROM dbo.Users)
       OR EXISTS (SELECT 1 FROM dbo.Books)
       OR EXISTS (SELECT 1 FROM dbo.Transactions)
       OR EXISTS (SELECT 1 FROM dbo.Notifications)
        THROW 51001, 'Database is not empty or contains a partial demo seed; no changes were made.', 1;

    SET IDENTITY_INSERT dbo.Roles ON;
    INSERT dbo.Roles(role_id,name) VALUES
      (1,'ROLE_ADMIN'),(2,'ROLE_LIBRARIAN'),(3,'ROLE_MEMBER');
    SET IDENTITY_INSERT dbo.Roles OFF;

    SET IDENTITY_INSERT dbo.MembershipTiers ON;
    INSERT dbo.MembershipTiers(tier_id,tier_name,discount_percent,borrow_limit,[condition],benefits) VALUES
      (1,N'Member',0,5,0,N'Borrow up to 5 books concurrently.'),
      (2,N'Silver',5,7,500000,N'5% borrowing-fee discount and a 7-book concurrent limit.'),
      (3,N'Gold',10,10,2000000,N'10% borrowing-fee discount and a 10-book concurrent limit.'),
      (4,N'Diamond',15,15,5000000,N'15% borrowing-fee discount and a 15-book concurrent limit.');
    SET IDENTITY_INSERT dbo.MembershipTiers OFF;

    SET IDENTITY_INSERT dbo.Users ON;
    INSERT dbo.Users(user_id,full_name,email,phone,status,avatar) VALUES
      (1,N'Quản trị hệ thống','admin@demo.library','0900000001','Active',NULL),
      (2,N'Thủ thư Minh Anh','librarian01@demo.library','0900000002','Active',NULL),
      (3,N'Thủ thư Hoàng Nam','librarian02@demo.library','0900000003','Active',NULL),
      (4,N'Nguyễn An','member01@demo.library','0910000001','Active',NULL),
      (5,N'Trần Bình','member02@demo.library','0910000002','Active',NULL),
      (6,N'Lê Chi','member03@demo.library','0910000003','Active',NULL),
      (7,N'Phạm Dũng','member04@demo.library','0910000004','Active',NULL),
      (8,N'Hoàng Giang','member05@demo.library','0910000005','Active',NULL),
      (9,N'Vũ Hà','member06@demo.library','0910000006','Active',NULL),
      (10,N'Đặng Huy','member07@demo.library','0910000007','Active',NULL),
      (11,N'Bùi Khánh','member08@demo.library','0910000008','Active',NULL),
      (12,N'Đỗ Lan','member09@demo.library','0910000009','Active',NULL),
      (13,N'Hồ Minh','member10@demo.library','0910000010','Active',NULL),
      (14,N'Ngô Ngân','member11@demo.library','0910000011','Active',NULL),
      (15,N'Dương Phúc','member12@demo.library','0910000012','Active',NULL),
      (16,N'Lý Quân','member13@demo.library','0910000013','Active',NULL),
      (17,N'Mai Thảo','member14@demo.library','0910000014','Active',NULL),
      (18,N'Cao Uyên','member15@demo.library','0910000015','Active',NULL),
      (19,N'Tạ Việt','member16@demo.library','0910000016','Active',NULL),
      (20,N'Chu Xuân','member17@demo.library','0910000017','Active',NULL),
      (21,N'Đinh Yến','member18@demo.library','0910000018','Active',NULL),
      (22,N'La Gia Bảo','member19@demo.library','0910000019','Inactive',NULL),
      (23,N'Kiều Hải Đăng','member20@demo.library','0910000020','Blocked',NULL);
    SET IDENTITY_INSERT dbo.Users OFF;

    SET IDENTITY_INSERT dbo.Staff ON;
    INSERT dbo.Staff(staff_id,user_id,staff_type) VALUES
      (1,1,'Admin'),(2,2,'Librarian'),(3,3,'Librarian');
    SET IDENTITY_INSERT dbo.Staff OFF;

    SET IDENTITY_INSERT dbo.Staff_Accounts ON;
    INSERT dbo.Staff_Accounts(id,staff_id,username,password_hash,status) VALUES
      (1,1,'admin',@PasswordHash,'Active'),
      (2,2,'librarian01',@PasswordHash,'Active'),
      (3,3,'librarian02',@PasswordHash,'Active');
    SET IDENTITY_INSERT dbo.Staff_Accounts OFF;
    INSERT dbo.Staff_Account_Roles(staff_account_id,role_id) VALUES (1,1),(2,2),(3,2);

    SET IDENTITY_INSERT dbo.Members ON;
    INSERT dbo.Members(member_id,user_id,tier_id) VALUES
      (1,4,3),(2,5,2),(3,6,1),(4,7,2),(5,8,3),
      (6,9,1),(7,10,1),(8,11,2),(9,12,3),(10,13,4),
      (11,14,1),(12,15,2),(13,16,1),(14,17,3),(15,18,2),
      (16,19,1),(17,20,1),(18,21,2),(19,22,1),(20,23,1);
    SET IDENTITY_INSERT dbo.Members OFF;

    SET IDENTITY_INSERT dbo.Member_Accounts ON;
    INSERT dbo.Member_Accounts(id,member_id,username,password_hash,status,preferred_language) VALUES
      (1,1,'member01',@PasswordHash,'Active',N'vi'),(2,2,'member02',@PasswordHash,'Active',N'vi'),
      (3,3,'member03',@PasswordHash,'Active',N'en'),(4,4,'member04',@PasswordHash,'Active',N'vi'),
      (5,5,'member05',@PasswordHash,'Active',N'vi'),(6,6,'member06',@PasswordHash,'Active',N'en'),
      (7,7,'member07',@PasswordHash,'Active',N'vi'),(8,8,'member08',@PasswordHash,'Active',N'vi'),
      (9,9,'member09',@PasswordHash,'Active',N'en'),(10,10,'member10',@PasswordHash,'Active',N'vi'),
      (11,11,'member11',@PasswordHash,'Active',N'vi'),(12,12,'member12',@PasswordHash,'Active',N'en'),
      (13,13,'member13',@PasswordHash,'Active',N'vi'),(14,14,'member14',@PasswordHash,'Active',N'vi'),
      (15,15,'member15',@PasswordHash,'Active',N'en'),(16,16,'member16',@PasswordHash,'Active',N'vi'),
      (17,17,'member17',@PasswordHash,'Active',N'vi'),(18,18,'member18',@PasswordHash,'Active',N'en'),
      (19,19,'member19',@PasswordHash,'Inactive',N'vi'),(20,20,'member20',@PasswordHash,'Blocked',N'vi');
    SET IDENTITY_INSERT dbo.Member_Accounts OFF;
    INSERT dbo.Member_Account_Roles(member_account_id,role_id)
    SELECT id,3 FROM dbo.Member_Accounts;

    SET IDENTITY_INSERT dbo.Wallets ON;
    INSERT dbo.Wallets(wallet_id,member_id,balance) VALUES
      (1,1,295000),(2,2,180000),(3,3,90000),(4,4,140000),(5,5,150000),
      (6,6,75000),(7,7,120000),(8,8,350000),(9,9,210000),(10,10,600000),
      (11,11,50000),(12,12,125000),(13,13,80000),(14,14,420000),(15,15,160000),
      (16,16,100000),(17,17,65000),(18,18,225000),(19,19,0),(20,20,0);
    SET IDENTITY_INSERT dbo.Wallets OFF;

    SET IDENTITY_INSERT dbo.Categories ON;
    INSERT dbo.Categories(category_id,category_name) VALUES
      (1,N'Literature'),(2,N'Science'),(3,N'Technology'),(4,N'Business and Economics'),
      (5,N'History'),(6,N'Psychology and Life Skills'),(7,N'Children'),(8,N'Languages');
    SET IDENTITY_INSERT dbo.Categories OFF;

    SET IDENTITY_INSERT dbo.Genres ON;
    INSERT dbo.Genres(genre_id,category_id,genre_name) VALUES
      (1,1,N'Vietnamese Literature'),(2,1,N'World Literature'),(3,1,N'Mystery and Detective'),
      (4,2,N'Popular Science'),(5,2,N'Environment'),(6,3,N'Programming'),
      (7,3,N'Artificial Intelligence'),(8,3,N'Databases'),(9,4,N'Management'),
      (10,4,N'Finance'),(11,5,N'Vietnamese History'),(12,5,N'World History'),
      (13,6,N'Personal Development'),(14,6,N'Psychology'),(15,7,N'Children Stories'),
      (16,8,N'English');
    SET IDENTITY_INSERT dbo.Genres OFF;

    SET IDENTITY_INSERT dbo.Authors ON;
    INSERT dbo.Authors(author_id,author_name) VALUES
      (1,N'Nam Cao'),(2,N'Nguyễn Nhật Ánh'),(3,N'Tô Hoài'),(4,N'Ngô Tất Tố'),
      (5,N'Arthur Conan Doyle'),(6,N'Agatha Christie'),(7,N'George Orwell'),(8,N'Paulo Coelho'),
      (9,N'Yuval Noah Harari'),(10,N'Stephen Hawking'),(11,N'Robert C. Martin'),(12,N'Martin Fowler'),
      (13,N'Andrew Hunt'),(14,N'Stuart Russell'),(15,N'Peter Norvig'),(16,N'Robert Kiyosaki'),
      (17,N'Dale Carnegie'),(18,N'Daniel Kahneman'),(19,N'James Clear'),(20,N'Carol S. Dweck'),
      (21,N'Charles Duhigg'),(22,N'Antoine de Saint-Exupéry'),(23,N'J. K. Rowling'),
      (24,N'Bill Bryson'),(25,N'Tim Marshall');
    SET IDENTITY_INSERT dbo.Authors OFF;

    SET IDENTITY_INSERT dbo.Shelves ON;
    INSERT dbo.Shelves(shelf_id,shelf_name,location) VALUES
      (1,N'Shelf A1',N'Floor 1 - Vietnamese Literature'),(2,N'Shelf A2',N'Floor 1 - World Literature'),
      (3,N'Shelf B1',N'Floor 1 - Science'),(4,N'Shelf C1',N'Floor 2 - Technology'),
      (5,N'Shelf C2',N'Floor 2 - Technology'),(6,N'Shelf D1',N'Floor 2 - Business and Economics'),
      (7,N'Shelf E1',N'Floor 3 - History'),(8,N'Shelf F1',N'Floor 3 - Life Skills'),
      (9,N'Shelf G1',N'Floor 3 - Children'),(10,N'Shelf H1',N'Floor 3 - Languages');
    SET IDENTITY_INSERT dbo.Shelves OFF;

    SET IDENTITY_INSERT dbo.Books ON;
    INSERT dbo.Books(book_id,genre_id,title,isbn,description,status,cover_image_url) VALUES
      (1,1,N'Chí Phèo','9786040000001',N'A Vietnamese realist classic about dignity, exclusion and rural society.','Active',NULL),
      (2,1,N'Mắt Biếc','9786040000002',N'A tender Vietnamese coming-of-age story about memory and unrequited love.','Active',NULL),
      (3,15,N'Dế Mèn Phiêu Lưu Ký','9786040000003',N'A beloved Vietnamese children story about adventure and growing up.','Active',NULL),
      (4,1,N'Tắt Đèn','9786040000004',N'A major work of Vietnamese social realism.','Active',NULL),
      (5,3,N'The Adventures of Sherlock Holmes','9786040000005',N'A classic collection of Sherlock Holmes detective stories.','Active',NULL),
      (6,3,N'Murder on the Orient Express','9786040000006',N'One of Hercule Poirot''s most celebrated mysteries.','Active',NULL),
      (7,2,N'1984','9786040000007',N'George Orwell''s landmark dystopian novel.','Active',NULL),
      (8,2,N'The Alchemist','9786040000008',N'A journey about dreams, purpose and personal destiny.','Active',NULL),
      (9,12,N'Sapiens: A Brief History of Humankind','9786040000009',N'An accessible survey of the history and development of humankind.','Active',NULL),
      (10,4,N'A Brief History of Time','9786040000010',N'An accessible introduction to cosmology and the universe.','Active',NULL),
      (11,6,N'Clean Code','9786040000011',N'Principles and practices for readable, maintainable source code.','Active',NULL),
      (12,6,N'Refactoring','9786040000012',N'Techniques for improving the design of existing code.','Active',NULL),
      (13,6,N'The Pragmatic Programmer','9786040000013',N'Practical thinking and habits for effective software development.','Active',NULL),
      (14,7,N'Artificial Intelligence: A Modern Approach','9786040000014',N'A foundational textbook on modern artificial intelligence.','Active',NULL),
      (15,8,N'Database System Concepts','9786040000015',N'Core concepts for database systems and data management.','Active',NULL),
      (16,10,N'Rich Dad Poor Dad','9786040000016',N'An introduction to personal-finance mindsets.','Active',NULL),
      (17,13,N'How to Win Friends and Influence People','9786040000017',N'Practical lessons on communication and relationships.','Active',NULL),
      (18,14,N'Thinking, Fast and Slow','9786040000018',N'A study of the two systems that shape human judgment.','Active',NULL),
      (19,13,N'Atomic Habits','9786040000019',N'A practical method for building small and sustainable habits.','Active',NULL),
      (20,14,N'Mindset','9786040000020',N'An exploration of the power of a growth mindset.','Active',NULL),
      (21,13,N'The Power of Habit','9786040000021',N'How habits form and how they can be changed.','Active',NULL),
      (22,15,N'The Little Prince','9786040000022',N'A timeless and humane story for readers of all ages.','Active',NULL),
      (23,15,N'Harry Potter and the Philosopher''s Stone','9786040000023',N'The beginning of Harry Potter''s journey at Hogwarts.','Active',NULL),
      (24,4,N'A Short History of Nearly Everything','9786040000024',N'A friendly tour through major ideas in natural science.','Active',NULL),
      (25,12,N'Prisoners of Geography','9786040000025',N'How geography shapes world politics and international relations.','Active',NULL),
      (26,5,N'The Future of Earth','9786040000026',N'Environmental challenges and paths toward sustainable development.','Active',NULL),
      (27,9,N'The Lean Startup','9786040000027',N'Rapid experimentation and validated learning for product development.','Active',NULL),
      (28,10,N'The Intelligent Investor','9786040000028',N'Principles of long-term value investing.','Active',NULL),
      (29,16,N'English Grammar in Use','9786040000029',N'A practical self-study reference for English grammar.','Active',NULL),
      (30,8,N'Designing Data-Intensive Applications','9786040000030',N'Designing reliable, scalable and maintainable data systems.','Active',NULL);
    SET IDENTITY_INSERT dbo.Books OFF;

    INSERT dbo.MembershipTierTranslations(tier_id,language_code,tier_name,benefits)
    SELECT tier_id,N'en',tier_name,benefits FROM dbo.MembershipTiers;
    INSERT dbo.MembershipTierTranslations(tier_id,language_code,tier_name,benefits) VALUES
      (1,N'vi',N'Thành viên',N'Được mượn đồng thời tối đa 5 cuốn.'),
      (2,N'vi',N'Bạc',N'Giảm 5% phí mượn; được mượn đồng thời tối đa 7 cuốn.'),
      (3,N'vi',N'Vàng',N'Giảm 10% phí mượn; được mượn đồng thời tối đa 10 cuốn.'),
      (4,N'vi',N'Kim cương',N'Giảm 15% phí mượn; được mượn đồng thời tối đa 15 cuốn.');

    INSERT dbo.BookAuthors(book_id,author_id) VALUES
      (1,1),(2,2),(3,3),(4,4),(5,5),(6,6),(7,7),(8,8),(9,9),(10,10),
      (11,11),(12,12),(13,13),(14,14),(14,15),(16,16),(17,17),(18,18),(19,19),
      (20,20),(21,21),(22,22),(23,23),(24,24),(25,25),(27,11),(28,16),(29,13),(30,12);

    /* Three physical copies for each title: 90 clean and traceable barcodes. */
    DECLARE @BookId INT = 1, @CopyNo INT;
    WHILE @BookId <= 30
    BEGIN
        SET @CopyNo = 1;
        WHILE @CopyNo <= 3
        BEGIN
            INSERT dbo.BookItems(book_id,shelf_id,barcode,status,book_condition,added_date)
            VALUES (
                @BookId,
                CASE
                  WHEN @BookId <= 4 THEN 1 WHEN @BookId <= 8 THEN 2 WHEN @BookId <= 10 THEN 3
                  WHEN @BookId <= 15 THEN 4 WHEN @BookId <= 16 THEN 6 WHEN @BookId <= 21 THEN 8
                  WHEN @BookId <= 23 THEN 9 WHEN @BookId <= 26 THEN 7 WHEN @BookId <= 28 THEN 6 ELSE 10 END,
                CONCAT('LMW-',RIGHT(CONCAT('0000',@BookId),4),'-',@CopyNo),
                'Available',N'Tốt',DATEADD(day,-(@BookId * 3 + @CopyNo),CONVERT(date,@Now))
            );
            SET @CopyNo += 1;
        END;
        SET @BookId += 1;
    END;

    UPDATE dbo.BookItems SET status='Borrowed' WHERE book_item_id IN (1,4,7,10,25,28);
    UPDATE dbo.BookItems SET status='Waiting_Pickup' WHERE book_item_id=19;
    UPDATE dbo.BookItems SET status='Damaged',book_condition=N'Hư hỏng',damage_note=N'Gáy sách bong nhẹ, đang chờ sửa.' WHERE book_item_id=89;
    UPDATE dbo.BookItems SET status='MinorDamaged',book_condition=N'Minor damage' WHERE book_item_id=90;

    SET IDENTITY_INSERT dbo.Borrows ON;
    INSERT dbo.Borrows(borrow_id,member_id,staff_id,borrow_date,status,rejection_code,rejection_reason) VALUES
      (1,1,2,DATEADD(day,-9,@Now),'Active',NULL,NULL),
      (2,2,2,DATEADD(day,-20,@Now),'Overdue',NULL,NULL),
      (3,3,3,DATEADD(day,-11,@Now),'Return_Pending',NULL,NULL),
      (4,4,2,DATEADD(day,-10,@Now),'Active',NULL,NULL),
      (5,5,3,DATEADD(day,-30,@Now),'Returned',NULL,NULL),
      (6,6,NULL,DATEADD(hour,-2,@Now),'Pending',NULL,NULL),
      (7,7,2,DATEADD(day,-1,@Now),'Waiting_Pickup',NULL,NULL),
      (8,8,2,DATEADD(day,-4,@Now),'Rejected','NO_COPY',N'Chưa có bản sách phù hợp tại thời điểm duyệt.'),
      (9,9,3,DATEADD(day,-7,@Now),'Active',NULL,NULL),
      (10,10,2,DATEADD(day,-40,@Now),'Returned',NULL,NULL),
      (11,11,NULL,DATEADD(day,-2,@Now),'Payment_Expired',NULL,NULL),
      (12,12,NULL,DATEADD(day,-2,@Now),'Canceled',NULL,NULL);
    SET IDENTITY_INSERT dbo.Borrows OFF;

    SET IDENTITY_INSERT dbo.BorrowDetails ON;
    INSERT dbo.BorrowDetails(borrow_detail_id,borrow_id,book_id,book_item_id,due_date,return_date,renew_count,status,condition_note,rejection_code,rejection_reason,condition_code) VALUES
      (1,1,1,1,DATEADD(day,5,@Now),NULL,0,'Borrowed',NULL,NULL,NULL,NULL),
      (2,2,2,4,DATEADD(day,-6,@Now),NULL,0,'Overdue',NULL,NULL,NULL,NULL),
      (3,3,3,7,DATEADD(day,3,@Now),NULL,0,'Return_Pending',NULL,NULL,NULL,NULL),
      (4,4,4,10,DATEADD(day,4,@Now),NULL,1,'Renew_Pending',NULL,NULL,NULL,NULL),
      (5,5,5,13,DATEADD(day,-16,@Now),DATEADD(day,-15,@Now),0,'Returned',N'Trả đúng tình trạng.',NULL,NULL,'GOOD'),
      (6,6,6,NULL,DATEADD(day,14,@Now),NULL,0,'Pending',NULL,NULL,NULL,NULL),
      (7,7,7,19,DATEADD(day,15,@Now),NULL,0,'Waiting_Pickup',NULL,NULL,NULL,NULL),
      (8,8,8,NULL,DATEADD(day,10,@Now),NULL,0,'Rejected',NULL,'NO_COPY',N'Không còn bản sách sẵn sàng.',NULL),
      (9,9,9,25,DATEADD(day,7,@Now),NULL,0,'Borrowed',NULL,NULL,NULL,NULL),
      (10,9,10,28,DATEADD(day,7,@Now),NULL,0,'Borrowed',NULL,NULL,NULL,NULL),
      (11,10,11,31,DATEADD(day,-26,@Now),DATEADD(day,-27,@Now),1,'Returned',N'Sách còn tốt.',NULL,NULL,'GOOD'),
      (12,11,12,NULL,DATEADD(day,12,@Now),NULL,0,'Cancelled',NULL,NULL,NULL,NULL),
      (13,12,13,NULL,DATEADD(day,12,@Now),NULL,0,'Canceled',NULL,NULL,NULL,NULL);
    SET IDENTITY_INSERT dbo.BorrowDetails OFF;

    INSERT dbo.Reservations(member_id,book_id,reservation_date,status,rejection_code,rejection_reason) VALUES
      (1,14,DATEADD(day,-2,@Now),'Pending',NULL,NULL),
      (2,15,DATEADD(day,-5,@Now),'Deposit_Paid',NULL,NULL),
      (3,16,DATEADD(day,-10,@Now),'Completed',NULL,NULL),
      (4,17,DATEADD(day,-1,@Now),'Ready',NULL,NULL),
      (5,18,DATEADD(day,-8,@Now),'Refund_Pending',NULL,NULL),
      (6,19,DATEADD(day,-12,@Now),'Refunded',NULL,NULL),
      (7,20,DATEADD(day,-3,@Now),'Rejected','COPY_AVAILABLE',N'Sách hiện có bản sẵn sàng để mượn trực tiếp.'),
      (8,21,DATEADD(day,-15,@Now),'Canceled',NULL,NULL),
      (9,22,DATEADD(hour,-6,@Now),'Active',NULL,NULL),
      (10,23,DATEADD(day,-4,@Now),'Pending',NULL,NULL);

    INSERT dbo.Favorites(member_id,book_id) VALUES
      (1,8),(1,11),(1,19),(2,5),(2,6),(3,3),(3,22),(4,14),(4,30),(5,9),
      (5,18),(6,17),(7,23),(8,2),(8,21),(9,10),(10,15),(11,24),(12,29),(13,27),
      (14,4),(15,12),(16,25),(17,1),(18,16);

    INSERT dbo.Feedbacks(member_id,book_id,rating,comment,created_date,status,librarian_response,response_date) VALUES
      (1,8,5,N'Câu chuyện truyền cảm hứng và dễ đọc.',DATEADD(day,-20,@Now),'APPROVED',N'Cảm ơn bạn đã chia sẻ.',DATEADD(day,-19,@Now)),
      (2,5,5,N'Cốt truyện trinh thám hấp dẫn.',DATEADD(day,-18,@Now),'APPROVED',NULL,NULL),
      (3,3,4,N'Phù hợp với nhiều lứa tuổi.',DATEADD(day,-16,@Now),'APPROVED',NULL,NULL),
      (4,11,5,N'Rất hữu ích cho sinh viên lập trình.',DATEADD(day,-15,@Now),'APPROVED',N'Chúc bạn áp dụng tốt vào dự án.',DATEADD(day,-14,@Now)),
      (5,9,4,N'Nhiều kiến thức thú vị.',DATEADD(day,-13,@Now),'APPROVED',NULL,NULL),
      (6,17,5,N'Nội dung thực tế và dễ áp dụng.',DATEADD(day,-11,@Now),'APPROVED',NULL,NULL),
      (7,19,4,N'Ví dụ rõ ràng, trình bày mạch lạc.',DATEADD(day,-9,@Now),'APPROVED',NULL,NULL),
      (8,2,5,N'Một câu chuyện đẹp và nhiều cảm xúc.',DATEADD(day,-8,@Now),'APPROVED',NULL,NULL),
      (9,10,4,N'Khó nhưng đáng đọc.',DATEADD(day,-6,@Now),'PENDING',NULL,NULL),
      (10,15,5,N'Giáo trình cơ sở dữ liệu rất đầy đủ.',DATEADD(day,-4,@Now),'PENDING',NULL,NULL),
      (11,22,5,N'Ngắn gọn nhưng sâu sắc.',DATEADD(day,-3,@Now),'APPROVED',NULL,NULL),
      (12,29,4,N'Bài tập phong phú.',DATEADD(day,-1,@Now),'PENDING',NULL,NULL);

    INSERT dbo.BookAcquisitionRequests(member_id,title,created_date,author,isbn,publisher,publication_year,request_reason,reference_url,status,decision_note,processed_date) VALUES
      (1,N'Fundamentals of Software Architecture',DATEADD(day,-12,@Now),N'Mark Richards',NULL,N'O''Reilly',2020,N'Phục vụ môn kiến trúc phần mềm.',NULL,N'APPROVED',N'Đã đưa vào kế hoạch mua quý tới.',DATEADD(day,-10,@Now)),
      (2,N'Head First Design Patterns',DATEADD(day,-9,@Now),N'Eric Freeman',NULL,N'O''Reilly',2020,N'Bổ sung tài liệu design pattern.',NULL,N'PENDING',NULL,NULL),
      (3,N'The Psychology of Money',DATEADD(day,-8,@Now),N'Morgan Housel',NULL,NULL,2020,N'Tài liệu tài chính cá nhân dễ tiếp cận.',NULL,N'APPROVED',N'Đề xuất phù hợp danh mục.',DATEADD(day,-7,@Now)),
      (4,N'Computer Networks',DATEADD(day,-7,@Now),N'Andrew S. Tanenbaum',NULL,N'Pearson',2021,N'Phục vụ môn mạng máy tính.',NULL,N'PENDING',NULL,NULL),
      (5,N'Deep Learning',DATEADD(day,-6,@Now),N'Ian Goodfellow',NULL,N'MIT Press',2016,N'Tài liệu nghiên cứu AI.',NULL,N'PENDING',NULL,NULL),
      (6,N'Một tựa sách không xác định',DATEADD(day,-5,@Now),NULL,NULL,NULL,NULL,N'Không có đủ thông tin.',NULL,N'REJECTED',N'Vui lòng bổ sung tác giả hoặc ISBN.',DATEADD(day,-4,@Now)),
      (7,N'Effective Java',DATEADD(day,-3,@Now),N'Joshua Bloch',NULL,N'Addison-Wesley',2018,N'Nâng cao kỹ năng Java.',NULL,N'APPROVED',N'Đề xuất phù hợp nhu cầu sinh viên.',DATEADD(day,-2,@Now)),
      (8,N'Fluent Python',DATEADD(day,-1,@Now),N'Luciano Ramalho',NULL,N'O''Reilly',2022,N'Bổ sung tài liệu Python nâng cao.',NULL,N'PENDING',NULL,NULL);

    INSERT dbo.SystemSettings(setting_key,setting_value,description) VALUES
      ('Fine_Per_Day',N'5000',N'Overdue fine per book per day (VND).'),
      ('Max_Borrow_Days',N'14',N'Standard borrowing period in days.'),
      ('Max_Books_Per_Member',N'5',N'Maximum books in one borrowing request.'),
      ('BORROW_FEE_PER_BOOK',N'5000',N'Borrowing fee per book (VND).'),
      ('Deposit_Amount',N'50000',N'Reservation deposit per book (VND).'),
      ('MAX_RENEWALS',N'2',N'Maximum renewals allowed per borrowed copy.'),
      ('RENEW_DAYS',N'7',N'Days added after an approved renewal.'),
      ('Max_Renewal_Days',N'14',N'Maximum days in one renewal request.'),
      ('RENEWAL_FEE_PER_DAY',N'1000',N'Renewal fee per additional day (VND).'),
      ('Damage_Compensation_Amount',N'120000',N'Default compensation for lost or severely damaged books.'),
      ('Damage_Compensation_Threshold',N'3',N'Condition threshold that triggers compensation.'),
      ('Overdue_Violation_Lock_Limit',N'3',N'Overdue violations before account restrictions apply.'),
      ('MIN_TOP_UP',N'10000',N'Minimum wallet top-up amount (VND).');

    SET IDENTITY_INSERT dbo.Transactions ON;
    INSERT dbo.Transactions(transaction_id,wallet_id,borrow_id,transaction_type,amount,transaction_date,status,borrow_detail_id,renewal_days,reference_code,performed_by_staff_id,channel,balance_before,balance_after) VALUES
      (1,1,NULL,'TOP_UP',100000,DATEADD(day,-15,@Now),'Completed',NULL,NULL,'DEMO-TOPUP-0001',2,'CASH',200000,300000),
      (2,1,1,'BORROW_FEE',-5000,DATEADD(day,-9,@Now),'Completed',1,NULL,'DEMO-BORROW-0001',2,'WALLET',300000,295000),
      (3,2,2,'BORROW_FEE',-5000,DATEADD(day,-20,@Now),'Completed',2,NULL,'DEMO-BORROW-0002',2,'WALLET',185000,180000),
      (5,3,3,'BORROW_FEE',-5000,DATEADD(day,-11,@Now),'Completed',3,NULL,'DEMO-BORROW-0003',3,'WALLET',95000,90000),
      (6,4,4,'BORROW_FEE',-5000,DATEADD(day,-10,@Now),'Completed',4,NULL,'DEMO-BORROW-0004',2,'WALLET',155000,150000),
      (7,4,4,'RENEWAL_FEE',-10000,DATEADD(hour,-5,@Now),'Pending',4,10,'DEMO-RENEW-0001',NULL,'WALLET',150000,140000),
      (8,5,5,'DEPOSIT',-50000,DATEADD(day,-35,@Now),'Completed',5,NULL,'DEMO-DEPOSIT-0001',NULL,'WALLET',150000,100000),
      (9,5,5,'REFUND',50000,DATEADD(day,-15,@Now),'Completed',5,NULL,'DEMO-REFUND-0001',3,'WALLET',100000,150000),
      (10,7,7,'BORROW_FEE',-5000,DATEADD(day,-1,@Now),'Completed',7,NULL,'DEMO-BORROW-0007',2,'CASH',NULL,NULL),
      (11,9,9,'BORROW_FEE',-10000,DATEADD(day,-7,@Now),'Completed',9,NULL,'DEMO-BORROW-0009',3,'WALLET',220000,210000),
      (12,10,10,'BORROW_FEE',-5000,DATEADD(day,-40,@Now),'Completed',11,NULL,'DEMO-BORROW-0010',2,'CASH',NULL,NULL),
      (13,6,NULL,'TOP_UP',50000,DATEADD(day,-3,@Now),'Completed',NULL,NULL,'DEMO-TOPUP-0006',NULL,'PAYOS',25000,75000),
      (14,8,NULL,'TOP_UP',150000,DATEADD(day,-2,@Now),'Completed',NULL,NULL,'DEMO-TOPUP-0008',2,'CASH',200000,350000),
      (15,11,11,'BORROW_FEE',-5000,DATEADD(day,-2,@Now),'Expired',12,NULL,'DEMO-BORROW-0011',NULL,'PAYOS',NULL,NULL),
      (16,12,NULL,'DEPOSIT',-50000,DATEADD(day,-5,@Now),'Completed',NULL,NULL,'DEMO-DEPOSIT-0012',NULL,'WALLET',175000,125000),
      (17,14,NULL,'TOP_UP',200000,DATEADD(day,-8,@Now),'Completed',NULL,NULL,'DEMO-TOPUP-0014',NULL,'PAYOS',220000,420000),
      (19,18,NULL,'TOP_UP',75000,DATEADD(day,-4,@Now),'Completed',NULL,NULL,'DEMO-TOPUP-0018',NULL,'PAYOS',150000,225000);
    SET IDENTITY_INSERT dbo.Transactions OFF;

    SET IDENTITY_INSERT dbo.PayOSPayments ON;
    INSERT dbo.PayOSPayments(payment_id,member_id,transaction_id,purpose,reference_id,amount,order_code,payment_link_id,checkout_url,qr_code,status,bank_reference,created_at,paid_at) VALUES
      (1,6,13,'TOP_UP',NULL,50000,900000000000001,'demo-link-0001',NULL,NULL,'PAID','DEMO-BANK-0001',DATEADD(day,-3,@Now),DATEADD(day,-3,DATEADD(minute,2,@Now))),
      (2,14,17,'TOP_UP',NULL,200000,900000000000002,'demo-link-0002',NULL,NULL,'PAID','DEMO-BANK-0002',DATEADD(day,-8,@Now),DATEADD(day,-8,DATEADD(minute,3,@Now))),
      (3,11,NULL,'BORROW_FEE',11,5000,900000000000003,'demo-link-0003',NULL,NULL,'EXPIRED',NULL,DATEADD(day,-2,@Now),NULL),
      (5,8,NULL,'TOP_UP',NULL,100000,900000000000005,'demo-link-0005',NULL,NULL,'EXPIRED',NULL,DATEADD(day,-4,@Now),NULL),
      (6,9,NULL,'DEPOSIT',9,50000,900000000000006,'demo-link-0006',NULL,NULL,'CANCELLED',NULL,DATEADD(day,-2,@Now),NULL);
    SET IDENTITY_INSERT dbo.PayOSPayments OFF;

    INSERT dbo.PayOSPaymentAuditLogs(payment_id,actor_user_id,event_type,source,old_status,new_status,successful,message,created_at) VALUES
      (1,9,'PAYMENT_CREATED','APPLICATION',NULL,'PENDING',1,N'Tạo yêu cầu nạp ví demo.',DATEADD(day,-3,@Now)),
      (1,NULL,'WEBHOOK_RECEIVED','PAYOS','PENDING','PAID',1,N'Webhook demo được xác minh thành công.',DATEADD(day,-3,DATEADD(minute,2,@Now))),
      (2,17,'PAYMENT_CREATED','APPLICATION',NULL,'PENDING',1,N'Tạo yêu cầu nạp ví demo.',DATEADD(day,-8,@Now)),
      (2,NULL,'WEBHOOK_RECEIVED','PAYOS','PENDING','PAID',1,N'Webhook demo được xác minh thành công.',DATEADD(day,-8,DATEADD(minute,3,@Now))),
      (3,14,'PAYMENT_EXPIRED','SYSTEM','PENDING','EXPIRED',1,N'Thanh toán phí mượn demo đã hết hạn.',DATEADD(day,-1,@Now)),
      (5,11,'PAYMENT_EXPIRED','SYSTEM','PENDING','EXPIRED',1,N'Liên kết demo đã hết hạn.',DATEADD(day,-3,@Now)),
      (6,12,'PAYMENT_CANCELLED','MEMBER','PENDING','CANCELLED',1,N'Thành viên hủy thanh toán demo.',DATEADD(day,-2,DATEADD(minute,4,@Now)));

    INSERT dbo.PayOSReconciliationIssues(payment_id,status,attempt_count,first_seen_at,last_attempt_at,resolved_at,error_message) VALUES
      (5,'RESOLVED',2,DATEADD(day,-3,@Now),DATEADD(day,-2,@Now),DATEADD(day,-2,@Now),N'Đã đồng bộ trạng thái hết hạn từ dữ liệu demo.');

    SET IDENTITY_INSERT dbo.Notifications ON;
    INSERT dbo.Notifications(notification_id,staff_id,title,content,created_date,status,notification_type,notification_source,event_type,title_key,content_key,message_arguments) VALUES
      (1,NULL,N'Welcome to the library',N'Your demonstration account is ready.',DATEADD(day,-30,@Now),'Active',N'GENERAL',N'SYSTEM',N'GENERAL',NULL,NULL,NULL),
      (2,2,N'Borrow request approved',N'Please collect your books at the desk within the allowed period.',DATEADD(day,-1,@Now),'Active',N'LOAN',N'LIBRARIAN',N'LOAN_APPROVED',NULL,NULL,NULL),
      (3,NULL,N'Return reminder',N'One of your books is approaching its due date.',DATEADD(day,-1,@Now),'Active',N'REMINDER',N'SYSTEM',N'OVERDUE_REMINDER',NULL,NULL,NULL),
      (4,NULL,N'Overdue fine recorded',N'The system recorded a demonstration overdue fine.',DATEADD(hour,-3,@Now),'Active',N'FINANCE',N'SYSTEM',N'OVERDUE_FINE_CREATED',NULL,NULL,NULL),
      (5,3,N'Return request received',N'Your return request was received at the library desk.',DATEADD(hour,-2,@Now),'Active',N'LOAN',N'LIBRARIAN',N'RETURN_CONFIRMED',NULL,NULL,NULL),
      (6,2,N'Reservation ready',N'Your reserved book is ready for collection.',DATEADD(hour,-6,@Now),'Active',N'RESERVATION',N'LIBRARIAN',N'RESERVATION_APPROVED',NULL,NULL,NULL),
      (7,NULL,N'Wallet top-up successful',N'The demonstration wallet top-up was completed.',DATEADD(day,-3,@Now),'Active',N'FINANCE',N'SYSTEM',N'TOP_UP_SUCCESS',NULL,NULL,NULL),
      (8,2,N'Review response',N'A librarian responded to your review.',DATEADD(day,-14,@Now),'Active',N'REVIEW',N'LIBRARIAN',N'REVIEW_REPLIED',NULL,NULL,NULL),
      (9,2,N'Book suggestion approved',N'Your suggestion was added to the acquisition plan.',DATEADD(day,-10,@Now),'Active',N'ACQUISITION',N'LIBRARIAN',N'ACQUISITION_APPROVED',NULL,NULL,NULL),
      (10,NULL,N'Scheduled maintenance',N'The demonstration system will be maintained this weekend.',DATEADD(hour,-12,@Now),'Active',N'MAINTENANCE',N'SYSTEM',N'GENERAL',NULL,NULL,NULL),
      (11,3,N'Renewal approved',N'Your book renewal request was approved.',DATEADD(day,-2,@Now),'Active',N'LOAN',N'LIBRARIAN',N'RENEWAL_APPROVED',NULL,NULL,NULL),
      (12,NULL,N'Reservation deposit refunded',N'The reservation deposit was returned to your wallet.',DATEADD(day,-1,@Now),'Active',N'RESERVATION',N'SYSTEM',N'RESERVATION_REFUNDED',NULL,NULL,NULL);
    SET IDENTITY_INSERT dbo.Notifications OFF;

    INSERT dbo.MemberNotifications(member_id,notification_id,is_read,read_date) VALUES
      (1,1,1,DATEADD(day,-29,@Now)),(1,2,0,NULL),(1,3,0,NULL),(2,1,1,DATEADD(day,-28,@Now)),
      (2,4,0,NULL),(3,5,0,NULL),(4,6,1,DATEADD(hour,-5,@Now)),(5,7,1,DATEADD(day,-3,@Now)),
      (6,8,0,NULL),(7,9,0,NULL),(8,10,0,NULL),(9,11,1,DATEADD(day,-2,@Now)),
      (10,12,0,NULL),(11,3,0,NULL),(12,10,1,DATEADD(hour,-10,@Now)),(13,10,0,NULL),
      (14,10,0,NULL),(15,10,0,NULL),(16,10,0,NULL),(17,10,0,NULL),(18,10,0,NULL);

    INSERT dbo.SystemLogs(user_id,action_type,ip_address,user_agent,description,created_at) VALUES
      (1,'LOGIN','127.0.0.1',N'Demo Browser',N'Quản trị viên đăng nhập.',DATEADD(day,-2,@Now)),
      (2,'CREATE_BOOK','127.0.0.1',N'Demo Browser',N'Tạo dữ liệu sách demo.',DATEADD(day,-2,@Now)),
      (4,'REQUEST_BORROW','127.0.0.1',N'Demo Browser',N'Thành viên gửi yêu cầu mượn.',DATEADD(day,-9,@Now)),
      (6,'REQUEST_RETURN','127.0.0.1',N'Demo Browser',N'Thành viên gửi yêu cầu trả.',DATEADD(hour,-2,@Now)),
      (7,'RESERVE_BOOK','127.0.0.1',N'Demo Browser',N'Thành viên đặt trước sách.',DATEADD(day,-1,@Now)),
      (3,'UPDATE_SETTINGS','127.0.0.1',N'Demo Browser',N'Kiểm tra cấu hình nghiệp vụ.',DATEADD(day,-1,@Now));

    INSERT dbo.DemoSeedHistory(seed_key) VALUES (@SeedKey);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* Fast post-seed sanity report. */
SELECT N'Users' AS entity_name, COUNT(*) AS row_count FROM dbo.Users
UNION ALL SELECT N'Members',COUNT(*) FROM dbo.Members
UNION ALL SELECT N'Books',COUNT(*) FROM dbo.Books
UNION ALL SELECT N'BookItems',COUNT(*) FROM dbo.BookItems
UNION ALL SELECT N'Borrows',COUNT(*) FROM dbo.Borrows
UNION ALL SELECT N'BorrowDetails',COUNT(*) FROM dbo.BorrowDetails
UNION ALL SELECT N'Transactions',COUNT(*) FROM dbo.Transactions
UNION ALL SELECT N'Notifications',COUNT(*) FROM dbo.Notifications;
GO

