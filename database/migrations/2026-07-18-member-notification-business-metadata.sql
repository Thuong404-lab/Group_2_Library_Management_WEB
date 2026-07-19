/*
 * Adds structured notification metadata and a persistent Member language preference.
 * Safe to execute more than once on SQL Server.
 */

IF COL_LENGTH('dbo.Notifications', 'notification_source') IS NULL
BEGIN
    ALTER TABLE dbo.Notifications
        ADD notification_source nvarchar(20) NOT NULL
            CONSTRAINT DF_Notifications_notification_source DEFAULT 'SYSTEM';
END;
GO

IF COL_LENGTH('dbo.Notifications', 'event_type') IS NULL
BEGIN
    ALTER TABLE dbo.Notifications
        ADD event_type nvarchar(50) NOT NULL
            CONSTRAINT DF_Notifications_event_type DEFAULT 'GENERAL';
END;
GO

IF COL_LENGTH('dbo.Notifications', 'title_key') IS NULL
BEGIN
    ALTER TABLE dbo.Notifications ADD title_key nvarchar(255) NULL;
END;
GO

IF COL_LENGTH('dbo.Notifications', 'content_key') IS NULL
BEGIN
    ALTER TABLE dbo.Notifications ADD content_key nvarchar(255) NULL;
END;
GO

IF COL_LENGTH('dbo.Notifications', 'message_arguments') IS NULL
BEGIN
    ALTER TABLE dbo.Notifications ADD message_arguments nvarchar(max) NULL;
END;
GO

IF COL_LENGTH('dbo.Member_Accounts', 'preferred_language') IS NULL
BEGIN
    ALTER TABLE dbo.Member_Accounts
        ADD preferred_language nvarchar(5) NOT NULL
            CONSTRAINT DF_Member_Accounts_preferred_language DEFAULT 'en';
END;
GO

UPDATE dbo.Notifications
SET notification_source = CASE WHEN staff_id IS NULL THEN 'SYSTEM' ELSE 'LIBRARIAN' END
WHERE notification_source IS NULL OR notification_source = 'SYSTEM';

UPDATE dbo.Notifications
SET event_type = 'LOAN_REQUESTED', notification_type = 'LOAN', notification_source = 'SYSTEM'
WHERE event_type = 'GENERAL'
  AND title IN (N'Yêu cầu mượn sách thành công', N'Loan Request Submitted');

UPDATE dbo.Notifications
SET event_type = 'LOAN_APPROVED', notification_type = 'LOAN', notification_source = 'LIBRARIAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Yêu cầu mượn sách đã được phê duyệt', N'Loan Request Approved');

UPDATE dbo.Notifications
SET event_type = 'LOAN_REJECTED', notification_type = 'LOAN', notification_source = 'LIBRARIAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Yêu cầu mượn sách bị từ chối', N'Loan Request Rejected');

UPDATE dbo.Notifications
SET event_type = 'LOAN_COLLECTED', notification_type = 'LOAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Mượn sách thành công', N'Books Borrowed Successfully',
                N'Đã nhận sách thành công', N'Books Collected Successfully');

UPDATE dbo.Notifications
SET event_type = 'LOAN_PICKUP_EXPIRED', notification_type = 'LOAN', notification_source = 'SYSTEM'
WHERE event_type = 'GENERAL'
  AND title IN (N'Vi phạm thời hạn nhận sách - Hủy phiếu mượn', N'Pickup Deadline Missed - Loan Canceled');

UPDATE dbo.Notifications
SET event_type = 'RETURN_CONFIRMED', notification_type = 'LOAN', notification_source = 'LIBRARIAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Xác nhận trả sách thành công', N'Return Request Approved',
                N'Xác nhận hoàn trả sách tại quầy thành công', N'Book Returned at Desk');

UPDATE dbo.Notifications
SET event_type = 'RENEWAL_APPROVED', notification_type = 'LOAN', notification_source = 'LIBRARIAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Gia hạn sách thành công', N'Book Renewed Successfully',
                N'Phê duyệt gia hạn thành công', N'Renewal Approved');

UPDATE dbo.Notifications
SET event_type = 'RENEWAL_REJECTED', notification_type = 'LOAN', notification_source = 'LIBRARIAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Từ chối gia hạn sách', N'Renewal Rejected');

UPDATE dbo.Notifications
SET event_type = 'RESERVATION_APPROVED', notification_type = 'RESERVATION', notification_source = 'LIBRARIAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Yêu cầu đặt trước được phê duyệt', N'Reservation Approved');

UPDATE dbo.Notifications
SET event_type = 'RESERVATION_REJECTED', notification_type = 'RESERVATION', notification_source = 'LIBRARIAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Yêu cầu đặt trước bị từ chối', N'Reservation Rejected');

UPDATE dbo.Notifications
SET event_type = 'RESERVATION_DEPOSIT_PAID', notification_type = 'RESERVATION', notification_source = 'SYSTEM'
WHERE event_type = 'GENERAL'
  AND title IN (N'Thanh toán tiền cọc thành công', N'Reservation Deposit Paid');

UPDATE dbo.Notifications
SET event_type = 'RESERVATION_REFUNDED', notification_type = 'RESERVATION'
WHERE event_type = 'GENERAL'
  AND title IN (N'Hoàn tiền cọc thành công', N'Deposit Refunded',
                N'Hoàn tiền cọc đặt trước', N'Reservation Deposit Refunded');

UPDATE dbo.Notifications
SET event_type = 'TOP_UP_SUCCESS', notification_type = 'FINANCE'
WHERE event_type = 'GENERAL'
  AND (title IN (N'Nạp tiền thành công', N'Nạp tiền qua KQPay thành công', N'Wallet Top-up Successful', N'KQPay Top-up Successful'));

UPDATE dbo.Notifications
SET event_type = 'OVERDUE_REMINDER', notification_type = 'REMINDER'
WHERE event_type = 'GENERAL'
  AND title IN (N'Nhắc nhở trả sách quá hạn', N'Overdue Book Reminder');

UPDATE dbo.Notifications
SET event_type = 'FINE_CREATED', notification_type = 'FINANCE', notification_source = 'LIBRARIAN'
WHERE event_type = 'GENERAL'
  AND title IN (N'Phí phạt mới', N'New Fine');

UPDATE dbo.Notifications
SET event_type = 'OVERDUE_FINE_CREATED', notification_type = 'FINANCE', notification_source = 'SYSTEM'
WHERE event_type = 'GENERAL'
  AND title IN (N'Phạt quá hạn trả sách', N'Overdue Fine');

UPDATE dbo.Notifications
SET notification_source = 'LIBRARIAN', event_type = 'REVIEW_REPLIED', notification_type = 'REVIEW'
WHERE event_type = 'GENERAL'
  AND title IN (N'Phản hồi đánh giá', N'Review Response');

UPDATE dbo.Notifications
SET notification_source = 'LIBRARIAN',
    event_type = CASE WHEN title IN (N'Đề xuất sách đã được duyệt', N'Book Request Approved')
                      THEN 'ACQUISITION_APPROVED' ELSE 'ACQUISITION_REJECTED' END,
    notification_type = 'ACQUISITION'
WHERE event_type = 'GENERAL'
  AND title IN (N'Đề xuất sách đã được duyệt', N'Book Request Approved',
                N'Đề xuất sách chưa được chấp nhận', N'Book Request Not Approved');

-- Only notifications that do not match a known business event remain manual.
UPDATE dbo.Notifications
SET event_type = 'MANUAL', notification_source = 'LIBRARIAN'
WHERE staff_id IS NOT NULL AND event_type = 'GENERAL';
