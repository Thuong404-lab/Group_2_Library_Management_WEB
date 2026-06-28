# 📚 Library Management Web (LMW)

![Java](https://img.shields.io/badge/Java-21-blue?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?logo=spring-boot)
![SQL Server](https://img.shields.io/badge/SQL_Server-2019+-red?logo=microsoft-sql-server)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-HTML5-green)

A professional, web-based Library Management Web developed for the SWP391 course. This system streamlines library operations, including book inventory, member borrowing/returning cycles, financial tracking (fines/deposits), and comprehensive reporting.

---

## ✨ Key Features

* **Authentication & Roles:** Secure login with role-based access control (Admin, Librarian, Member, Guest).
* **Catalog & Inventory:** Browse, search, and manage books, categories, and physical storage locations.
* **Borrowing Cycle:** Seamless process for reserving, borrowing, extending, and returning books.
* **Financial Module:** Automated calculation of late fines, damage compensation, and member wallet top-ups.
* **Engagement:** Book reviews, ratings, and email/system notifications.
* **Administration:** Detailed statistical reports, user management, and system data backups.

## 🛠️ Technology Stack

* **Backend:** Java 17, Spring Boot 3, Spring Security, Spring Data JPA (Hibernate)
* **Frontend:** Thymeleaf, Bootstrap 5, Vanilla JS/CSS
* **Database:** Microsoft SQL Server
* **Integrations:** Cloudinary (Images), Spring Mail (Notifications), iText 7 (PDF Export)

---

## Getting Started

Follow these steps to set up the project locally.

### 1. Prerequisites
* **Java 17** or higher
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
