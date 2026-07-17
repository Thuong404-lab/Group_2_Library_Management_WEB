/*
 * Extended test data for Library Management Web.
 * Run this script after database/LMW_DB_17_07.sql.
 * The script is idempotent and can be executed more than once.
 */

IF DB_ID(N'tes') IS NULL
    THROW 50001, 'Database [tes] does not exist. Run LMW_DB_17_07.sql first.', 1;
GO

USE [tes];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    /* Reference data */
    INSERT INTO dbo.Categories (category_name)
    SELECT seed.category_name
    FROM (VALUES
        (N'Thiếu nhi'),
        (N'Ngoại ngữ'),
        (N'Y học - Sức khỏe')
    ) AS seed(category_name)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Categories c WHERE c.category_name = seed.category_name
    );

    INSERT INTO dbo.Genres (category_id, genre_name)
    SELECT c.category_id, seed.genre_name
    FROM (VALUES
        (N'Khoa học - Công nghệ', N'Lập trình'),
        (N'Khoa học - Công nghệ', N'Trí tuệ nhân tạo'),
        (N'Kinh tế - Tài chính', N'Quản trị kinh doanh'),
        (N'Thiếu nhi', N'Văn học thiếu nhi'),
        (N'Ngoại ngữ', N'Tiếng Anh'),
        (N'Y học - Sức khỏe', N'Dinh dưỡng')
    ) AS seed(category_name, genre_name)
    JOIN dbo.Categories c ON c.category_name = seed.category_name
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Genres g WHERE g.genre_name = seed.genre_name
    );

    INSERT INTO dbo.Shelves (shelf_name, location)
    SELECT seed.shelf_name, seed.location
    FROM (VALUES
        (N'Kệ B2 - Lập trình', N'Tầng 2 - Khu B'),
        (N'Kệ C2 - Kinh doanh', N'Tầng 3 - Khu C'),
        (N'Kệ E1 - Thiếu nhi', N'Tầng 1 - Khu E'),
        (N'Kệ E2 - Ngoại ngữ', N'Tầng 2 - Khu E'),
        (N'Kệ F1 - Sức khỏe', N'Tầng 3 - Khu F')
    ) AS seed(shelf_name, location)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Shelves s WHERE s.shelf_name = seed.shelf_name
    );

    INSERT INTO dbo.Authors (author_name)
    SELECT seed.author_name
    FROM (VALUES
        (N'Andrew Hunt'), (N'David Thomas'), (N'Martin Fowler'),
        (N'Erich Gamma'), (N'Richard Helm'), (N'Ralph Johnson'),
        (N'John Vlissides'), (N'James Clear'), (N'Daniel Kahneman'),
        (N'Morgan Housel'), (N'Eric Ries'), (N'Robin Sharma'),
        (N'Antoine de Saint-Exupéry'), (N'Hector Malot'), (N'Haruki Murakami'),
        (N'Agatha Christie'), (N'Arthur Conan Doyle'), (N'Stephen Hawking'),
        (N'Nguyễn Ngọc Tư'), (N'Bảo Ninh'), (N'Cal Newport'),
        (N'Raymond Murphy')
    ) AS seed(author_name)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Authors a WHERE a.author_name = seed.author_name
    );

    /* Catalog: 20 books */
    INSERT INTO dbo.Books (genre_id, title, isbn, description, status, cover_image_url)
    SELECT g.genre_id, seed.title, seed.isbn, seed.description, seed.status, NULL
    FROM (VALUES
        (N'The Pragmatic Programmer', '9786044001000', N'Kinh nghiệm thực tiễn để phát triển phần mềm bền vững.', 'Active', N'Lập trình'),
        (N'Refactoring', '9786044001001', N'Các kỹ thuật cải tiến thiết kế mã nguồn hiện có.', 'Active', N'Lập trình'),
        (N'Design Patterns', '9786044001002', N'Các mẫu thiết kế nền tảng trong lập trình hướng đối tượng.', 'Active', N'Công nghệ thông tin'),
        (N'Atomic Habits', '9786044001003', N'Phương pháp xây dựng thói quen tốt bằng những thay đổi nhỏ.', 'Active', N'Phát triển bản thân'),
        (N'Tư Duy Nhanh Và Chậm', '9786044001004', N'Khám phá hai hệ thống tư duy chi phối quyết định của con người.', 'Active', N'Tâm lý học'),
        (N'Tâm Lý Học Về Tiền', '9786044001005', N'Những bài học thực tế về hành vi tài chính cá nhân.', 'Active', N'Đầu tư tài chính'),
        (N'Khởi Nghiệp Tinh Gọn', '9786044001006', N'Phương pháp kiểm chứng ý tưởng và phát triển sản phẩm hiệu quả.', 'Active', N'Kinh doanh khởi nghiệp'),
        (N'Nhà Lãnh Đạo Không Chức Danh', '9786044001007', N'Bài học về năng lực lãnh đạo từ mọi vị trí.', 'Active', N'Phát triển bản thân'),
        (N'Hoàng Tử Bé', '9786044001008', N'Câu chuyện giàu tính nhân văn dành cho mọi lứa tuổi.', 'Active', N'Văn học thiếu nhi'),
        (N'Không Gia Đình', '9786044001009', N'Hành trình trưởng thành đầy nghị lực của cậu bé Rémi.', 'Active', N'Văn học thiếu nhi'),
        (N'Rừng Na Uy', '9786044001010', N'Tiểu thuyết về tuổi trẻ, tình yêu và mất mát.', 'Active', N'Tiểu thuyết văn học'),
        (N'Án Mạng Trên Chuyến Tàu Tốc Hành Phương Đông', '9786044001011', N'Vụ án nổi tiếng của thám tử Hercule Poirot.', 'Active', N'Trinh thám'),
        (N'Chú Chó Của Dòng Họ Baskerville', '9786044001012', N'Một trong những vụ án bí ẩn nhất của Sherlock Holmes.', 'Active', N'Trinh thám'),
        (N'Lược Sử Thời Gian', '9786044001013', N'Giới thiệu dễ tiếp cận về vũ trụ, thời gian và hố đen.', 'Active', N'Khoa học vũ trụ'),
        (N'Cánh Đồng Bất Tận', '9786044001014', N'Tập truyện về con người và miền sông nước Nam Bộ.', 'Active', N'Truyện ngắn'),
        (N'Nỗi Buồn Chiến Tranh', '9786044001015', N'Tác phẩm về ký ức, chiến tranh và thân phận con người.', 'Active', N'Lịch sử Việt Nam'),
        (N'Sống Mòn', '9786044001016', N'Tác phẩm hiện thực sâu sắc của nhà văn Nam Cao.', 'Active', N'Tiểu thuyết văn học'),
        (N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', '9786044001017', N'Câu chuyện trong trẻo về ký ức tuổi thơ.', 'Active', N'Văn học thiếu nhi'),
        (N'Deep Work', '9786044001018', N'Phương pháp tập trung sâu để tạo ra kết quả có giá trị.', 'Active', N'Phát triển bản thân'),
        (N'English Grammar in Use', '9786044001019', N'Tài liệu ngữ pháp tiếng Anh thực hành.', 'Active', N'Tiếng Anh')
    ) AS seed(title, isbn, description, status, genre_name)
    JOIN dbo.Genres g ON g.genre_name = seed.genre_name
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Books b WHERE b.isbn = seed.isbn
    );

    /* Book-author relationships */
    INSERT INTO dbo.BookAuthors (book_id, author_id)
    SELECT b.book_id, a.author_id
    FROM (VALUES
        (N'The Pragmatic Programmer', N'Andrew Hunt'),
        (N'The Pragmatic Programmer', N'David Thomas'),
        (N'Refactoring', N'Martin Fowler'),
        (N'Design Patterns', N'Erich Gamma'),
        (N'Design Patterns', N'Richard Helm'),
        (N'Design Patterns', N'Ralph Johnson'),
        (N'Design Patterns', N'John Vlissides'),
        (N'Atomic Habits', N'James Clear'),
        (N'Tư Duy Nhanh Và Chậm', N'Daniel Kahneman'),
        (N'Tâm Lý Học Về Tiền', N'Morgan Housel'),
        (N'Khởi Nghiệp Tinh Gọn', N'Eric Ries'),
        (N'Nhà Lãnh Đạo Không Chức Danh', N'Robin Sharma'),
        (N'Hoàng Tử Bé', N'Antoine de Saint-Exupéry'),
        (N'Không Gia Đình', N'Hector Malot'),
        (N'Rừng Na Uy', N'Haruki Murakami'),
        (N'Án Mạng Trên Chuyến Tàu Tốc Hành Phương Đông', N'Agatha Christie'),
        (N'Chú Chó Của Dòng Họ Baskerville', N'Arthur Conan Doyle'),
        (N'Lược Sử Thời Gian', N'Stephen Hawking'),
        (N'Cánh Đồng Bất Tận', N'Nguyễn Ngọc Tư'),
        (N'Nỗi Buồn Chiến Tranh', N'Bảo Ninh'),
        (N'Sống Mòn', N'Nam Cao'),
        (N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', N'Nguyễn Nhật Ánh'),
        (N'Deep Work', N'Cal Newport'),
        (N'English Grammar in Use', N'Raymond Murphy')
    ) AS seed(title, author_name)
    JOIN dbo.Books b ON b.title = seed.title
    JOIN dbo.Authors a ON a.author_name = seed.author_name
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.BookAuthors ba
        WHERE ba.book_id = b.book_id AND ba.author_id = a.author_id
    );

    /* Three physical copies per new book (60 items) */
    INSERT INTO dbo.BookItems (book_id, shelf_id, barcode, status, book_condition, damage_note)
    SELECT b.book_id,
           s.shelf_id,
           CONCAT('TST', seed.book_code, '-', RIGHT(CONCAT('0', copy_no.value), 2)),
           CASE CONCAT(seed.book_code, '-', RIGHT(CONCAT('0', copy_no.value), 2))
               WHEN '019-03' THEN 'Damaged'
               WHEN '020-01' THEN 'Borrowed'
               WHEN '021-01' THEN 'Borrowed'
               WHEN '023-01' THEN 'Borrowed'
               WHEN '024-01' THEN 'Borrowed'
               WHEN '025-01' THEN 'Payment_Pending'
               WHEN '029-03' THEN 'Disposed'
               WHEN '030-03' THEN 'Lost'
               WHEN '031-01' THEN 'Borrowed'
               WHEN '032-01' THEN 'Borrowed'
               ELSE 'Available'
           END,
           CASE CONCAT(seed.book_code, '-', RIGHT(CONCAT('0', copy_no.value), 2))
               WHEN '019-03' THEN N'Hư hỏng'
               WHEN '029-03' THEN N'Không thể sử dụng'
               WHEN '030-03' THEN N'Thất lạc'
               ELSE N'Mới'
           END,
           CASE CONCAT(seed.book_code, '-', RIGHT(CONCAT('0', copy_no.value), 2))
               WHEN '019-03' THEN 'Rach bia va uot trang'
               WHEN '029-03' THEN 'Sach cu, hong gay'
               WHEN '030-03' THEN 'That lac khi kiem ke'
               ELSE NULL
           END
    FROM (VALUES
        (N'The Pragmatic Programmer', '018', N'Kệ B2 - Lập trình'),
        (N'Refactoring', '019', N'Kệ B2 - Lập trình'),
        (N'Design Patterns', '020', N'Kệ B2 - Lập trình'),
        (N'Atomic Habits', '021', N'Kệ C1 - Kỹ năng'),
        (N'Tư Duy Nhanh Và Chậm', '022', N'Kệ C1 - Kỹ năng'),
        (N'Tâm Lý Học Về Tiền', '023', N'Kệ C2 - Kinh doanh'),
        (N'Khởi Nghiệp Tinh Gọn', '024', N'Kệ C2 - Kinh doanh'),
        (N'Nhà Lãnh Đạo Không Chức Danh', '025', N'Kệ C1 - Kỹ năng'),
        (N'Hoàng Tử Bé', '026', N'Kệ E1 - Thiếu nhi'),
        (N'Không Gia Đình', '027', N'Kệ E1 - Thiếu nhi'),
        (N'Rừng Na Uy', '028', N'Kệ A2 - Văn học NN'),
        (N'Án Mạng Trên Chuyến Tàu Tốc Hành Phương Đông', '029', N'Kệ D1 - Trinh thám'),
        (N'Chú Chó Của Dòng Họ Baskerville', '030', N'Kệ D1 - Trinh thám'),
        (N'Lược Sử Thời Gian', '031', N'Kệ A2 - Văn học NN'),
        (N'Cánh Đồng Bất Tận', '032', N'Kệ A1 - Văn học VN'),
        (N'Nỗi Buồn Chiến Tranh', '033', N'Kệ A1 - Văn học VN'),
        (N'Sống Mòn', '034', N'Kệ A1 - Văn học VN'),
        (N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', '035', N'Kệ E1 - Thiếu nhi'),
        (N'Deep Work', '036', N'Kệ C1 - Kỹ năng'),
        (N'English Grammar in Use', '037', N'Kệ E2 - Ngoại ngữ')
    ) AS seed(title, book_code, shelf_name)
    CROSS JOIN (VALUES (1), (2), (3)) AS copy_no(value)
    JOIN dbo.Books b ON b.title = seed.title
    JOIN dbo.Shelves s ON s.shelf_name = seed.shelf_name
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.BookItems bi
        WHERE bi.barcode = CONCAT('TST', seed.book_code, '-', RIGHT(CONCAT('0', copy_no.value), 2))
    );

    /* Borrowing scenarios: returned, active, overdue, pending and payment pending */
    INSERT INTO dbo.Borrows (member_id, staff_id, borrow_date, status)
    SELECT seed.member_id, librarian.staff_id,
           CONVERT(datetime, seed.borrow_date, 126), seed.status
    FROM (VALUES
        (1,  '2026-05-01T09:00:00', 'Returned'),
        (2,  '2026-05-03T10:00:00', 'Returned'),
        (3,  '2026-07-10T08:30:00', 'Active'),
        (4,  '2026-06-10T14:00:00', 'Overdue'),
        (5,  '2026-07-15T09:15:00', 'Pending'),
        (6,  '2026-07-01T11:00:00', 'Active'),
        (7,  '2026-07-02T13:30:00', 'Active'),
        (8,  '2026-04-10T15:00:00', 'Returned'),
        (9,  '2026-03-05T08:00:00', 'Returned'),
        (10, '2026-07-16T16:00:00', 'Payment_Pending'),
        (11, '2026-06-01T09:45:00', 'Overdue'),
        (12, '2026-07-08T10:30:00', 'Active')
    ) AS seed(member_id, borrow_date, status)
    CROSS APPLY (
        SELECT TOP (1) st.staff_id
        FROM dbo.Staff st
        WHERE st.staff_type = 'Librarian'
        ORDER BY st.staff_id
    ) librarian
    WHERE EXISTS (SELECT 1 FROM dbo.Members m WHERE m.member_id = seed.member_id)
      AND NOT EXISTS (
        SELECT 1 FROM dbo.Borrows br
        WHERE br.member_id = seed.member_id
          AND br.borrow_date = CONVERT(datetime, seed.borrow_date, 126)
    );

    INSERT INTO dbo.BorrowDetails
        (borrow_id, book_id, book_item_id, due_date, return_date, renew_count, status, condition_note)
    SELECT br.borrow_id, b.book_id, bi.book_item_id,
           CONVERT(datetime, seed.due_date, 126),
           CASE WHEN seed.return_date IS NULL THEN NULL
                ELSE CONVERT(datetime, seed.return_date, 126) END,
           seed.renew_count, seed.detail_status, seed.condition_note
    FROM (VALUES
        (1,  '2026-05-01T09:00:00', N'The Pragmatic Programmer', 'TST018-01', '2026-05-15T09:00:00', '2026-05-14T10:00:00', 0, 'Returned', N'Tốt - Sách nguyên vẹn'),
        (2,  '2026-05-03T10:00:00', N'Refactoring', 'TST019-01', '2026-05-17T10:00:00', '2026-05-24T09:00:00', 0, 'Returned', N'Tốt - Trả trễ 7 ngày'),
        (3,  '2026-07-10T08:30:00', N'Design Patterns', 'TST020-01', '2026-07-24T08:30:00', NULL, 0, 'Borrowed', NULL),
        (4,  '2026-06-10T14:00:00', N'Atomic Habits', 'TST021-01', '2026-06-24T14:00:00', NULL, 0, 'Overdue', NULL),
        (5,  '2026-07-15T09:15:00', N'Tư Duy Nhanh Và Chậm', NULL, '2026-07-29T09:15:00', NULL, 0, 'Pending', NULL),
        (6,  '2026-07-01T11:00:00', N'Tâm Lý Học Về Tiền', 'TST023-01', '2026-07-15T11:00:00', NULL, 0, 'Return_Pending', N'Chờ thủ thư kiểm tra tình trạng sách'),
        (7,  '2026-07-02T13:30:00', N'Khởi Nghiệp Tinh Gọn', 'TST024-01', '2026-07-16T13:30:00', NULL, 0, 'Renew_Pending', N'Yêu cầu gia hạn thêm 7 ngày'),
        (8,  '2026-04-10T15:00:00', N'Refactoring', 'TST019-03', '2026-04-24T15:00:00', '2026-04-22T14:00:00', 0, 'Returned', N'Hư hỏng - Rách bìa và ướt trang'),
        (9,  '2026-03-05T08:00:00', N'Hoàng Tử Bé', 'TST026-01', '2026-03-19T08:00:00', '2026-03-18T16:00:00', 1, 'Returned', N'Tốt - Sách nguyên vẹn'),
        (9,  '2026-03-05T08:00:00', N'Không Gia Đình', 'TST027-01', '2026-03-19T08:00:00', '2026-03-18T16:00:00', 0, 'Returned', N'Tốt - Sách nguyên vẹn'),
        (10, '2026-07-16T16:00:00', N'Nhà Lãnh Đạo Không Chức Danh', 'TST025-01', '2026-07-30T16:00:00', NULL, 0, 'Payment_Pending', N'Chờ thanh toán phí mượn'),
        (11, '2026-06-01T09:45:00', N'Lược Sử Thời Gian', 'TST031-01', '2026-06-15T09:45:00', NULL, 1, 'Overdue', NULL),
        (12, '2026-07-08T10:30:00', N'Cánh Đồng Bất Tận', 'TST032-01', '2026-07-22T10:30:00', NULL, 0, 'Borrowed', NULL)
    ) AS seed(member_id, borrow_date, title, barcode, due_date, return_date, renew_count, detail_status, condition_note)
    JOIN dbo.Borrows br
      ON br.member_id = seed.member_id
     AND br.borrow_date = CONVERT(datetime, seed.borrow_date, 126)
    JOIN dbo.Books b ON b.title = seed.title
    LEFT JOIN dbo.BookItems bi ON bi.barcode = seed.barcode
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.BorrowDetails bd
        WHERE bd.borrow_id = br.borrow_id AND bd.book_id = b.book_id
    );

    /* Reservations in all workflow states */
    INSERT INTO dbo.Reservations (member_id, book_id, reservation_date, status)
    SELECT seed.member_id, b.book_id, CONVERT(datetime, seed.reservation_date, 126), seed.status
    FROM (VALUES
        (1,  N'Atomic Habits', '2026-07-01T08:00:00', 'Pending'),
        (2,  N'Hoàng Tử Bé', '2026-07-02T09:00:00', 'Active'),
        (3,  N'Refactoring', '2026-07-03T10:00:00', 'Ready'),
        (4,  N'Rừng Na Uy', '2026-06-20T11:00:00', 'Completed'),
        (5,  N'Deep Work', '2026-06-21T13:00:00', 'Canceled'),
        (6,  N'Tâm Lý Học Về Tiền', '2026-06-22T14:00:00', 'Refund_Pending'),
        (7,  N'Lược Sử Thời Gian', '2026-06-23T15:00:00', 'Rejected'),
        (8,  N'English Grammar in Use', '2026-07-04T16:00:00', 'Pending'),
        (9,  N'Án Mạng Trên Chuyến Tàu Tốc Hành Phương Đông', '2026-07-05T08:30:00', 'Active'),
        (10, N'Cho Tôi Xin Một Vé Đi Tuổi Thơ', '2026-07-06T09:30:00', 'Ready'),
        (11, N'Nỗi Buồn Chiến Tranh', '2026-06-25T10:30:00', 'Completed'),
        (12, N'Design Patterns', '2026-07-07T11:30:00', 'Pending')
    ) AS seed(member_id, title, reservation_date, status)
    JOIN dbo.Books b ON b.title = seed.title
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Reservations r
        WHERE r.member_id = seed.member_id
          AND r.book_id = b.book_id
          AND r.reservation_date = CONVERT(datetime, seed.reservation_date, 126)
    );

    /* Reviews with different ratings and moderation states */
    INSERT INTO dbo.Feedbacks
        (member_id, book_id, rating, comment, created_date, status, librarian_response, response_date)
    SELECT seed.member_id, b.book_id, seed.rating, seed.comment,
           CONVERT(datetime, seed.created_date, 126), seed.status,
           seed.librarian_response,
           CASE WHEN seed.response_date IS NULL THEN NULL
                ELSE CONVERT(datetime, seed.response_date, 126) END
    FROM (VALUES
        (1, N'The Pragmatic Programmer', 5, N'Nội dung thực tế, nhiều lời khuyên hữu ích cho lập trình viên.', '2026-05-15T09:00:00', 'APPROVED', N'Cảm ơn bạn đã chia sẻ đánh giá.', '2026-05-15T14:00:00'),
        (2, N'Refactoring', 4, N'Ví dụ rõ ràng, phù hợp để cải thiện chất lượng mã nguồn.', '2026-05-25T10:00:00', 'APPROVED', NULL, NULL),
        (3, N'Design Patterns', 5, N'Tài liệu nền tảng nhưng cần đọc chậm để hiểu kỹ.', '2026-07-12T08:30:00', 'APPROVED', NULL, NULL),
        (4, N'Atomic Habits', 5, N'Dễ áp dụng vào việc học và quản lý thời gian hằng ngày.', '2026-07-01T14:00:00', 'APPROVED', N'Chúc bạn duy trì được những thói quen tích cực.', '2026-07-02T08:00:00'),
        (5, N'Tư Duy Nhanh Và Chậm', 4, N'Nhiều kiến thức tâm lý thú vị và có chiều sâu.', '2026-07-16T09:15:00', 'PENDING', NULL, NULL),
        (6, N'Tâm Lý Học Về Tiền', 5, N'Cách trình bày gần gũi, phù hợp với người mới tìm hiểu tài chính.', '2026-07-14T11:00:00', 'APPROVED', NULL, NULL),
        (7, N'Khởi Nghiệp Tinh Gọn', 4, N'Hữu ích khi cần kiểm chứng ý tưởng sản phẩm nhanh.', '2026-07-15T13:30:00', 'APPROVED', NULL, NULL),
        (8, N'Refactoring', 2, N'Sách bị hư hỏng trong quá trình sử dụng nên trải nghiệm chưa tốt.', '2026-04-23T15:00:00', 'APPROVED', N'Thư viện đã ghi nhận tình trạng bản sách.', '2026-04-24T09:00:00'),
        (9, N'Hoàng Tử Bé', 5, N'Một câu chuyện ngắn nhưng có nhiều thông điệp sâu sắc.', '2026-03-20T08:00:00', 'APPROVED', NULL, NULL),
        (9, N'Không Gia Đình', 4, N'Câu chuyện cảm động về nghị lực và tình cảm gia đình.', '2026-03-20T08:05:00', 'APPROVED', NULL, NULL),
        (11, N'Lược Sử Thời Gian', 3, N'Nội dung hay nhưng một số phần khá khó với người mới.', '2026-06-10T09:45:00', 'DELETED_BY_MEMBER', NULL, NULL),
        (12, N'Cánh Đồng Bất Tận', 5, N'Văn phong giàu cảm xúc và khắc họa nhân vật chân thực.', '2026-07-12T10:30:00', 'APPROVED', NULL, NULL)
    ) AS seed(member_id, title, rating, comment, created_date, status, librarian_response, response_date)
    JOIN dbo.Books b ON b.title = seed.title
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Feedbacks f
        WHERE f.member_id = seed.member_id
          AND f.book_id = b.book_id
          AND f.created_date = CONVERT(datetime, seed.created_date, 126)
    );

    /* Favorites */
    INSERT INTO dbo.Favorites (member_id, book_id)
    SELECT seed.member_id, b.book_id
    FROM (VALUES
        (1, N'Atomic Habits'), (1, N'Deep Work'), (1, N'The Pragmatic Programmer'),
        (2, N'Refactoring'), (2, N'Design Patterns'), (2, N'English Grammar in Use'),
        (3, N'Lược Sử Thời Gian'), (3, N'Tư Duy Nhanh Và Chậm'),
        (4, N'Tâm Lý Học Về Tiền'), (4, N'Khởi Nghiệp Tinh Gọn'),
        (5, N'Hoàng Tử Bé'), (5, N'Không Gia Đình'), (5, N'Cho Tôi Xin Một Vé Đi Tuổi Thơ'),
        (6, N'Rừng Na Uy'), (6, N'Cánh Đồng Bất Tận'),
        (7, N'Án Mạng Trên Chuyến Tàu Tốc Hành Phương Đông'), (7, N'Chú Chó Của Dòng Họ Baskerville'),
        (8, N'Nỗi Buồn Chiến Tranh'), (8, N'Sống Mòn'),
        (9, N'Nhà Lãnh Đạo Không Chức Danh'), (9, N'Atomic Habits'),
        (10, N'Deep Work'), (10, N'English Grammar in Use'),
        (11, N'Design Patterns'), (11, N'Refactoring'),
        (12, N'The Pragmatic Programmer'), (12, N'Lược Sử Thời Gian'),
        (13, N'Tâm Lý Học Về Tiền'), (14, N'Hoàng Tử Bé'), (15, N'Cánh Đồng Bất Tận')
    ) AS seed(member_id, title)
    JOIN dbo.Books b ON b.title = seed.title
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Favorites f
        WHERE f.member_id = seed.member_id AND f.book_id = b.book_id
    );

    /* Book acquisition requests */
    INSERT INTO dbo.BookAcquisitionRequests
        (member_id, title, created_date, author, decision_note, processed_date,
         publication_year, publisher, reference_url, request_reason, status, isbn)
    SELECT seed.member_id, seed.title, CONVERT(datetime, seed.created_date, 126),
           seed.author, seed.decision_note,
           CASE WHEN seed.processed_date IS NULL THEN NULL
                ELSE CONVERT(datetime2, seed.processed_date, 126) END,
           seed.publication_year, seed.publisher, seed.reference_url,
           seed.request_reason, seed.status, seed.isbn
    FROM (VALUES
        (1, N'Introduction to Algorithms', '2026-07-01T08:00:00', N'Thomas H. Cormen', NULL, NULL, 2022, 'MIT Press', 'https://example.com/books/clrs', N'Bổ sung tài liệu thuật toán chuyên sâu.', 'PENDING', '9780262046305'),
        (2, N'Domain-Driven Design', '2026-06-15T09:00:00', N'Eric Evans', N'Đã duyệt để bổ sung cho khu lập trình.', '2026-06-18T10:00:00', 2003, 'Addison-Wesley', 'https://example.com/books/ddd', N'Phục vụ môn kiến trúc phần mềm.', 'APPROVED', '9780321125217'),
        (3, N'Effective Java', '2026-06-16T10:00:00', N'Joshua Bloch', N'Đã có đủ số lượng bản tương tự.', '2026-06-19T11:00:00', 2018, 'Addison-Wesley', 'https://example.com/books/effective-java', N'Tài liệu Java nâng cao.', 'REJECTED', '9780134685991'),
        (4, N'Computer Networking: A Top-Down Approach', '2026-07-02T11:00:00', N'James Kurose', NULL, NULL, 2021, 'Pearson', 'https://example.com/books/networking', N'Tài liệu học mạng máy tính.', 'PENDING', '9780136681557'),
        (5, N'Head First Design Patterns', '2026-07-03T12:00:00', N'Eric Freeman', NULL, NULL, 2020, 'OReilly Media', 'https://example.com/books/head-first-patterns', N'Cách tiếp cận trực quan cho design pattern.', 'PENDING', '9781492078005'),
        (6, N'The Psychology of Money Workbook', '2026-06-20T13:00:00', N'Morgan Housel', N'Được duyệt cho khu tài chính.', '2026-06-22T08:30:00', 2024, 'Harriman House', 'https://example.com/books/money-workbook', N'Bổ sung bài tập thực hành tài chính cá nhân.', 'APPROVED', '9781804090632'),
        (7, N'Fluent Python', '2026-07-04T14:00:00', N'Luciano Ramalho', NULL, NULL, 2022, 'OReilly Media', 'https://example.com/books/fluent-python', N'Tài liệu Python nâng cao.', 'PENDING', '9781492056355'),
        (8, N'Learning SQL', '2026-06-21T15:00:00', N'Alan Beaulieu', N'Đã duyệt cho nhóm cơ sở dữ liệu.', '2026-06-23T09:00:00', 2020, 'OReilly Media', 'https://example.com/books/learning-sql', N'Tài liệu luyện tập SQL.', 'APPROVED', '9781492057611'),
        (9, N'The Clean Coder', '2026-06-25T16:00:00', N'Robert C. Martin', N'Nội dung trùng lặp nhiều với các sách hiện có.', '2026-06-28T10:00:00', 2011, 'Prentice Hall', 'https://example.com/books/clean-coder', N'Phát triển tác phong chuyên nghiệp.', 'REJECTED', '9780137081073'),
        (10, N'Database System Concepts', '2026-07-05T08:30:00', N'Abraham Silberschatz', NULL, NULL, 2019, 'McGraw-Hill', 'https://example.com/books/database-concepts', N'Tài liệu nền tảng hệ quản trị cơ sở dữ liệu.', 'PENDING', '9780078022159'),
        (11, N'Grokking Algorithms', '2026-07-06T09:30:00', N'Aditya Bhargava', NULL, NULL, 2024, 'Manning', 'https://example.com/books/grokking-algorithms', N'Giải thích thuật toán bằng hình ảnh dễ hiểu.', 'PENDING', '9781633438538'),
        (12, N'Clean Architecture in Practice', '2026-06-26T10:30:00', N'Various Authors', N'Chưa có nhà cung cấp phù hợp.', '2026-06-29T14:00:00', 2023, 'Tech Press', 'https://example.com/books/architecture-practice', N'Tình huống thực tế về kiến trúc sạch.', 'REJECTED', '9786044001999')
    ) AS seed(member_id, title, created_date, author, decision_note, processed_date,
              publication_year, publisher, reference_url, request_reason, status, isbn)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.BookAcquisitionRequests r
        WHERE r.member_id = seed.member_id
          AND r.title = seed.title
          AND r.created_date = CONVERT(datetime, seed.created_date, 126)
    );

    /* Wallet and financial scenarios */
    INSERT INTO dbo.Transactions
        (wallet_id, borrow_id, transaction_type, amount, transaction_date, status)
    SELECT w.wallet_id, br.borrow_id, seed.transaction_type, seed.amount,
           CONVERT(datetime, seed.transaction_date, 126), seed.status
    FROM (VALUES
        (1,  NULL, NULL,                  'TOP_UP',      CAST(150000.00 AS decimal(18,2)), '2026-04-25T09:00:00', 'Completed'),
        (1,  1, '2026-05-01T09:00:00',    'BORROW_FEE',  CAST(-5000.00 AS decimal(18,2)), '2026-05-01T09:05:00', 'Completed'),
        (2,  2, '2026-05-03T10:00:00',    'BORROW_FEE',  CAST(-5000.00 AS decimal(18,2)), '2026-05-03T10:05:00', 'Completed'),
        (2,  2, '2026-05-03T10:00:00',    'FINE',        CAST(35000.00 AS decimal(18,2)), '2026-05-24T09:05:00', 'Pending'),
        (3,  NULL, NULL,                  'TOP_UP',      CAST(200000.00 AS decimal(18,2)), '2026-05-09T10:00:00', 'Completed'),
        (4,  4, '2026-06-10T14:00:00',    'FINE',        CAST(80000.00 AS decimal(18,2)), '2026-06-25T08:00:00', 'Pending'),
        (4,  4, '2026-06-10T14:00:00',    'DAMAGE_FEE',  CAST(40000.00 AS decimal(18,2)), '2026-06-25T08:01:00', 'Pending'),
        (5,  NULL, NULL,                  'DEPOSIT',     CAST(-50000.00 AS decimal(18,2)), '2026-06-21T13:05:00', 'Completed'),
        (5,  NULL, NULL,                  'REFUND',      CAST(50000.00 AS decimal(18,2)), '2026-06-22T13:05:00', 'Completed'),
        (6,  NULL, NULL,                  'TOP_UP',      CAST(300000.00 AS decimal(18,2)), '2026-06-30T10:00:00', 'Completed'),
        (7,  7, '2026-07-02T13:30:00',    'BORROW_FEE',  CAST(-5000.00 AS decimal(18,2)), '2026-07-02T13:35:00', 'Completed'),
        (8,  8, '2026-04-10T15:00:00',    'DAMAGE_FEE',  CAST(120000.00 AS decimal(18,2)), '2026-04-22T14:05:00', 'Completed'),
        (9,  9, '2026-03-05T08:00:00',    'BORROW_FEE',  CAST(-10000.00 AS decimal(18,2)), '2026-03-05T08:05:00', 'Completed'),
        (11, 11, '2026-06-01T09:45:00',   'FINE',        CAST(100000.00 AS decimal(18,2)), '2026-06-16T09:45:00', 'Pending'),
        (12, NULL, NULL,                  'TOP_UP',      CAST(250000.00 AS decimal(18,2)), '2026-05-07T10:00:00', 'Completed'),
        (12, 12, '2026-07-08T10:30:00',   'BORROW_FEE',  CAST(-5000.00 AS decimal(18,2)), '2026-07-08T10:35:00', 'Completed')
    ) AS seed(member_id, borrow_member_id, borrow_date, transaction_type, amount, transaction_date, status)
    JOIN dbo.Wallets w ON w.member_id = seed.member_id
    LEFT JOIN dbo.Borrows br
      ON br.member_id = seed.borrow_member_id
     AND br.borrow_date = CONVERT(datetime, seed.borrow_date, 126)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Transactions t
        WHERE t.wallet_id = w.wallet_id
          AND t.transaction_type = seed.transaction_type
          AND t.amount = seed.amount
          AND t.transaction_date = CONVERT(datetime, seed.transaction_date, 126)
    );

    /* PayOS lifecycle scenarios. Dates are older than seven days so the
       reconciliation scheduler will not call the real gateway for seed rows. */
    INSERT INTO dbo.PayOSPayments
        (member_id, transaction_id, purpose, reference_id, amount, order_code,
         payment_link_id, checkout_url, qr_code, status, bank_reference, created_at, paid_at)
    SELECT seed.member_id,
           linked_transaction.transaction_id,
           seed.purpose,
           CASE
               WHEN seed.purpose = 'FINE' THEN reference_transaction.transaction_id
               WHEN seed.purpose = 'BORROW_FEE' THEN reference_borrow.borrow_id
               ELSE NULL
           END,
           seed.amount, seed.order_code,
           CONCAT('seed-link-', seed.order_code),
           CONCAT('https://pay.payos.vn/web/seed-', seed.order_code),
           CONCAT(N'TEST_QR_', seed.order_code),
           seed.status, seed.bank_reference,
           CONVERT(datetime2, seed.created_at, 126),
           CASE WHEN seed.paid_at IS NULL THEN NULL
                ELSE CONVERT(datetime2, seed.paid_at, 126) END
    FROM (VALUES
        (3,  'TOP_UP',     CAST(200000.00 AS decimal(18,2)), CAST(20260717009001 AS bigint), 'PAID',      'SEED-BANK-001', '2026-05-09T09:58:00', '2026-05-09T10:00:00', 3,  'TOP_UP',     '2026-05-09T10:00:00', NULL, NULL,                    NULL, NULL, NULL),
        (4,  'FINE_BATCH', CAST(120000.00 AS decimal(18,2)), CAST(20260717009002 AS bigint), 'PENDING',   NULL,            '2026-05-10T08:00:00', NULL,                  NULL, NULL,         NULL,                  NULL, NULL, NULL,                    NULL, NULL),
        (5,  'TOP_UP',     CAST(100000.00 AS decimal(18,2)), CAST(20260717009003 AS bigint), 'CANCELLED', NULL,            '2026-05-11T08:00:00', NULL,                  NULL, NULL,         NULL,                  NULL, NULL, NULL,                    NULL, NULL),
        (10, 'BORROW_FEE', CAST(5000.00 AS decimal(18,2)),   CAST(20260717009004 AS bigint), 'PENDING',   NULL,            '2026-05-12T08:00:00', NULL,                  NULL, NULL,         NULL,                  NULL, NULL, NULL,                    10,  '2026-07-16T16:00:00'),
        (6,  'TOP_UP',     CAST(50000.00 AS decimal(18,2)),  CAST(20260717009005 AS bigint), 'FAILED',    NULL,            '2026-05-13T08:00:00', NULL,                  NULL, NULL,         NULL,                  NULL, NULL, NULL,                    NULL, NULL),
        (8,  'FINE',       CAST(120000.00 AS decimal(18,2)), CAST(20260717009006 AS bigint), 'PAID',      'SEED-BANK-006', '2026-04-22T14:00:00', '2026-04-22T14:05:00', 8,  'DAMAGE_FEE', '2026-04-22T14:05:00', 8,  'DAMAGE_FEE', '2026-04-22T14:05:00', NULL, NULL),
        (12, 'TOP_UP',     CAST(50000.00 AS decimal(18,2)),  CAST(20260717009007 AS bigint), 'EXPIRED',   NULL,            '2026-05-14T08:00:00', NULL,                  NULL, NULL,         NULL,                  NULL, NULL, NULL,                    NULL, NULL),
        (9,  'BORROW_FEE', CAST(10000.00 AS decimal(18,2)),  CAST(20260717009008 AS bigint), 'PAID',      'SEED-BANK-008', '2026-03-05T08:01:00', '2026-03-05T08:05:00', 9,  'BORROW_FEE', '2026-03-05T08:05:00', NULL, NULL, NULL,                    9,   '2026-03-05T08:00:00'),
        (1,  'TOP_UP',     CAST(100000.00 AS decimal(18,2)), CAST(20260717009009 AS bigint), 'PENDING',   NULL,            '2026-05-15T08:00:00', NULL,                  NULL, NULL,         NULL,                  NULL, NULL, NULL,                    NULL, NULL)
    ) AS seed(member_id, purpose, amount, order_code, status, bank_reference, created_at, paid_at,
              linked_member_id, linked_type, linked_date,
              reference_member_id, reference_type, reference_date,
              borrow_member_id, borrow_date)
    LEFT JOIN dbo.Wallets linked_wallet ON linked_wallet.member_id = seed.linked_member_id
    LEFT JOIN dbo.Transactions linked_transaction
      ON linked_transaction.wallet_id = linked_wallet.wallet_id
     AND linked_transaction.transaction_type = seed.linked_type
     AND linked_transaction.transaction_date = CONVERT(datetime, seed.linked_date, 126)
    LEFT JOIN dbo.Wallets reference_wallet ON reference_wallet.member_id = seed.reference_member_id
    LEFT JOIN dbo.Transactions reference_transaction
      ON reference_transaction.wallet_id = reference_wallet.wallet_id
     AND reference_transaction.transaction_type = seed.reference_type
     AND reference_transaction.transaction_date = CONVERT(datetime, seed.reference_date, 126)
    LEFT JOIN dbo.Borrows reference_borrow
      ON reference_borrow.member_id = seed.borrow_member_id
     AND reference_borrow.borrow_date = CONVERT(datetime, seed.borrow_date, 126)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.PayOSPayments p WHERE p.order_code = seed.order_code
    );

    /* Fine items for the pending batch payment */
    INSERT INTO dbo.PayOSPaymentFineItems (payment_id, fine_transaction_id, amount_snapshot)
    SELECT p.payment_id, t.transaction_id, ABS(t.amount)
    FROM dbo.PayOSPayments p
    JOIN dbo.Wallets w ON w.member_id = 4
    JOIN dbo.Transactions t ON t.wallet_id = w.wallet_id
    WHERE p.order_code = 20260717009002
      AND t.transaction_date IN (
          CONVERT(datetime, '2026-06-25T08:00:00', 126),
          CONVERT(datetime, '2026-06-25T08:01:00', 126)
      )
      AND NOT EXISTS (
          SELECT 1 FROM dbo.PayOSPaymentFineItems fi
          WHERE fi.payment_id = p.payment_id AND fi.fine_transaction_id = t.transaction_id
      );

    /* Payment audit history */
    INSERT INTO dbo.PayOSPaymentAuditLogs
        (created_at, event_type, message, new_status, old_status, source,
         successful, actor_user_id, payment_id)
    SELECT CONVERT(datetime2, seed.created_at, 126), seed.event_type, seed.message,
           seed.new_status, seed.old_status, seed.source, seed.successful,
           seed.actor_user_id, p.payment_id
    FROM (VALUES
        (CAST(20260717009001 AS bigint), '2026-05-09T09:58:00', 'PAYMENT_CREATED', N'Tạo yêu cầu nạp ví thử nghiệm.', 'PENDING', NULL, 'APPLICATION', CAST(1 AS bit), NULL),
        (CAST(20260717009001 AS bigint), '2026-05-09T10:00:00', 'PAYMENT_PAID', N'Webhook xác nhận thanh toán thành công.', 'PAID', 'PENDING', 'WEBHOOK', CAST(1 AS bit), NULL),
        (CAST(20260717009002 AS bigint), '2026-05-10T08:00:00', 'PAYMENT_CREATED', N'Tạo yêu cầu thanh toán nhiều khoản phạt.', 'PENDING', NULL, 'APPLICATION', CAST(1 AS bit), NULL),
        (CAST(20260717009003 AS bigint), '2026-05-11T08:03:00', 'PAYMENT_CANCELLED', N'Thành viên hủy giao dịch.', 'CANCELLED', 'PENDING', 'MEMBER', CAST(1 AS bit), NULL),
        (CAST(20260717009004 AS bigint), '2026-05-12T08:00:00', 'PAYMENT_CREATED', N'Tạo yêu cầu thanh toán phí mượn.', 'PENDING', NULL, 'APPLICATION', CAST(1 AS bit), NULL),
        (CAST(20260717009005 AS bigint), '2026-05-13T08:05:00', 'RECONCILIATION_FAILED', N'Không thể đồng bộ trạng thái từ cổng thanh toán.', 'FAILED', 'PENDING', 'SCHEDULED_JOB', CAST(0 AS bit), NULL),
        (CAST(20260717009006 AS bigint), '2026-04-22T14:05:00', 'PAYMENT_PAID', N'Đã thanh toán phí bồi thường sách hư hỏng.', 'PAID', 'PENDING', 'WEBHOOK', CAST(1 AS bit), NULL),
        (CAST(20260717009007 AS bigint), '2026-05-14T08:10:00', 'STATUS_CHANGED', N'Liên kết thanh toán đã hết hạn.', 'EXPIRED', 'PENDING', 'RECONCILIATION', CAST(1 AS bit), NULL),
        (CAST(20260717009008 AS bigint), '2026-03-05T08:05:00', 'PAYMENT_PAID', N'Đã thanh toán phí mượn sách.', 'PAID', 'PENDING', 'WEBHOOK', CAST(1 AS bit), NULL),
        (CAST(20260717009009 AS bigint), '2026-05-15T08:00:00', 'PAYMENT_CREATED', N'Tạo yêu cầu nạp ví đang chờ xử lý.', 'PENDING', NULL, 'APPLICATION', CAST(1 AS bit), NULL)
    ) AS seed(order_code, created_at, event_type, message, new_status, old_status, source, successful, actor_user_id)
    JOIN dbo.PayOSPayments p ON p.order_code = seed.order_code
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.PayOSPaymentAuditLogs a
        WHERE a.payment_id = p.payment_id
          AND a.event_type = seed.event_type
          AND a.created_at = CONVERT(datetime2, seed.created_at, 126)
    );

    INSERT INTO dbo.PayOSReconciliationIssues
        (attempt_count, error_message, first_seen_at, last_attempt_at, resolved_at, status, payment_id)
    SELECT seed.attempt_count, seed.error_message,
           CONVERT(datetime2, seed.first_seen_at, 126),
           CONVERT(datetime2, seed.last_attempt_at, 126),
           CASE WHEN seed.resolved_at IS NULL THEN NULL
                ELSE CONVERT(datetime2, seed.resolved_at, 126) END,
           seed.status, p.payment_id
    FROM (VALUES
        (CAST(20260717009005 AS bigint), 3, N'Lỗi kết nối cổng thanh toán trong dữ liệu test.', '2026-05-13T08:05:00', '2026-05-13T08:15:00', NULL, 'OPEN'),
        (CAST(20260717009007 AS bigint), 2, N'Giao dịch hết hạn đã được xác nhận lại.', '2026-05-14T08:10:00', '2026-05-14T08:20:00', '2026-05-14T08:21:00', 'RESOLVED')
    ) AS seed(order_code, attempt_count, error_message, first_seen_at, last_attempt_at, resolved_at, status)
    JOIN dbo.PayOSPayments p ON p.order_code = seed.order_code
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.PayOSReconciliationIssues i
        WHERE i.payment_id = p.payment_id AND i.status = seed.status
    );

    /* Inventory disposal scenarios */
    INSERT INTO dbo.BookDisposals (book_item_id, staff_id, reason, disposal_date, status)
    SELECT bi.book_item_id, librarian.staff_id, seed.reason,
           CONVERT(datetime, seed.disposal_date, 126), seed.status
    FROM (VALUES
        ('TST029-03', N'Sách cũ, hỏng gáy và không còn khả năng phục hồi.', '2026-06-01T09:00:00', 'Completed'),
        ('TST030-03', N'Bản sách thất lạc sau đợt kiểm kê định kỳ.', '2026-06-02T10:00:00', 'Completed'),
        ('TST019-03', N'Đang chờ phê duyệt phương án sửa chữa hoặc thanh lý.', '2026-06-03T11:00:00', 'Pending')
    ) AS seed(barcode, reason, disposal_date, status)
    JOIN dbo.BookItems bi ON bi.barcode = seed.barcode
    CROSS APPLY (
        SELECT TOP (1) st.staff_id
        FROM dbo.Staff st
        WHERE st.staff_type = 'Librarian'
        ORDER BY st.staff_id
    ) librarian
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.BookDisposals d
        WHERE d.book_item_id = bi.book_item_id
          AND d.disposal_date = CONVERT(datetime, seed.disposal_date, 126)
    );

    /* System settings required by borrowing and fine calculations */
    INSERT INTO dbo.SystemSettings (setting_key, setting_value, description)
    SELECT seed.setting_key, seed.setting_value, seed.description
    FROM (VALUES
        ('BORROW_FEE_PER_BOOK', N'5000', N'Phí mượn cho mỗi cuốn sách (VND)'),
        ('MAX_RENEWALS', N'2', N'Số lần gia hạn tối đa cho một bản sách'),
        ('RENEW_DAYS', N'7', N'Số ngày được cộng thêm cho mỗi lần gia hạn'),
        ('FINE_RATE_PER_DAY', N'5000', N'Mức phạt quá hạn mỗi ngày (VND)')
    ) AS seed(setting_key, setting_value, description)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.SystemSettings s WHERE s.setting_key = seed.setting_key
    );

    /* Notifications and recipient/read-state combinations */
    INSERT INTO dbo.Notifications
        (staff_id, title, content, created_date, status, notification_type)
    SELECT librarian.staff_id, seed.title, seed.content,
           CONVERT(datetime, seed.created_date, 126), 'Active', seed.notification_type
    FROM (VALUES
        (N'Sách mới đã có tại thư viện', N'20 đầu sách mới đã được bổ sung vào danh mục thử nghiệm.', '2026-07-01T08:00:00', N'GENERAL'),
        (N'Nhắc hạn trả sách', N'Bạn có sách sắp đến hạn. Vui lòng kiểm tra lịch sử mượn.', '2026-07-12T08:00:00', N'BORROW_REMINDER'),
        (N'Cảnh báo sách quá hạn', N'Một phiếu mượn đã quá hạn và có thể phát sinh tiền phạt.', '2026-07-13T08:00:00', N'OVERDUE'),
        (N'Yêu cầu gia hạn đang chờ duyệt', N'Yêu cầu gia hạn của bạn đã được gửi đến thủ thư.', '2026-07-14T08:00:00', N'RENEWAL'),
        (N'Yêu cầu trả sách đang chờ xử lý', N'Thủ thư sẽ kiểm tra tình trạng sách trước khi hoàn tất.', '2026-07-14T09:00:00', N'RETURN'),
        (N'Thanh toán thành công', N'Giao dịch thử nghiệm đã được PayOS xác nhận thành công.', '2026-05-09T10:01:00', N'PAYMENT'),
        (N'Thanh toán thất bại', N'Không thể hoàn tất giao dịch thử nghiệm. Vui lòng thử lại.', '2026-05-13T08:06:00', N'PAYMENT'),
        (N'Đề xuất sách được phê duyệt', N'Đề xuất bổ sung sách của bạn đã được thư viện chấp nhận.', '2026-06-23T09:01:00', N'ACQUISITION')
    ) AS seed(title, content, created_date, notification_type)
    CROSS APPLY (
        SELECT TOP (1) st.staff_id
        FROM dbo.Staff st
        WHERE st.staff_type = 'Librarian'
        ORDER BY st.staff_id
    ) librarian
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.Notifications n
        WHERE n.title = seed.title
          AND n.created_date = CONVERT(datetime, seed.created_date, 126)
    );

    INSERT INTO dbo.MemberNotifications (member_id, notification_id, is_read, read_date)
    SELECT seed.member_id, n.notification_id, seed.is_read,
           CASE WHEN seed.read_date IS NULL THEN NULL
                ELSE CONVERT(datetime, seed.read_date, 126) END
    FROM (VALUES
        (1, N'Sách mới đã có tại thư viện', '2026-07-01T08:00:00', CAST(1 AS bit), '2026-07-01T09:00:00'),
        (2, N'Sách mới đã có tại thư viện', '2026-07-01T08:00:00', CAST(0 AS bit), NULL),
        (3, N'Nhắc hạn trả sách', '2026-07-12T08:00:00', CAST(1 AS bit), '2026-07-12T08:30:00'),
        (4, N'Cảnh báo sách quá hạn', '2026-07-13T08:00:00', CAST(0 AS bit), NULL),
        (7, N'Yêu cầu gia hạn đang chờ duyệt', '2026-07-14T08:00:00', CAST(1 AS bit), '2026-07-14T08:10:00'),
        (6, N'Yêu cầu trả sách đang chờ xử lý', '2026-07-14T09:00:00', CAST(0 AS bit), NULL),
        (3, N'Thanh toán thành công', '2026-05-09T10:01:00', CAST(1 AS bit), '2026-05-09T10:05:00'),
        (6, N'Thanh toán thất bại', '2026-05-13T08:06:00', CAST(0 AS bit), NULL),
        (8, N'Đề xuất sách được phê duyệt', '2026-06-23T09:01:00', CAST(1 AS bit), '2026-06-23T10:00:00'),
        (9, N'Sách mới đã có tại thư viện', '2026-07-01T08:00:00', CAST(0 AS bit), NULL),
        (10, N'Nhắc hạn trả sách', '2026-07-12T08:00:00', CAST(0 AS bit), NULL),
        (11, N'Cảnh báo sách quá hạn', '2026-07-13T08:00:00', CAST(1 AS bit), '2026-07-13T09:00:00')
    ) AS seed(member_id, title, created_date, is_read, read_date)
    JOIN dbo.Notifications n
      ON n.title = seed.title
     AND n.created_date = CONVERT(datetime, seed.created_date, 126)
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.MemberNotifications mn
        WHERE mn.member_id = seed.member_id AND mn.notification_id = n.notification_id
    );

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* Quick verification summary */
SELECT N'Authors' AS entity_name, COUNT_BIG(*) AS total_rows FROM dbo.Authors
UNION ALL SELECT N'Books', COUNT_BIG(*) FROM dbo.Books
UNION ALL SELECT N'BookItems', COUNT_BIG(*) FROM dbo.BookItems
UNION ALL SELECT N'Borrows', COUNT_BIG(*) FROM dbo.Borrows
UNION ALL SELECT N'BorrowDetails', COUNT_BIG(*) FROM dbo.BorrowDetails
UNION ALL SELECT N'Reservations', COUNT_BIG(*) FROM dbo.Reservations
UNION ALL SELECT N'Feedbacks', COUNT_BIG(*) FROM dbo.Feedbacks
UNION ALL SELECT N'Transactions', COUNT_BIG(*) FROM dbo.Transactions
UNION ALL SELECT N'PayOSPayments', COUNT_BIG(*) FROM dbo.PayOSPayments
UNION ALL SELECT N'BookAcquisitionRequests', COUNT_BIG(*) FROM dbo.BookAcquisitionRequests;
GO
