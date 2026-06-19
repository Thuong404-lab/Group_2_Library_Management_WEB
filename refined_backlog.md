# 📚 BẢN PHÂN CÔNG NHIỆM VỤ & PRODUCT BACKLOG (REFINED v2)
**Dự án:** Library Management Web | **Mô hình:** Agile / Scrum | **Nhóm:** G2 (6 Thành viên)
**Ngày bắt đầu:** 22/06/2026 | **Mỗi Task:** 3 ngày

> [!IMPORTANT]
> **Các thay đổi so với bản v1 để CÂN BẰNG ĐỘ KHÓ:**
> 1. ~~UC-16 (Librarian Profile)~~ chuyển từ **Member 1** → **Member 3** (vì Member 3 quản lý module Librarian/Loan).
> 2. **UC-13.3 + UC-13.4** (Process Borrow & Renewal — Complex) chuyển từ **Member 3** → **Member 1** (giảm tải Complex cho Member 3, tăng cho Member 1).
> 3. **UC-19.3** (View System Logs) chuyển từ **Member 6** → **Member 1** (giảm số lượng UC cho Member 6).
> 4. **UC-14.2** (Manage Fines — Complex) chuyển từ **Member 5** → **Member 4** (tăng Complex cho Member 4).
> 5. **UC-20.5 + UC-20.6** (Change Status & Reset Password) chuyển từ **Member 6** → **Member 5** (giảm UC cho Member 6, bù UC cho Member 5).
> 6. Loại bỏ cụm `UC-19` khỏi Member 1 (Thương) do trùng lặp với Member 6 → chỉ giữ lại UC-19.3 cho Member 1.

---

## 📅 TỔNG QUAN SPRINT

| Sprint | Thời gian | Số ngày | Mục tiêu chính |
|:---:|:---|:---:|:---|
| **Sprint 1** | 22/06/2026 – 05/07/2026 | 14 ngày | 🟢 Nền tảng: Auth, View cơ bản, Setup CRUD đơn giản |
| **Sprint 2** | 06/07/2026 – 19/07/2026 | 14 ngày | 🟡 Tính năng cốt lõi: Mượn/Trả, Thanh toán, Quản lý sách |
| **Sprint 3** | 20/07/2026 – 05/08/2026 | 17 ngày | 🔴 Nâng cao: Backup, Báo cáo, Loan Processing, Polish |

---

## 👥 TỔNG QUAN PHÂN CÔNG (CÂN BẰNG)

| Member | Họ và Tên | Roll Number | Role Module | UC | 🟢S | 🟡M | 🔴C | Điểm |
|:---:|:---|:---|:---|:---:|:---:|:---:|:---:|:---:|
| **1** | **Nguyễn Tiến Thương** | CE191329 | 🔐 Auth, Profile & Loan Processing | **12** | 5 | 5 | **2** | 21 |
| **2** | **La Tấn Khanh** | CE191640 | 📦 Book Inventory & Storage | **9** | 1 | 5 | **3** | 20 |
| **3** | **Huỳnh Gia Hưng** | CE190488 | 📚 Borrowing, Membership & Librarian Profile | **11** | 3 | 4 | **4** | 23 |
| **4** | **Trần Nguyễn Quốc Anh** | CE191655 | 💬 Services, Interactions & Moderation | **11** | 2 | 7 | **2** | 22 |
| **5** | **Phạm Kiến Quốc** | CE201286 | 💰 Financial, Fines & Account Maintenance | **10** | 3 | 3 | **4** | 21 |
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
| 2 | **UC-10** | Logout | Sprint 1 | 25/06 | 27/06 | 🟢 Simple |
| 3 | **UC-2** | Register | Sprint 1 | 28/06 | 30/06 | 🟡 Medium |
| 4 | **UC-1** | Search Books | Sprint 1 | 01/07 | 03/07 | 🟢 Simple |
| 5 | **UC-3** | View Book List | Sprint 2 | 07/07 | 09/07 | 🟢 Simple |
| 6 | **UC-4.1** | View Profile | Sprint 2 | 10/07 | 12/07 | 🟢 Simple |
| 7 | **UC-4.2** | Update Profile Information | Sprint 2 | 13/07 | 15/07 | 🟡 Medium |
| 8 | **UC-4.3** | Change Password | Sprint 2 | 16/07 | 18/07 | 🟡 Medium |
| 9 | **UC-4.4** | View Favorites List | Sprint 3 | 20/07 | 22/07 | 🟢 Simple |
| 10 | **UC-9.1** | Manage Active Sessions & Session Logs | Sprint 3 | 23/07 | 25/07 | 🟡 Medium |
| 11 | **UC-13.3** | Process Borrow Requests ⚡ | Sprint 3 | 26/07 | 28/07 | 🔴 Complex |
| 12 | **UC-13.4** | Process Renewal Requests ⚡ | Sprint 3 | 29/07 | 31/07 | 🔴 Complex |

---

### 👤 Member 2: La Tấn Khanh (CE191640)
> [!NOTE]
> **Module:** 📦 Book Inventory & Storage
> **File chính:** `StorageController`, `InventoryController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-12.6** | Manage Categories & Genres | Sprint 1 | 22/06 | 24/06 | 🟡 Medium |
| 2 | **UC-11.2** | Add Storage Location | Sprint 1 | 25/06 | 27/06 | 🟡 Medium |
| 3 | **UC-11.1** | Update Storage Location | Sprint 1 | 28/06 | 30/06 | 🟡 Medium |
| 4 | **UC-11.3** | Remove Storage Location | Sprint 1 | 01/07 | 03/07 | 🟢 Simple |
| 5 | **UC-12.2** | Update Book Status | Sprint 2 | 07/07 | 09/07 | 🟡 Medium |
| 6 | **UC-12.5** | Remove Books | Sprint 2 | 10/07 | 12/07 | 🟡 Medium |
| 7 | **UC-12.3** | Add New Books | Sprint 2 | 13/07 | 15/07 | 🔴 Complex |
| 8 | **UC-12.4** | Update Book | Sprint 2 | 16/07 | 18/07 | 🔴 Complex |
| 9 | **UC-12.1** | Perform Periodic Inventory Audit | Sprint 3 | 20/07 | 22/07 | 🔴 Complex |

---

### 👤 Member 3: Huỳnh Gia Hưng (CE190488)
> [!NOTE]
> **Module:** 📚 Borrowing, Membership & Librarian Profile
> **File chính:** `MembershipController`, `BorrowController`, `LoanController` (UC-13.1/13.2), `LibrarianProfileController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-5.1** | View Benefits & Privileges | Sprint 1 | 22/06 | 24/06 | 🟢 Simple |
| 2 | **UC-5.2** | View Membership Tier | Sprint 1 | 25/06 | 27/06 | 🟢 Simple |
| 3 | **UC-16.1** | View Librarian Profile ⚡ | Sprint 1 | 28/06 | 30/06 | 🟢 Simple |
| 4 | **UC-6.1** | View Borrowing History | Sprint 1 | 01/07 | 03/07 | 🟡 Medium |
| 5 | **UC-16.2** | Edit Librarian Profile ⚡ | Sprint 2 | 07/07 | 09/07 | 🟡 Medium |
| 6 | **UC-16.3** | Change Librarian Password ⚡ | Sprint 2 | 10/07 | 12/07 | 🟡 Medium |
| 7 | **UC-13.1** | View Loan Details | Sprint 2 | 13/07 | 15/07 | 🟡 Medium |
| 8 | **UC-6.2** | Reserve Books | Sprint 2 | 16/07 | 18/07 | 🔴 Complex |
| 9 | **UC-6.3** | Borrow Book | Sprint 3 | 20/07 | 22/07 | 🔴 Complex |
| 10 | **UC-6.4** | Return Books | Sprint 3 | 23/07 | 25/07 | 🔴 Complex |
| 11 | **UC-13.2** | Confirm Book Returns | Sprint 3 | 26/07 | 28/07 | 🔴 Complex |

---

### 👤 Member 4: Trần Nguyễn Quốc Anh (CE191655)
> [!NOTE]
> **Module:** 💬 Services, Interactions & Moderation
> **File chính:** `LibraryServiceController`, `InteractionController`, `ReportController`, + code UC-14.2 trong `MemberMgmtController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-7.1** | Add to Favorites | Sprint 1 | 22/06 | 24/06 | 🟢 Simple |
| 2 | **UC-7.4** | View Notification | Sprint 1 | 25/06 | 27/06 | 🟢 Simple |
| 3 | **UC-7.2** | Suggest New Books | Sprint 1 | 28/06 | 30/06 | 🟡 Medium |
| 4 | **UC-7.3** | Rate & Review Books | Sprint 1 | 01/07 | 03/07 | 🟡 Medium |
| 5 | **UC-15.1** | Send Notifications to Members | Sprint 2 | 07/07 | 09/07 | 🟡 Medium |
| 6 | **UC-15.2** | Respond to Book Acquisition Requests | Sprint 2 | 10/07 | 12/07 | 🟡 Medium |
| 7 | **UC-15.3** | Moderate Reviews & Comments | Sprint 2 | 13/07 | 15/07 | 🟡 Medium |
| 8 | **UC-14.2** | Manage Fines & Violations ⚡ | Sprint 2 | 16/07 | 18/07 | 🔴 Complex |
| 9 | **UC-17.1** | Generate Reports | Sprint 3 | 20/07 | 22/07 | 🟡 Medium |
| 10 | **UC-22.1** | Generate Revenue Report | Sprint 3 | 23/07 | 25/07 | 🔴 Complex |
| 11 | **UC-22.2** | Export Report | Sprint 3 | 26/07 | 28/07 | 🟡 Medium |

---

### 👤 Member 5: Phạm Kiến Quốc (CE201286)
> [!NOTE]
> **Module:** 💰 Financial, Fines & Account Maintenance
> **File chính:** `FinancialController`, `MemberMgmtController` (UC-14.1/14.3/14.4), + code UC-20.5/20.6 trong `AccountController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-8.5** | View Top-up Notifications | Sprint 1 | 22/06 | 24/06 | 🟢 Simple |
| 2 | **UC-8.4** | View Transaction History | Sprint 1 | 25/06 | 27/06 | 🟡 Medium |
| 3 | **UC-14.1** | View Member List | Sprint 1 | 28/06 | 30/06 | 🟢 Simple |
| 4 | **UC-20.6** | Reset Password ⚡ | Sprint 1 | 01/07 | 03/07 | 🟢 Simple |
| 5 | **UC-14.3** | View Transaction History (Librarian) | Sprint 2 | 07/07 | 09/07 | 🟡 Medium |
| 6 | **UC-20.5** | Change Account Status ⚡ | Sprint 2 | 10/07 | 12/07 | 🟡 Medium |
| 7 | **UC-8.2** | Pay Borrowing Fees | Sprint 2 | 13/07 | 15/07 | 🔴 Complex |
| 8 | **UC-8.1** | Pay Overdue Fines | Sprint 2 | 16/07 | 18/07 | 🔴 Complex |
| 9 | **UC-8.3** | Pay Reservation Deposit | Sprint 3 | 20/07 | 22/07 | 🔴 Complex |
| 10 | **UC-14.4** | Top Up Member Account | Sprint 3 | 23/07 | 25/07 | 🔴 Complex |

---

### 👤 Member 6: Trần Ngọc Linh Đang (CE191088)
> [!NOTE]
> **Module:** ⚙️ Admin Dashboard, Accounts & System
> **File chính:** `DashboardController`, `AccountController` (UC-20.1-20.4), `SystemMgmtController`, `SettingsController`

| # | Mã UC | Tên Use Case | Sprint | Bắt đầu | Kết thúc | Độ khó |
|:---:|:---|:---|:---:|:---:|:---:|:---:|
| 1 | **UC-18.1** | View Admin Dashboard | Sprint 1 | 22/06 | 24/06 | 🟡 Medium |
| 2 | **UC-18.2** | View Librarian List | Sprint 1 | 25/06 | 27/06 | 🟢 Simple |
| 3 | **UC-18.3** | View System Logs | Sprint 1 | 28/06 | 30/06 | 🟡 Medium |
| 4 | **UC-20.4** | Search Accounts | Sprint 1 | 01/07 | 03/07 | 🟡 Medium |
| 5 | **UC-20.1** | Create Account | Sprint 2 | 07/07 | 09/07 | 🔴 Complex |
| 6 | **UC-20.2** | Update Account | Sprint 2 | 10/07 | 12/07 | 🟡 Medium |
| 7 | **UC-20.3** | Delete Account | Sprint 2 | 13/07 | 15/07 | 🟢 Simple |
| 8 | **UC-19.1** | Backup Data | Sprint 2 | 16/07 | 18/07 | 🔴 Complex |
| 9 | **UC-19.2** | Restore Data | Sprint 3 | 20/07 | 22/07 | 🔴 Complex |
| 10 | **UC-19.3** | View System Logs (Admin) | Sprint 3 | 23/07 | 25/07 | 🟡 Medium |
| 11 | **UC-21.1** | Manage Borrowing/Return Policies | Sprint 3 | 26/07 | 28/07 | 🔴 Complex |

---

## 📊 SO SÁNH TRƯỚC VÀ SAU CÂN BẰNG

| Member | **Trước** (S/M/C) | Điểm | ➡️ | **Sau** (S/M/C) | Điểm | Thay đổi |
|:---:|:---|:---:|:---:|:---|:---:|:---|
| 1 - Thương | 6/6/**0** | 18 | ➡️ | 5/5/**2** | 21 | Thay UC-19.3 thành UC-9.1 ✅ |
| 2 - Khanh | 1/5/**3** | 20 | ➡️ | 1/5/**3** | 20 | Không đổi |
| 3 - Hưng | 2/2/**6** | 24 | ➡️ | 3/4/**4** | 23 | −2 Complex ✅ |
| 4 - Quốc Anh | 2/7/**1** | 19 | ➡️ | 2/7/**2** | 22 | +1 Complex ✅ |
| 5 - Kiến Quốc | 2/2/**5** | 21 | ➡️ | 3/3/**4** | 21 | −1 Complex ✅ |
| 6 - Linh Đang | 3/5/**4** (13 UC) | 25 | ➡️ | 2/5/**4** (11 UC) | 24 | Lấy lại UC-19.3 ✅ |

<br>

> [!TIP]
> **Cách trình bày cho Giảng viên:**
> Bạn có thể trực tiếp export file markdown này ra định dạng PDF hoặc copy dán vào Word. Các icon và cấu trúc màu sắc sẽ giúp báo cáo của nhóm trông cực kỳ chuyên nghiệp và thu hút.
