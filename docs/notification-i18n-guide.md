# Quy ước i18n cho thông báo Member

## Mục tiêu

- `messages.properties` là tiếng Anh mặc định.
- `messages_vi.properties` chứa bản dịch tiếng Việt.
- Cùng một thông báo hệ thống phải đổi EN/VI theo ngôn ngữ giao diện hiện tại.
- Tên sách, tên người dùng, lý do do người dùng nhập và thông báo thủ thư nhập tay không tự dịch.

## Khi tạo thông báo nghiệp vụ

Không gọi `getForMember(...)` rồi lưu kết quả vào `title/content`. Hãy lưu key và arguments bằng `prepareNotification(...)`:

```java
Notification notification = new Notification();
localizedMessageService.prepareNotification(
        notification,
        "systemNotification.reservation.approved.title",
        "systemNotification.reservation.approved.content",
        reservation.getBook().getTitle());

notification.setNotificationType(NotificationType.RESERVATION);
notification.setEventType(NotificationEventType.RESERVATION_APPROVED);
notification.setNotificationSource(NotificationSource.LIBRARIAN);
notification.setCreatedDate(LocalDateTime.now());
notification.setStatus("Active");
notificationRepository.save(notification);
```

`prepareNotification(...)` sẽ:

1. Lưu `title_key`, `content_key`, `message_arguments`.
2. Lưu bản tiếng Anh vào `title/content` để fallback.
3. Cho phép `MemberNotificationServiceImpl` render theo locale của request hiện tại.

## Quy tắc arguments

- Truyền dữ liệu gốc: tên sách, ID, số ngày, `BigDecimal`, ngày ISO hoặc ngày đã thống nhất định dạng.
- Không truyền cả câu đã dịch làm argument.
- Với tiền, truyền giá trị số và định dạng trong properties:

```properties
# messages.properties
notification.payment.content=Paid {0,number,#,##0} VND.

# messages_vi.properties
notification.payment.content=Đã thanh toán {0,number,#,##0} VNĐ.
```

## Thông báo thủ công

Thông báo do thủ thư nhập trực tiếp giữ `title_key`, `content_key`, `message_arguments` là `NULL` và lưu nguyên văn `title/content`. Không sử dụng dịch máy tự động cho dữ liệu này.

## Checklist khi thêm chức năng mới

1. Thêm hoặc chọn `NotificationType` và `NotificationEventType` đúng nghiệp vụ.
2. Xác định `NotificationSource.SYSTEM` hay `NotificationSource.LIBRARIAN`.
3. Thêm cùng một key vào cả hai file properties.
4. Gọi `prepareNotification(...)`, không gọi `getForMember(...)` để tạo nội dung lưu database.
5. Không dịch dữ liệu nghiệp vụ do người dùng nhập.
6. Kiểm tra cùng một `notification_id` ở `?lang=en`, sau đó `?lang=vi`.
7. Đảm bảo trang hiển thị gọi `renderNotificationTitle/Content`, hoặc lấy DTO từ `MemberNotificationService`.

## Database

Chạy `database/migrations/2026-07-18-member-notification-business-metadata.sql` trước khi khởi động phiên bản code có các trường template mới.

