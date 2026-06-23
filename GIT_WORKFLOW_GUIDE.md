# 🚀 HƯỚNG DẪN QUY TRÌNH & NỘI QUY GIT WORKFLOW

Tài liệu này bao gồm các **Quy định Thưởng/Phạt** (đóng quỹ) nghiêm ngặt của team, kèm theo hướng dẫn chi tiết quy trình làm việc với Git để tránh conflict và đảm bảo tiến độ.

---

## ⚖️ PHẦN 1: NỘI QUY & THƯỞNG PHẠT (GIT RULES)

> [!CAUTION]
> **Thông báo quan trọng:** Mọi hoạt động `push` code lên Github dù nhiều hay ít đều sẽ được hệ thống ghi log. Leader sẽ kiểm tra thường xuyên, mọi người chú ý cẩn thận!

| Hành vi vi phạm | Hình thức xử lý |
|:---|:---:|
| 🚫 Tự ý push code lên nhánh `main` mà không thông báo | **Phạt 50K (vào quỹ)** |
| 🚫 Tự ý push code lên nhánh của thành viên khác khi chưa được phép | **Phạt 50K (vào quỹ)** |
| 🚫 Đặt sai format Commit Message quá 3 lần | **Phạt 50K (vào quỹ)** |
| 🚫 Trễ tiến độ (không check BackLog) bị nhắc nhở quá 2 lần | **Phạt 100K (vào quỹ)** |

> [!TIP]
> 💸 **Chính sách thưởng:** Bất kỳ ai kiểm tra và phát hiện thành viên khác vi phạm các quy tắc trên, báo cáo lại Leader sẽ được **Thưởng 50K**!

---

## 🌿 PHẦN 2: QUY ĐỊNH LÀM VIỆC TRÊN NHÁNH (BRANCH)

> [!IMPORTANT]
> **Nguyên tắc cốt lõi:**
> 1. Mỗi thành viên chỉ làm việc trên nhánh mang tên của mình (hoặc nhánh task được giao).
> 2. **KHÔNG BAO GIỜ** code trực tiếp trên nhánh `main`.
> 3. Chỉ được push code (merge) lên `main` những khi Leader đã kiểm tra chức năng hoạt động ổn định.

- Mọi người được push tự do lên nhánh cá nhân của mình để lưu trữ code.
- **TUYỆT ĐỐI KHÔNG** tự ý push code lên nhánh của người khác nếu không có sự đồng ý của họ (Vi phạm phạt 50K).

---

## 💻 PHẦN 3: QUY TRÌNH CODE HÀNG NGÀY

### 🔄 1. Trước khi code (Cập nhật code mới nhất)
Luôn luôn chuyển về nhánh của mình và kéo code mới nhất từ Github về để đảm bảo không bị lỗi out-of-date.

```bash
# Chuyển về đúng nhánh của mình (Ví dụ: feature/book-inventory)
git checkout <Tên_nhánh>

# Kéo code mới nhất từ server về máy
git pull origin <Tên_nhánh>
```

### 💾 2. Sau khi hoàn thành một chức năng (Lưu & Đẩy code)
Khi bạn đã code xong một phần nhỏ và muốn lưu lại, thực hiện theo thứ tự sau:

```bash
# Bước 1: Lưu (Thêm) tất cả các thay đổi
git add .

# Bước 2: Commit code kèm thông điệp rõ ràng
# (Lưu ý: Nhớ ghi đúng Message, sai quá 3 lần phạt 50K)
git commit -m "Thêm tính năng đăng nhập (UC-09)"

# Bước 3: Đẩy code lên nhánh của mình trên Github
git push origin <Tên_nhánh>
```

---

## 🛠️ PHẦN 4: HƯỚNG DẪN SETUP MÔI TRƯỜNG (DÀNH CHO LẦN ĐẦU)

> [!NOTE]
> Chỉ thực hiện nếu bạn vừa clone project về máy lần đầu tiên.

### Bước 1: Tải mã nguồn (Clone)
```bash
git clone https://github.com/Thuong404-lab/Group_2_Library_Management_WEB.git
cd Library_Management_Web
```

### Bước 2: Cấu hình Database
1. Mở **SQL Server Management Studio (SSMS)**.
2. Chạy file `database/LibraryManagementWeb.sql` để tạo DB và Seed data mẫu.
3. Vào thư mục `src/main/resources/`, tạo file **`application-dev.properties`** (từ file `.example`).
4. Sửa lại cấu hình mật khẩu SQL Server của máy bạn:
```properties
spring.datasource.username=sa
spring.datasource.password=mat_khau_cua_ban
```

### Bước 3: Chạy thử ứng dụng
- Mở IDE, chạy file `LibraryApplication.java`.
- Nếu console báo `Started LibraryApplication...` là thành công!
- **Tài khoản test chung:** 
  - Admin: `admin` / `Test@1234`
  - Librarian: `librarian01` / `Test@1234`
  - Member: `member01` / `Test@1234`
