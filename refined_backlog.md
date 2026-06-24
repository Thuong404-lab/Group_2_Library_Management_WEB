# 📚 BẢN PHÂN CÔNG NHIỆM VỤ & PRODUCT BACKLOG (REFINED v2)
**Dự án:** Library Management Web | **Mô hình:** Agile / Scrum | **Nhóm:** G2 (6 Thành viên)
**Ngày bắt đầu:** 22/06/2026 | **Ngày kết thúc:** 10/07/2026 | **Mỗi Task:** 1-3 ngày

> [!IMPORTANT]
> **Các thay đổi so với bản v1 để CÂN BẰNG ĐỘ KHÓ:**
> 1. ~~UC-16 (Librarian Profile)~~ chuyển từ **Member 1** → **Member 3** (vì Member 3 quản lý module Librarian/Loan).
> 2. **UC-13.3 + UC-13.4** (Process Borrow & Renewal — Complex) chuyển từ **Member 3** → **Member 1** (giảm tải Complex cho Member 3, tăng cho Member 1).
> 3. **UC-19.3** (View System Logs) chuyển từ **Member 6** → **Member 1** (giảm số lượng UC cho Member 6).
> 4. **UC-14.2** (Manage Fines — Complex) chuyển từ **Member 5** → **Member 4** (tăng Complex cho Member 4).
> 5. **UC-20.5 + UC-20.6** (Change Status & Reset Password) chuyển từ **Member 6** → **Member 5** (giảm UC cho Member 6, bù UC cho Member 5).
> 6. Loại bỏ cụm `UC-19` khỏi Member 1 (Thương) do trùng lặp với Member 6 → chỉ giữ lại UC-19.3 cho Member 1.
  MK: Test@1234
---

## 📅 TỔNG QUAN SPRINT (TĂNG TỐC)

| Sprint | Thời gian | Số ngày | Mục tiêu chính |
|:---:|:---|:---:|:---|
| **Sprint 1** | 22/06/2026 – 27/06/2026 | 6 ngày | 🟢 Nền tảng: Auth, View cơ bản, Setup CRUD đơn giản |
| **Sprint 2** | 28/06/2026 – 04/07/2026 | 7 ngày | 🟡 Tính năng cốt lõi: Mượn/Trả, Thanh toán, Quản lý sách |
| **Sprint 3** | 05/07/2026 – 10/07/2026 | 6 ngày | 🔴 Nâng cao: Backup, Báo cáo, Loan Processing, Polish |

---

## 📈 ĐẢM BẢO YÊU CẦU LINES OF CODE (LOC)

> [!IMPORTANT]
> **Yêu cầu LOC:** Mỗi thành viên cần đạt khoảng **1900 - 2000 LOC (Lines of Code)** (Chỉ tính BackEnd, không tính HTML/CSS/JS).
> 
> **Đánh giá khả thi:** Hoàn toàn khả thi với Java Spring Boot. Trung bình mỗi Use Case (chỉ tính Backend) sẽ bao gồm:
> *   **Controller:** ~50 - 150 LOC.
> *   **Service (Interface & Implementation):** ~100 - 250 LOC (chứa logic nghiệp vụ lõi).
> *   **DTOs (Request/Response), Exception, Repository:** ~100 - 150 LOC.
> 
> => **Tổng cộng 1 UC = ~250 - 550 LOC (Thuần Backend).** 
> Mỗi thành viên được giao từ **9 đến 12 UCs**, tương đương khối lượng dao động từ **2,250 đến 6,600 LOC** (chỉ tính Java). 
> 
> Tuy nhiên, để đảm bảo vượt mức 2000 LOC một cách an toàn và chất lượng nhất (tránh việc code quá ngắn do dùng các hàm có sẵn), các thành viên **BẮT BUỘC** nên áp dụng:
> 1. **Viết Unit Test (JUnit / Mockito):** Code test cũng là code Backend. Viết test đầy đủ cho Controller và Service sẽ nhân đôi số lượng LOC hợp lệ của bạn (1000 dòng code thực tế thường đi kèm 1000 - 1500 dòng code test).
> 2. **Tách nhỏ hàm:** Tránh viết các hàm "God Method" dài hàng trăm dòng, hãy chia nhỏ logic ra các private methods trong Service.
> 3. **Validation kỹ lưỡng:** Bổ sung các Custom Validator (Anotation) ở Backend thay vì chỉ tin tưởng Frontend.

---

## 👥 TỔNG QUAN PHÂN CÔNG (CÂN BẰNG)

| Member | Họ và Tên | Roll Number | Role Module | UC | 🟢S | 🟡M | 🔴C | Điểm |
|:---:|:---|:---|:---|:---:|:---:|:---:|:---:|:---:|
| **1** | **Nguyễn Tiến Thương** | CE191329 | 🔐 Auth, Profile & Loan Processing | **12** | 5 | 5 | **2** | 21 |
| **2** | **La Tấn Khanh** | CE191640 | 📦 Book Inventory & Storage | **10** | 2 | 5 | **3** | 21 |
| **3** | **Huỳnh Gia Hưng** | CE190488 | 📚 Borrowing, Membership & Librarian Profile | **11** | 3 | 4 | **4** | 23 |
| **4** | **Trần Nguyễn Quốc Anh** | CE191655 | 💬 Services, Interactions & Moderation | **12** | 3 | 7 | **2** | 23 |
| **5** | **Phạm Kiến Quốc** | CE201286 | 💰 Financial, Fines & Account Maintenance | **11** | 4 | 3 | **4** | 22 |
| **6** | **Trần Ngọc Linh Đang** | CE191088 | ⚙️ Admin Dashboard, Accounts & System | **10** | 2 | 4 | **4** | 22 |

> [!TIP]
> **Công thức tính điểm:** Simple = 1đ, Medium = 2đ, Complex = 3đ.
> Khoảng dao động: **20 – 23 điểm** (rất đều). Complex mỗi người: **2 – 4 task** (không ai 0, không ai 6).

---

## 📋 CHI TIẾT PRODUCT BACKLOG THEO TỪNG THÀNH VIÊN

### 👤 Member 1: Nguyễn Tiến Thương (CE191329)
> [!NOTE]
> **Module:** 🔐 Auth, Profile & Loan Processing
> **File chính:** `AuthController`, `GuestController`, `ProfileController`, + code UC-13.3/13.4 trong `LoanController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-9** | Login | Sprint 1 | 22/06 | 24/06 | 🟡 Medium |
| 2 | **UC-10** | Logout | Sprint 1 | 23/06 | 24/06 | 🟢 Simple |
| 3 | **UC-2** | Register | Sprint 1 | 24/06 | 26/06 | 🟡 Medium |
| 4 | **UC-1** | Search Books | Sprint 1 | 25/06 | 26/06 | 🟢 Simple |
| 5 | **UC-3** | View Books List | Sprint 2 | 28/06 | 29/06 | 🟢 Simple |
| 6 | **UC-4.1** | View Profile | Sprint 2 | 29/06 | 30/06 | 🟢 Simple |
| 7 | **UC-4.2** | Update Profile Information | Sprint 2 | 30/06 | 02/07 | 🟡 Medium |
| 8 | **UC-4.3** | Change Password | Sprint 2 | 01/07 | 03/07 | 🟡 Medium |
| 9 | **UC-4.4** | View Favorites List | Sprint 3 | 05/07 | 06/07 | 🟢 Simple |
| 10 | **UC-13.3** | Process Borrow Requests ⚡ | Sprint 3 | 06/07 | 08/07 | 🔴 Complex |
| 11 | **UC-13.4** | Process Renewal Requests ⚡ | Sprint 3 | 07/07 | 09/07 | 🔴 Complex |
| 12 | **UC-22.3** | Membership Tier Management | Sprint 3 | 08/07 | 10/07 | 🟡 Medium |

---

### 👤 Member 2: La Tấn Khanh (CE191640)
> [!NOTE]
> **Module:** 📦 Book Inventory & Storage
> **File chính:** `StorageController`, `InventoryController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-12.6** | Manage Categories & Genres | Sprint 1 | 22/06 | 24/06 | 🟡 Medium |
| 2 | **UC-11.2** | Add Storage Location | Sprint 1 | 23/06 | 25/06 | 🟡 Medium |
| 3 | **UC-11.1** | Update Storage Location | Sprint 1 | 24/06 | 26/06 | 🟡 Medium |
| 4 | **UC-11.3** | Remove Storage Location | Sprint 1 | 25/06 | 26/06 | 🟢 Simple |
| 5 | **UC-12.2** | Update Books Status | Sprint 2 | 28/06 | 30/06 | 🟡 Medium |
| 6 | **UC-12.5** | Remove Books | Sprint 2 | 29/06 | 01/07 | 🟡 Medium |
| 7 | **UC-12.3** | Add New Books | Sprint 2 | 30/06 | 02/07 | 🔴 Complex |
| 8 | **UC-12.4** | Update Books | Sprint 2 | 01/07 | 03/07 | 🔴 Complex |
| 9 | **UC-12.1** | Perform Periodic Inventory Audit | Sprint 3 | 05/07 | 07/07 | 🔴 Complex |
| 10 | **UC-22.5** | Manage Categories | Sprint 3 | 06/07 | 07/07 | 🟢 Simple |

---

### 👤 Member 3: Huỳnh Gia Hưng (CE190488)
> [!NOTE]
> **Module:** 📚 Borrowing, Membership & Librarian Profile
> **File chính:** `MembershipController`, `BorrowController`, `LoanController` (UC-13.1/13.2), `LibrarianProfileController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-5.1** | View Benefits & Privileges | Sprint 1 | 22/06 | 23/06 | 🟢 Simple |
| 2 | **UC-5.2** | View Membership Tier | Sprint 1 | 23/06 | 24/06 | 🟢 Simple |
| 3 | **UC-16.1** | View Profile ⚡ | Sprint 1 | 24/06 | 25/06 | 🟢 Simple |
| 4 | **UC-6.1** | View Borrowing History | Sprint 1 | 25/06 | 27/06 | 🟡 Medium |
| 5 | **UC-16.2** | Update Profile Information ⚡ | Sprint 2 | 28/06 | 30/06 | 🟡 Medium |
| 6 | **UC-16.3** | Change Password ⚡ | Sprint 2 | 29/06 | 01/07 | 🟡 Medium |
| 7 | **UC-13.1** | View Loan Details | Sprint 2 | 30/06 | 02/07 | 🟡 Medium |
| 8 | **UC-6.2** | Reserve Books | Sprint 2 | 01/07 | 03/07 | 🔴 Complex |
| 9 | **UC-6.3** | Borrow Books | Sprint 3 | 05/07 | 07/07 | 🔴 Complex |
| 10 | **UC-6.4** | Return Books | Sprint 3 | 06/07 | 08/07 | 🔴 Complex |
| 11 | **UC-13.2** | Confirm Book Returns | Sprint 3 | 07/07 | 09/07 | 🔴 Complex |

---

### 👤 Member 4: Trần Nguyễn Quốc Anh (CE191655)
> [!NOTE]
> **Module:** 💬 Services, Interactions & Moderation
> **File chính:** `LibraryServiceController`, `InteractionController`, `ReportController`, + code UC-14.2 trong `MemberMgmtController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-7.1** | Add to Favorites | Sprint 1 | 22/06 | 23/06 | 🟢 Simple |
| 2 | **UC-7.4** | View Notification | Sprint 1 | 23/06 | 24/06 | 🟢 Simple |
| 3 | **UC-7.2** | Suggest New Books | Sprint 1 | 24/06 | 26/06 | 🟡 Medium |
| 4 | **UC-7.3** | Rate & Review Books | Sprint 1 | 25/06 | 27/06 | 🟡 Medium |
| 5 | **UC-15.1** | Send Notifications to Members | Sprint 2 | 28/06 | 30/06 | 🟡 Medium |
| 6 | **UC-15.2** | Respond to Book Acquisition Requests | Sprint 2 | 29/06 | 01/07 | 🟡 Medium |
| 7 | **UC-15.3** | Moderate Reviews & Comments | Sprint 2 | 30/06 | 02/07 | 🟡 Medium |
| 8 | **UC-14.2** | Manage Fines & Violations ⚡ | Sprint 2 | 01/07 | 03/07 | 🔴 Complex |
| 9 | **UC-17** | Revenue report | Sprint 3 | 05/07 | 07/07 | 🟡 Medium |
| 10 | **UC-23.1** | Admin console report | Sprint 3 | 06/07 | 08/07 | 🔴 Complex |
| 11 | **UC-23.2** | Export Report | Sprint 3 | 07/07 | 09/07 | 🟡 Medium |
| 12 | **UC-22.2** | Set Fine Rates | Sprint 3 | 08/07 | 09/07 | 🟢 Simple |

---

### 👤 Member 5: Phạm Kiến Quốc (CE201286)
> [!NOTE]
> **Module:** 💰 Financial, Fines & Account Maintenance
> **File chính:** `FinancialController`, `MemberMgmtController` (UC-14.1/14.3/14.4), + code UC-20.5/20.6 trong `AccountController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-8.5** | View Top-up Notifications | Sprint 1 | 22/06 | 23/06 | 🟢 Simple |
| 2 | **UC-8.4** | View Transaction History | Sprint 1 | 23/06 | 25/06 | 🟡 Medium |
| 3 | **UC-14.1** | View Member List | Sprint 1 | 24/06 | 25/06 | 🟢 Simple |
| 4 | **UC-21.2** | Reset Password ⚡ | Sprint 1 | 25/06 | 26/06 | 🟢 Simple |
| 5 | **UC-14.3** | View Transaction History | Sprint 2 | 28/06 | 30/06 | 🟡 Medium |
| 6 | **UC-21.1** | Change Account Status ⚡ | Sprint 2 | 29/06 | 01/07 | 🟡 Medium |
| 7 | **UC-8.2** | Pay Borrowing Fees | Sprint 2 | 30/06 | 02/07 | 🔴 Complex |
| 8 | **UC-8.1** | Pay Overdue Fines | Sprint 2 | 01/07 | 03/07 | 🔴 Complex |
| 9 | **UC-8.3** | Deposit payment | Sprint 3 | 05/07 | 07/07 | 🔴 Complex |
| 10 | **UC-14.4** | Top Up Member Account | Sprint 3 | 06/07 | 08/07 | 🔴 Complex |
| 11 | **UC-22.4** | Configure Payment Settings | Sprint 3 | 07/07 | 08/07 | 🟢 Simple |

---

### 👤 Member 6: Trần Ngọc Linh Đang (CE191088)
> [!NOTE]
> **Module:** ⚙️ Admin Dashboard, Accounts & System
> **File chính:** `DashboardController`, `AccountController` (UC-20.1-20.4), `SystemMgmtController`, `SettingsController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-18.1** | Member Accounts Management | Sprint 1 | 22/06 | 24/06 | 🟡 Medium |
| 2 | **UC-18.2** | View Librarian List | Sprint 1 | 23/06 | 24/06 | 🟢 Simple |
| 3 | **UC-20.4** | Search Accounts | Sprint 1 | 24/06 | 26/06 | 🟡 Medium |
| 4 | **UC-20.1** | Create Account | Sprint 1 | 25/06 | 27/06 | 🔴 Complex |
| 5 | **UC-20.2** | Update Account | Sprint 2 | 28/06 | 30/06 | 🟡 Medium |
| 6 | **UC-20.3** | Delete Account | Sprint 2 | 29/06 | 30/06 | 🟢 Simple |
| 7 | **UC-19.1** | Backup Data | Sprint 2 | 30/06 | 02/07 | 🔴 Complex |
| 8 | **UC-19.2** | Restore Data | Sprint 2 | 01/07 | 03/07 | 🔴 Complex |
| 9 | **UC-19.3** | View System Logs | Sprint 3 | 05/07 | 07/07 | 🟡 Medium |
| 10 | **UC-22.1** | Manage Borrowing and Return Policies | Sprint 3 | 06/07 | 08/07 | 🔴 Complex |

---

## 📊 SO SÁNH TRƯỚC VÀ SAU CÂN BẰNG

| Member | **Trước** (S/M/C) | Điểm | ➡️ | **Sau** (S/M/C) | Điểm | Thay đổi |
|:---:|:---|:---:|:---:|:---|:---:|:---|
| 1 - Thương | 6/6/**0** | 18 | ➡️ | 5/5/**2** | 21 | Bổ sung UC-22.3 ✅ |
| 2 - Khanh | 1/5/**3** | 20 | ➡️ | 2/5/**3** | 21 | Bổ sung UC-22.5 ✅ |
| 3 - Hưng | 2/2/**6** | 24 | ➡️ | 3/4/**4** | 23 | −2 Complex ✅ |
| 4 - Quốc Anh | 2/7/**1** | 19 | ➡️ | 3/7/**2** | 23 | Bổ sung UC-22.2 ✅ |
| 5 - Kiến Quốc | 2/2/**5** | 21 | ➡️ | 4/3/**4** | 22 | Bổ sung UC-22.4 ✅ |
| 6 - Linh Đang | 3/5/**4** (13 UC) | 25 | ➡️ | 2/4/**4** (10 UC) | 22 | Lấy lại UC-19.3 ✅ |

<br>

> [!TIP]
> **Cách trình bày cho Giảng viên:**
> Bạn có thể trực tiếp export file markdown này ra định dạng PDF hoặc copy dán vào Word. Các icon và cấu trúc màu sắc sẽ giúp báo cáo của nhóm trông cực kỳ chuyên nghiệp và thu hút.


## 📂 HƯỚNG DẪN CẤU TRÚC THƯ MỤC DỰ ÁN (DIRECTORY STRUCTURE)

Dưới đây là giải thích chi tiết về vai trò của từng thư mục hiện tại trong dự án, giúp các thành viên nhóm hiểu rõ cấu trúc và biết chính xác vị trí cần viết code:

### 1. `src/main/java/com/lms/` (Mã nguồn Backend - Java)
Đây là khu vực chứa toàn bộ logic xử lý của hệ thống.
*   **`entity/`**: Chứa các lớp thực thể ánh xạ trực tiếp với các bảng trong cơ sở dữ liệu SQL Server (ví dụ: `Book`, `Member`). **Hạn chế sửa đổi** trừ khi có thay đổi Database.
*   **`repository/`**: Chứa các giao diện (Interface) dùng để tương tác (Thêm, Sửa, Xóa, Tìm kiếm) với Database bằng Spring Data JPA.
*   **`dto/` (Data Transfer Objects)**: Chứa các class chuyên dùng để bọc dữ liệu truyền qua lại giữa Web và Server (ví dụ: `LoginRequest`, `ApiResponse`). Giúp bảo mật thông tin, không làm lộ cấu trúc Database thật.
*   **`exception/`**: Nơi chứa `GlobalExceptionHandler` và các class lỗi tùy chỉnh (vd: `ResourceNotFoundException`). Dùng để bắt lỗi tập trung trên toàn hệ thống và tự động trả về trang báo lỗi thân thiện.
*   **`enums/`**: Chứa các tập hợp hằng số cố định (ví dụ: `Role`, `BookStatus`, `LoanStatus`). Giúp chuẩn hóa các trạng thái, tránh gõ sai chính tả so với việc dùng String thông thường.
*   **`service/`**: Trái tim của ứng dụng. Đây là nơi bắt buộc phải chứa **toàn bộ logic nghiệp vụ (Business Logic)** như: tính toán tiền phạt, kiểm tra điều kiện mượn sách, xác thực tài khoản.
*   **`controller/`**: Nơi tiếp nhận các Request từ đường dẫn URL trên trình duyệt. Nhiệm vụ của nó rất nhẹ: gọi Service để xử lý logic, sau đó quyết định trả về giao diện HTML nào. Đã được chia rõ theo role: `admin/`, `librarian/`, `member/`, `auth/`.
*   **`config/`**: Chứa các file cấu hình hệ thống như `SecurityConfig` (Phân quyền đăng nhập).

### 2. `src/main/resources/` (Tài nguyên và Giao diện - Frontend)
Khu vực cấu hình hệ thống, chứa CSS/JS và các giao diện HTML.
*   **`application-dev.properties`**: File thông số kết nối Database, cấu hình gửi Email, và cấu hình Cloudinary lưu ảnh.
*   **`static/`**: Chứa các tài nguyên tĩnh như hình ảnh, file `.css` custom, và file `.js` tự viết.
*   **`templates/`**: Chứa toàn bộ các file giao diện HTML (sử dụng Thymeleaf).
    *   **`layout/base.html`**: Bộ khung giao diện gốc (Master Layout). Nó cấu hình sẵn thẻ `<head>`, bao bọc toàn bộ nội dung, tự động nạp Header, Sidebar, Footer.
    *   **`fragments/`**: Chứa các mảnh ghép HTML dùng chung như `header.html` (Thanh điều hướng ngang), `sidebar.html` (Menu dọc bên trái), `footer.html`.
    *   **`error/`**: Chứa các trang báo lỗi (400, 404, 500) đã được trang trí sẵn.
    *   **`admin/`, `librarian/`, `member/`**: Nơi các thành viên sẽ viết giao diện cho từng tính năng cụ thể. Các trang này sẽ rất ngắn gọn vì chỉ cần tập trung viết nội dung chính, phần khung đã được `layout/base.html` tự động đắp vào.
