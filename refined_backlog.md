# 📚 BẢN PHÂN CÔNG NHIỆM VỤ & PRODUCT BACKLOG (REFINED)
**Dự án:** Library Management Web | **Mô hình:** Agile / Scrum | **Nhóm:** G2 (6 Thành viên)

> [!IMPORTANT]
> **Lưu ý các thay đổi so với bản gốc để hợp lý hóa logic nghiệp vụ:**
> 1. Chuyển cụm `UC-21 System Settings` từ **La Tấn Khanh** sang **Trần Ngọc Linh Đang** (Do Admin / System Manager phải là người nắm quyền cấu hình Setting hệ thống).
> 2. Lược bỏ `UC-21.5 Manage Categories` do bị trùng lặp chức năng hoàn toàn với `UC-12.6 Manage Categories & Genres` của La Tấn Khanh.
> 3. Đổi tên `UC-18.1` thành `View Admin Dashboard` để tránh trùng lặp ý nghĩa với cụm Account Management.
> 4. Làm rõ tên `UC-8.3 Pay Reservation Deposit` và `UC-15.2 Respond to Book Acquisition Requests`.

---

## 👥 TỔNG QUAN PHÂN CÔNG (~12,220 LOC)

| Member | Trạng thái | Họ và Tên | Roll Number | Role Module | Tổng số UC | LOC Dự kiến |
|:---:|:---:|:---|:---|:---|:---:|:---:|
| **1** | 🟢 Sẵn sàng | **Nguyễn Tiến Thương** | CE191329 | 🔐 Auth, Profile & Security | **12** | ~2,010 |
| **2** | 🟢 Sẵn sàng | **La Tấn Khanh** | CE191640 | 📦 Book Inventory & Storage | **9** | ~2,050 |
| **3** | 🟢 Sẵn sàng | **Huỳnh Gia Hưng** | CE190488 | 📚 Borrowing, Loan & Membership| **10** | ~2,030 |
| **4** | 🟢 Sẵn sàng | **Trần Nguyễn Quốc Anh** | CE191655 | 💬 Services, Interactions, Reports| **10** | ~2,010 |
| **5** | 🟢 Sẵn sàng | **Phạm Kiến Quốc** | CE201286 | 💰 Financial & Fines Management | **9** | ~2,080 |
| **6** | 🟢 Sẵn sàng | **Trần Ngọc Linh Đang** | CE191088 | ⚙️ Admin, Accounts & Settings | **16** | ~2,040 |

---

## 📋 CHI TIẾT PRODUCT BACKLOG THEO TỪNG THÀNH VIÊN

### 👤 Member 1: Nguyễn Tiến Thương (CE191329)
> [!NOTE]
> **Module Phụ Trách:** Authentication & Profile (Quản lý Xác thực & Hồ sơ)

| Mã UC | Tên Use Case | Phân hệ (Feature) | Độ khó |
|:---|:---|:---|:---:|
| **UC-1** | Search books | Guest View | 🟢 Simple |
| **UC-2** | Register | Authentication | 🟡 Medium |
| **UC-3** | View Book List | Guest View | 🟢 Simple |
| **UC-9** | Login | Authentication | 🟡 Medium |
| **UC-10**| Logout | Authentication | 🟢 Simple |
| **UC-4.1** | View Profile | Profile Management | 🟢 Simple |
| **UC-4.2** | Update Profile Information | Profile Management | 🟡 Medium |
| **UC-4.3** | Change Password | Profile Management | 🟡 Medium |
| **UC-4.4** | View Favorites List | Profile Management | 🟢 Simple |
| **UC-16.1**| View Librarian Profile | Librarian Profile | 🟢 Simple |
| **UC-16.2**| Edit Librarian Profile | Librarian Profile | 🟡 Medium |
| **UC-16.3**| Change Librarian Password | Librarian Profile | 🟡 Medium |

---

### 👤 Member 2: La Tấn Khanh (CE191640)
> [!NOTE]
> **Module Phụ Trách:** Book Inventory & Storage (Quản lý Kho Sách & Vị trí Lưu trữ)

| Mã UC | Tên Use Case | Phân hệ (Feature) | Độ khó |
|:---|:---|:---|:---:|
| **UC-11.1**| Update Storage Location | Book Storage | 🟡 Medium |
| **UC-11.2**| Add Storage Location | Book Storage | 🟡 Medium |
| **UC-11.3**| Remove Storage Location | Book Storage | 🟢 Simple |
| **UC-12.1**| Perform Periodic Inventory Audit | Book Inventory | 🔴 Complex |
| **UC-12.2**| Update Book Status | Book Inventory | 🟡 Medium |
| **UC-12.3**| Add New Books | Book Inventory | 🔴 Complex |
| **UC-12.4**| Update Book | Book Inventory | 🔴 Complex |
| **UC-12.5**| Remove Books | Book Inventory | 🟡 Medium |
| **UC-12.6**| Manage Categories & Genres | Book Inventory | 🟡 Medium |

---

### 👤 Member 3: Huỳnh Gia Hưng (CE190488)
> [!NOTE]
> **Module Phụ Trách:** Borrowing, Loan & Membership Tier (Quản lý Mượn trả & Hạng Thành viên)

| Mã UC | Tên Use Case | Phân hệ (Feature) | Độ khó |
|:---|:---|:---|:---:|
| **UC-5.1** | View Benefits & Privileges | Membership | 🟢 Simple |
| **UC-5.2** | View Membership Tier | Membership | 🟢 Simple |
| **UC-6.1** | View Borrowing History | Borrowing Management | 🟡 Medium |
| **UC-6.2** | Reserve Books | Borrowing Management | 🔴 Complex |
| **UC-6.3** | Borrow Book | Borrowing Management | 🔴 Complex |
| **UC-6.4** | Return Books | Borrowing Management | 🔴 Complex |
| **UC-13.1**| View Loan Details | Loan Management | 🟡 Medium |
| **UC-13.2**| Confirm Book Returns | Loan Management | 🔴 Complex |
| **UC-13.3**| Process Borrow Requests | Loan Management | 🔴 Complex |
| **UC-13.4**| Process Renewal Requests | Loan Management | 🔴 Complex |

---

### 👤 Member 4: Trần Nguyễn Quốc Anh (CE191655)
> [!NOTE]
> **Module Phụ Trách:** Services, Interaction & Reports (Dịch vụ thư viện, Tương tác & Báo cáo)

| Mã UC | Tên Use Case | Phân hệ (Feature) | Độ khó |
|:---|:---|:---|:---:|
| **UC-7.1** | Add to Favorites | Library Services | 🟢 Simple |
| **UC-7.2** | Suggest New Books | Library Services | 🟡 Medium |
| **UC-7.3** | Rate & Review Books | Library Services | 🟡 Medium |
| **UC-7.4** | View Notification | Library Services | 🟢 Simple |
| **UC-15.1**| Send Notifications to Members | Interaction | 🟡 Medium |
| **UC-15.2**| Respond to Book Acquisition Requests| Interaction | 🟡 Medium |
| **UC-15.3**| Moderate Reviews & Comments | Interaction | 🟡 Medium |
| **UC-17.1**| Generate Reports | Reporting | 🟡 Medium |
| **UC-22.1**| Generate Revenue Report | Reports and Statistics | 🔴 Complex |
| **UC-22.2**| Export Report | Reports and Statistics | 🟡 Medium |

---

### 👤 Member 5: Phạm Kiến Quốc (CE201286)
> [!NOTE]
> **Module Phụ Trách:** Financial & Fines Management (Tài chính & Quản lý Vi phạm)

| Mã UC | Tên Use Case | Phân hệ (Feature) | Độ khó |
|:---|:---|:---|:---:|
| **UC-8.1** | Pay Overdue Fines | Financial Transactions | 🔴 Complex |
| **UC-8.2** | Pay Borrowing Fees | Financial Transactions | 🔴 Complex |
| **UC-8.3** | Pay Reservation Deposit | Financial Transactions | 🔴 Complex |
| **UC-8.4** | View Transaction History | Financial Transactions | 🟡 Medium |
| **UC-8.5** | View Top-up Notifications | Financial Transactions | 🟢 Simple |
| **UC-14.1**| View Member List | Member & Financial Mgmt | 🟢 Simple |
| **UC-14.2**| Manage Fines & Violations | Member & Financial Mgmt | 🔴 Complex |
| **UC-14.3**| View Transaction History | Member & Financial Mgmt | 🟡 Medium |
| **UC-14.4**| Top Up Member Account | Member & Financial Mgmt | 🔴 Complex |

---

### 👤 Member 6: Trần Ngọc Linh Đang (CE191088)
> [!NOTE]
> **Module Phụ Trách:** Admin, Accounts & Settings (Quản trị Hệ thống, Tài khoản & Cấu hình)

| Mã UC | Tên Use Case | Phân hệ (Feature) | Độ khó |
|:---|:---|:---|:---:|
| **UC-18.1**| View Admin Dashboard | System Admin | 🟡 Medium |
| **UC-18.2**| View Librarian List | System Admin | 🟢 Simple |
| **UC-18.3**| View System Logs | System Admin | 🟡 Medium |
| **UC-19.1**| Backup Data | System Management | 🔴 Complex |
| **UC-19.2**| Restore Data | System Management | 🔴 Complex |
| **UC-19.3**| View System Logs | System Management | 🟡 Medium |
| **UC-20.1**| Create Account | Accounts Management | 🔴 Complex |
| **UC-20.2**| Update Account | Accounts Management | 🟡 Medium |
| **UC-20.3**| Delete Account | Accounts Management | 🟢 Simple |
| **UC-20.4**| Search Accounts | Accounts Management | 🟡 Medium |
| **UC-20.5**| Change Account Status | Accounts Management | 🟡 Medium |
| **UC-20.6**| Reset Password | Accounts Management | 🟢 Simple |
| **UC-21.1**| Manage Borrowing/Return Policies| System Settings | 🔴 Complex |
| **UC-21.2**| Set Fine Rates | System Settings | 🟡 Medium |
| **UC-21.3**| Membership Tier Management | System Settings | 🟡 Medium |
| **UC-21.4**| Configure Payment Settings | System Settings | 🔴 Complex |

<br>

> [!TIP]
> **Cách trình bày cho Giảng viên:** 
> Bạn có thể trực tiếp export file markdown này ra định dạng PDF hoặc copy dán vào Word. Các icon và cấu trúc màu sắc sẽ giúp báo cáo của nhóm trông cực kỳ chuyên nghiệp và thu hút.
