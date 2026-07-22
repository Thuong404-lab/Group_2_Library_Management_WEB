# Quy ước Flyway migration

Các migration từ `V1` đến `V13` đã được chia sẻ và phải được giữ nguyên tên cũng như nội dung. Không sửa, xóa hoặc đánh lại số các file này.

## Cách đặt tên migration mới

Từ migration tiếp theo, dùng thời gian tạo chính xác đến giây thay cho số thứ tự liên tiếp:

```text
VyyyyMMddHHmmss__mo_ta_ngan_gon.sql
```

Ví dụ:

```text
V20260722143015__allow_zero_book_quantity.sql
V20260722144732__add_inventory_condition_index.sql
```

Không tự nhập timestamp. Từ thư mục gốc dự án, tạo file bằng lệnh:

```powershell
.\scripts\new-migration.ps1 -Description add_inventory_condition_index
```

Script dùng múi giờ Việt Nam và từ chối tạo file nếu version đã tồn tại.

- Dùng giờ Việt Nam (`Asia/Ho_Chi_Minh`).
- Phần mô tả viết chữ thường, không dấu và phân cách bằng dấu gạch dưới.
- Mỗi thay đổi database phải tạo một migration mới.
- Không sửa migration đã được push hoặc đã chạy trên bất kỳ database nào.
- `data.sql` chỉ chứa dữ liệu mẫu và không thay thế migration thay đổi cấu trúc hoặc chuẩn hóa dữ liệu.

## Trước khi tạo và push

1. Pull/rebase code mới nhất từ nhánh chung.
2. Kiểm tra thư mục này để chắc chắn version timestamp chưa tồn tại.
3. Tạo migration với timestamp hiện tại.
4. Chạy ứng dụng hoặc test để Flyway thực thi migration.
5. Kiểm tra `flyway_schema_history` và chức năng liên quan trước khi push.

Lệnh `mvnw.cmd test` cũng kiểm tra tự động rằng:

- Không có hai migration trùng version.
- Migration mới không quay lại kiểu số tuần tự `V14`, `V15`, ...
- Tên file đúng định dạng timestamp và mô tả `snake_case`.

Sau khi pull một commit có đổi tên migration, chạy `mvnw.cmd clean test` để xóa resource cũ còn sót trong `target/classes`.

Nếu hai migration vẫn vô tình trùng version, người chưa push phải đổi file của mình sang timestamp mới. Nếu file đã được chạy cục bộ trước khi đổi tên, nên tạo lại database phát triển thay vì tự ý sửa lịch sử trên database dùng chung.
