# 🚀 HƯỚNG DẪN QUY TRÌNH LÀM VIỆC NHÓM (GIT WORKFLOW & SETUP)

Tài liệu này hướng dẫn chi tiết các bước để các thành viên (Members) tải code, cài đặt môi trường và quy trình sử dụng Git hợp lý để tránh conflict.

---

## PHẦN 1: CÀI ĐẶT MÔI TRƯỜNG LẦN ĐẦU TÊN (DÀNH CHO TẤT CẢ MEMBER)

### Bước 1: Tải mã nguồn (Clone)
Mở Terminal/Git Bash và chạy lệnh sau để tải source code về máy:
```bash
git clone <đường-dẫn-repo-github-của-nhóm>
cd Library_Management_Web
```

### Bước 2: Khởi tạo Database (SQL Server)
1. Mở **SQL Server Management Studio (SSMS)**.
2. Mở file `database/LibraryManagementWeb.sql` có trong thư mục dự án vừa tải về.
3. Bấm **Execute** (hoặc F5) để chạy toàn bộ file. 
4. Hệ thống sẽ tự tạo Database tên là `LibraryManagementWeb` kèm theo toàn bộ bảng và dữ liệu mẫu (Seed Data).

### Bước 3: Cấu hình kết nối Spring Boot
1. Vào thư mục `src/main/resources/`.
2. Tạo một file mới tên là **`application-dev.properties`** (nếu chưa có).
3. Copy toàn bộ nội dung từ file `application-dev.properties.example` dán vào file vừa tạo.
4. Chỉnh sửa lại **username** và **password** của SQL Server cho khớp với máy của bạn:
```properties
spring.datasource.username=sa
spring.datasource.password=mat_khau_cua_ban
```

### Bước 4: Chạy thử dự án
- Mở IDE (IntelliJ / Eclipse), chạy file `LibraryApplication.java`.
- Nếu console báo `Started LibraryApplication...` là thành công!
- **Tài khoản test chung:** 
  - Admin: `admin` / `Test@1234`
  - Librarian: `librarian01` / `Test@1234`
  - Member: `member01` / `Test@1234`

---

## PHẦN 2: QUY TRÌNH LÀM VIỆC VỚI GIT (RẤT QUAN TRỌNG)

> ⚠️ **LUẬT THÉP:** Tuyệt đối KHÔNG ai được code và push trực tiếp lên nhánh `main` hoặc `dev`. Mọi dòng code mới phải được viết trên nhánh `feature/...`.

### 1. Bắt đầu một Task mới (Nhận nhánh đã tạo sẵn)
Do Leader đã tạo sẵn toàn bộ 19 nhánh (branches) cho từng cụm chức năng trên Github, mỗi thành viên KHÔNG CẦN tạo nhánh mới, chỉ cần lấy nhánh có sẵn về máy:

```bash
# 1. Cập nhật danh sách các nhánh mới nhất từ Github
git fetch --all

# 2. Chuyển sang nhánh tính năng được phân công (Ví dụ: feature/book-inventory)
git checkout feature/book-inventory

# 3. Kéo code mới nhất của nhánh này về (phòng trường hợp người khác cũng đang code nhánh đó)
git pull origin feature/book-inventory
```
Bây giờ bạn đã an toàn để bắt đầu code trên nhánh `feature/...` được giao của mình.

### 2. Quá trình Code và Lưu trữ (Commit)
Trong quá trình code, hãy lưu lại các thay đổi một cách thường xuyên:

```bash
# 1. Thêm các file đã thay đổi
git add .

# 2. Commit với message rõ ràng
git commit -m "Thêm tính năng thêm mới sách (UC-05)"
```

### 3. Đồng bộ code với Team (Chống Conflict)
Giả sử bạn code tính năng đó mất 3 ngày. Trong 3 ngày đó, các thành viên khác có thể đã push code mới lên nhánh `dev`. Để code của bạn không bị lỗi thời, **hãy cập nhật code mỗi sáng**:

```bash
# Đang đứng ở nhánh feature của bạn, kéo code mới từ dev về để gộp
git pull origin dev
```
*(Nếu có conflict, Git sẽ báo màu đỏ, bạn mở file ra, họp với người code đoạn đó để giữ lại dòng code nào cho đúng).*

### 4. Đẩy code lên và Chờ duyệt (Push & Pull Request)
Khi đã code xong tính năng và test kỹ không có lỗi, bạn đẩy nhánh của mình lên Github:

```bash
# Đẩy nhánh của bạn lên server
git push origin feature/book-management
```

**Tiếp theo:**
1. Lên trang Github của dự án.
2. Bấm vào nút **Compare & pull request** (Tạo Yêu cầu gộp code).
3. Chọn nhánh đích là **`dev`** (Base: `dev` <- Compare: `feature/book-management`).
4. Viết mô tả ngắn gọn bạn đã làm gì rồi bấm **Create Pull Request**.
5. Nhờ Leader (Thương) hoặc các thành viên khác vào Review Code và bấm gộp (Merge).

---

## PHẦN 3: XỬ LÝ SỰ CỐ NHANH

- **Trót code nhầm trên nhánh dev?**
  - Chạy `git stash` (cất code đi) -> `git checkout feature/...` -> `git stash pop` (lấy code ra lại).
- **File config `application-dev.properties` cứ bị Git bắt commit?**
  - Đảm bảo file đó đã được thêm vào `.gitignore` để không push mật khẩu cá nhân lên mạng.

Chúc toàn đội hoàn thành tốt Sprint 1! 🔥
