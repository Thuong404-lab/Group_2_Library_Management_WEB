# 📋 BẢNG PHÂN CÔNG NHIỆM VỤ & KHỐI LƯỢNG CODE (LOC)
**Dự án**: Library Management Web  
**Mô hình làm việc**: Agile / Scrum  
**Quy mô nhóm**: 6 Thành viên

> [!IMPORTANT]
> **Quy tắc tính LOC (Lines of Code)**
> - **Chỉ tính**: Code Backend `.java` (Entity, Repository, Service, Controller, DTO, Config, Exception, Util, Enum).
> - **Không tính**: Giao diện HTML (Thymeleaf), CSS, JavaScript, hay file cấu hình `.properties`, `pom.xml`.
> - **Mục tiêu cốt lõi**: Mỗi thành viên đều phải đạt khối lượng code xấp xỉ **~2,000 LOC** để đảm bảo công bằng.

---

## 📊 BẢNG TỔNG HỢP CHUNG

| Member | Họ và Tên | Roll No | Module Phụ Trách Chính | LOC Mục tiêu | Trạng thái |
|:---:|:---|:---|:---|:---:|:---:|
| 1 | **Nguyễn Tiến Thương** | CE191329 | 🔐 Auth, Profile, Security | **~2,010** | 🟢 Sẵn sàng |
| 2 | **La Tấn Khanh** | CE191640 | 📦 Inventory, Storage, System Settings | **~2,120** | 🟢 Sẵn sàng |
| 3 | **Huỳnh Gia Hưng** | CE190488 | 📚 Borrowing, Loan Mgmt, Membership | **~2,030** | 🟢 Sẵn sàng |
| 4 | **Trần Nguyễn Quốc Anh** | CE191655 | 💬 Services, Interaction, Reports, Noti | **~2,010** | 🟢 Sẵn sàng |
| 5 | **Phạm Kiến Quốc** | CE201286 | 💰 Financial, Fines, Member Transactions | **~2,080** | 🟢 Sẵn sàng |
| 6 | **Trần Ngọc Linh Đang**| CE191088 | ⚙️ Admin Accounts, System Admin, Backup| **~2,040** | 🟢 Sẵn sàng |
| **Σ** | **Tổng Dự Án** | | | **~12,290 lines**| |

---

## 🎯 CHI TIẾT THEO TỪNG THÀNH VIÊN

### 👤 Member 1: Nguyễn Tiến Thương (CE191329)
> [!NOTE]
> **Use Case đảm nhận:** UC-1, UC-2, UC-3, UC-9, UC-10 (Guest/Auth) & UC-4.1 -> 4.4, UC-16.1 -> 16.3 (Profiles)

| Layer | Danh sách File tiêu biểu | Dự kiến LOC |
|:---|:---|:---:|
| 🗄️ **Entity** | `User.java`, `Role.java`, `MemberProfile.java` | ~380 |
| 🔗 **Repository** | `UserRepository.java`, `RoleRepository.java`... | ~130 |
| ⚙️ **Service** | `AuthService.java`, `UserService.java`, `ProfileService.java` | ~750 |
| 🎮 **Controller**| `LoginController.java`, `RegisterController.java`, `ProfileController.java` | ~450 |
| 🔒 **Config** | `SecurityConfig.java`, `CustomUserDetailsService.java` | ~250 |
| 📦 **DTO & Util** | Các record DTO Request/Response, Exception Global | ~50 |
| | **TỔNG CỘNG** | **~2,010** |

---

### 👤 Member 2: La Tấn Khanh (CE191640)
> [!NOTE]
> **Use Case đảm nhận:** UC-11.1 -> 11.3 (Storage), UC-12.1 -> 12.6 (Inventory), UC-21.1 -> 21.5 (System Settings)

| Layer | Danh sách File tiêu biểu | Dự kiến LOC |
|:---|:---|:---:|
| 🗄️ **Entity** | `Book.java`, `BookCopy.java`, `Category.java`, `Genre.java`, `StorageLocation.java`, `SystemSetting.java` | ~455 |
| 🏷️ **Enum** | `CopyStatus.java` | ~40 |
| 🔗 **Repository** | `BookRepository`, `BookCopyRepository`, `CategoryRepository`, `GenreRepository`, `StorageRepository` | ~215 |
| ⚙️ **Service** | `BookService.java`, `BookCopyService.java`, `GenreService.java`, `SystemSettingsService.java` | ~850 |
| 🎮 **Controller**| `InventoryController.java`, `SystemSettingsController.java` | ~450 |
| 📦 **DTO** | Record DTO cho Book, Genre, Storage, Setting | ~110 |
| | **TỔNG CỘNG** | **~2,120** |

---

### 👤 Member 3: Huỳnh Gia Hưng (CE190488)
> [!NOTE]
> **Use Case đảm nhận:** UC-6.1 -> 6.4 (Borrowing), UC-13.1 -> 13.4 (Loan), UC-5.1, UC-5.2 (Membership Tier)

| Layer | Danh sách File tiêu biểu | Dự kiến LOC |
|:---|:---|:---:|
| 🗄️ **Entity** | `LoanRecord.java`, `Reservation.java`, `LoanRenewal.java` | ~350 |
| 🏷️ **Enum** | `LoanStatus.java`, `ReservationStatus.java` | ~90 |
| 🔗 **Repository** | Các Interface Repository tương ứng | ~200 |
| ⚙️ **Service** | `LoanService.java`, `ReservationService.java`, `MembershipViewService.java` | ~800 |
| 🎮 **Controller**| `LoanController.java`, `ReservationController.java` | ~400 |
| 📦 **DTO & Util** | Các record DTO, helper `DateUtil.java` | ~190 |
| | **TỔNG CỘNG** | **~2,030** |

---

### 👤 Member 4: Trần Nguyễn Quốc Anh (CE191655)
> [!NOTE]
> **Use Case đảm nhận:** UC-7.1 -> 7.4 (Library Services), UC-15.1 -> 15.3 (Interaction), UC-17.1, UC-22.1, UC-22.2 (Reports)

| Layer | Danh sách File tiêu biểu | Dự kiến LOC |
|:---|:---|:---:|
| 🗄️ **Entity** | `Favorite.java`, `BookReview.java`, `Notification.java`, `BookAcquisitionRequest.java` | ~350 |
| 🏷️ **Enum** | `NotificationType.java` | ~35 |
| 🔗 **Repository** | Các Interface Repository tương ứng | ~235 |
| ⚙️ **Service** | `ReviewService.java`, `NotificationService.java`, `PdfExportService.java`, `ReportService.java` | ~900 |
| 🎮 **Controller**| `ReviewController.java`, `ReportController.java`, `NotificationController.java` | ~400 |
| 📦 **DTO & Config**| `MailConfig.java` và các payload DTO | ~90 |
| | **TỔNG CỘNG** | **~2,010** |

---

### 👤 Member 5: Phạm Kiến Quốc (CE201286)
> [!NOTE]
> **Use Case đảm nhận:** UC-8.1 -> 8.5 (Financial), UC-14.1 -> 14.4 (Member & Financial Mgmt)

| Layer | Danh sách File tiêu biểu | Dự kiến LOC |
|:---|:---|:---:|
| 🗄️ **Entity** | `Fine.java`, `Transaction.java`, `Violation.java` | ~300 |
| 🏷️ **Enum** | `TransactionType.java`, `FineStatus.java`, `FineType.java` | ~110 |
| 🔗 **Repository** | Các Interface Repository tương ứng | ~200 |
| ⚙️ **Service** | `FineService.java`, `TransactionService.java`, `ViolationService.java` | ~850 |
| 🎮 **Controller**| `PaymentController.java`, `MemberFinanceController.java` | ~450 |
| 📦 **DTO & Util** | Payload Payment, `PaginationUtil.java` | ~170 |
| | **TỔNG CỘNG** | **~2,080** |

---

### 👤 Member 6: Trần Ngọc Linh Đang (CE191088)
> [!NOTE]
> **Use Case đảm nhận:** UC-20.1 -> 20.6 (Accounts), UC-18.1 -> 18.3 (System Admin), UC-19.1 -> 19.3 (Backup)

| Layer | Danh sách File tiêu biểu | Dự kiến LOC |
|:---|:---|:---:|
| 🗄️ **Entity** | `SystemLog.java` | ~100 |
| 🔗 **Repository** | Các Interface Repository liên quan Admin/Log | ~100 |
| ⚙️ **Service** | `AdminUserService.java`, `BackupRestoreService.java`, `SystemLogService.java` | ~850 |
| 🎮 **Controller**| `AccountsManagementController.java`, `SystemManagementController.java` | ~750 |
| 📦 **DTO & Ex** | Các class Account Request/Response, `UnauthorizedException.java` | ~240 |
| | **TỔNG CỘNG** | **~2,040** |

---
> [!TIP]
> **Mẹo kỹ thuật để dễ dàng đạt số lượng LOC mục tiêu:**
> - Viết **Custom `@Query`** JPQL thay vì phụ thuộc hoàn toàn vào JPA Naming Conventions.
> - Xử lý **Pagination** & **Sorting** đầy đủ ở mọi bảng dữ liệu (List Book, List Member, List Transaction).
> - Map DTO thủ công thay vì dùng thư viện AutoMapper (tăng tính an toàn và LOC).
> - Bắt **Validation chặt chẽ** ở Controller (check null, check logic business).
