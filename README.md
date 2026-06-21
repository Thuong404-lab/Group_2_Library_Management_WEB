# 📚 Library Management System (LMS)

![Java](https://img.shields.io/badge/Java-21-blue?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?logo=spring-boot)
![SQL Server](https://img.shields.io/badge/SQL_Server-2019+-red?logo=microsoft-sql-server)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-HTML5-green)

A professional, web-based Library Management System developed for the SWP391 course. This system streamlines library operations, including book inventory, member borrowing/returning cycles, financial tracking (fines/deposits), and comprehensive reporting.

---

## ✨ Key Features

* **Authentication & Roles:** Secure login with role-based access control (Admin, Librarian, Member, Guest).
* **Catalog & Inventory:** Browse, search, and manage books, categories, and physical storage locations.
* **Borrowing Cycle:** Seamless process for reserving, borrowing, extending, and returning books.
* **Financial Module:** Automated calculation of late fines, damage compensation, and member wallet top-ups.
* **Engagement:** Book reviews, ratings, and email/system notifications.
* **Administration:** Detailed statistical reports, user management, and system data backups.

## 🛠️ Technology Stack

* **Backend:** Java 21, Spring Boot 3, Spring Security, Spring Data JPA (Hibernate)
* **Frontend:** Thymeleaf, Bootstrap 5, Vanilla JS/CSS
* **Database:** Microsoft SQL Server
* **Integrations:** Cloudinary (Images), Spring Mail (Notifications), iText 7 (PDF Export)

---

## Getting Started

Follow these steps to set up the project locally.

### 1. Prerequisites
* **Java 21** or higher
* **Maven 3.8+**
* **SQL Server 2019+**

### 2. Database Setup
1. Open SQL Server Management Studio (SSMS).
2. Execute the script located at `database/LibraryManagementSystem_FIXED.sql` to generate the schema and mock data.

### 3. Environment Configuration
Duplicate the example configuration file:
```bash
cp src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties
```
Update `application-dev.properties` với thông tin Database, Cloudinary API keys, và Mail configurations của bạn.

### 4. Run the Application
```bash
mvn spring-boot:run
```
Truy cập ứng dụng tại: `http://localhost:8080`

---

## 👥 Team & Workflows

**Nhóm G2 (6 Thành viên):**
Để xem chi tiết phân công công việc, tiến độ các Sprints, và yêu cầu về số lượng dòng code (LOC), vui lòng tham khảo file [Refined Backlog](./refined_backlog.md).

**Quy trình Git Flow:**
1. Tạo branch mới từ nhánh `develop`: `feature/<tên-thành-viên>-<tên-task>`
2. Code và Commit.
3. Tạo **Pull Request (PR)** vào nhánh `develop` để Code Review.
4. Merge sau khi được approve.

>  **Lưu ý Quan Trọng:** Tuyệt đối không commit file `application-dev.properties`, thư mục `target/`, hoặc các file `.class` lên Git.

---
*Dự án được phát triển phục vụ mục đích học tập môn SWP391 - Đại học FPT.*
