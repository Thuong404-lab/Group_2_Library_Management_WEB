# 📚 Library Management System

> Software Project (SWP391) – Group G2

A comprehensive web-based Library Management System developed using Spring Boot and SQL Server. The system supports book inventory management, borrowing and returning books, membership management, fines, reservations, notifications, and administrative operations.

---

## 🎯 Project Objectives

The Library Management System aims to:

* Digitize library operations
* Manage books and inventory efficiently
* Support borrowing, returning, and reservation workflows
* Manage membership levels and transactions
* Automate fines and notifications
* Provide reporting and statistical analysis
* Ensure security through role-based access control

---

## 🚀 Main Features

### 👤 User & Authentication

* User Registration
* Login / Logout
* Forgot Password
* Profile Management
* Change Password
* Role-Based Authorization

### 📚 Book Management

* View Book List
* Search Books
* Filter by Author, Category, Genre, Language
* View Book Details
* Book Inventory Management

### 📖 Borrowing Management

* Borrow Books
* Return Books
* Loan Extension
* Borrowing History
* Loan Status Tracking

### 💰 Financial Management

* Membership Fee Collection
* Fine Calculation
* Damage Compensation
* Member Wallet Transactions

### 🔔 Notification System

* Email Notifications
* Reservation Notifications
* Due Date Reminders
* System Announcements

### ⭐ Review & Rating

* Book Reviews
* Book Ratings
* Review Management

### 📊 Reporting

* Revenue Reports
* Borrowing Reports
* Membership Reports
* Inventory Reports

### ⚙️ Administration

* User Management
* Role Management
* System Settings
* Database Backup

---

## 👥 Team Members

| Member               | Roll Number | Responsibilities                              |
| -------------------- | ----------- | --------------------------------------------- |
| Nguyễn Tiến Thương   | CE191329    | Authentication, Profile, Security             |
| La Tấn Khanh         | CE191640    | Inventory, Storage, System Settings           |
| Huỳnh Gia Hưng       | CE190488    | Borrowing, Loan Management, Membership        |
| Trần Nguyễn Quốc Anh | CE191655    | Services, Notifications, Reports              |
| Phạm Kiến Quốc       | CE201286    | Financial Management, Fines, Transactions     |
| Trần Ngọc Linh Đang  | CE191088    | Admin Accounts, System Administration, Backup |

---

## 🏗️ Technology Stack

| Layer           | Technology                 |
| --------------- | -------------------------- |
| Backend         | Java 21, Spring Boot 3.x   |
| Frontend        | Thymeleaf, Bootstrap 5     |
| Database        | SQL Server (MSSQL)         |
| ORM             | Spring Data JPA, Hibernate |
| Security        | Spring Security 6          |
| Build Tool      | Maven                      |
| Cloud Storage   | Cloudinary                 |
| Email Service   | Spring Mail                |
| PDF Export      | iText 7                    |
| Version Control | Git & GitHub               |

---

## 🏛️ System Architecture

```text
Client Browser
      │
      ▼
Thymeleaf Views
      │
      ▼
Spring MVC Controllers
      │
      ▼
Service Layer
      │
      ▼
Repository Layer (JPA)
      │
      ▼
SQL Server Database
```

---

## 🚀 Installation Guide

### Requirements

* Java 21+
* Maven 3.8+
* SQL Server 2019+
* SQL Server Management Studio
* IntelliJ IDEA

---

### Step 1: Clone Repository

```bash
git clone https://github.com/<org>/Library_Management_Web.git
cd Library_Management_Web
```

---

### Step 2: Create Database

Execute:

```text
database/LibraryManagementSystem_FIXED.sql
```

using SQL Server Management Studio.

---

### Step 3: Configure Application

Create:

```bash
cp src/main/resources/application-dev.properties.example \
src/main/resources/application-dev.properties
```

Update:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=LibraryManagementSystem;encrypt=false
spring.datasource.username=sa
spring.datasource.password=YOUR_PASSWORD

cloudinary.cloud-name=YOUR_CLOUD_NAME
cloudinary.api-key=YOUR_API_KEY
cloudinary.api-secret=YOUR_API_SECRET

spring.mail.username=YOUR_GMAIL
spring.mail.password=YOUR_APP_PASSWORD
```

---

### Step 4: Run Application

```bash
mvn spring-boot:run
```

Access:

```text
http://localhost:8080
```

---

## 📁 Project Structure

```text
Library_Management_Web/
├── database/
│   └── LibraryManagementSystem_FIXED.sql
│
├── src/
│   └── main/
│       ├── java/com/lms/
│       │   ├── config/
│       │   ├── controller/
│       │   │   ├── auth/
│       │   │   ├── member/
│       │   │   ├── librarian/
│       │   │   └── admin/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── entity/
│       │   ├── dto/
│       │   ├── enums/
│       │   ├── exception/
│       │   └── util/
│       │
│       └── resources/
│           ├── static/
│           ├── templates/
│           └── application.properties
│
└── pom.xml
```

---

## 📋 Business Rules

| ID    | Description                                        |
| ----- | -------------------------------------------------- |
| BR-3  | Only Active Members can borrow books               |
| BR-4  | Borrowing fee must be paid before borrowing        |
| BR-6  | Extension is allowed only if no reservation exists |
| BR-8  | Fine = Overdue Days × 5,000 VND                    |
| BR-10 | Compensation = 120,000 VND when damage > 50%       |
| BR-11 | Account locked after 3 overdue violations          |
| BR-15 | Notify member when reserved book becomes available |
| BR-16 | Review only after borrowing the book               |
| BR-17 | One rating per member per book                     |
| BR-22 | Liquidate book if condition < 40%                  |
| BR-31 | Loyal Member if spending ≥ 1,000,000 VND           |
| BR-32 | Loyal Member pays only 50% borrowing fee           |
| BR-37 | Wallet top-up only through librarian               |
| BR-41 | Reservation deposit = 50% book value               |

---

## 🔐 Authorization Matrix

| URL Pattern   | Access Role |
| ------------- | ----------- |
| /             | Public      |
| /books/**     | Public      |
| /search       | Public      |
| /auth/**      | Public      |
| /member/**    | MEMBER      |
| /librarian/** | LIBRARIAN   |
| /admin/**     | ADMIN       |

---

## 🌿 Git Flow Strategy

```text
main
 └── develop
      ├── feature/member1-auth
      ├── feature/member2-book
      ├── feature/member3-loan
      ├── feature/member4-finance
      ├── feature/member5-services
      └── feature/member6-admin
```

Workflow:

1. Create feature branch from develop
2. Implement feature
3. Commit frequently
4. Create Pull Request
5. Code Review
6. Merge into develop
7. Release develop to main

---

## 📅 Sprint Planning

| Sprint   | Deliverables                          |
| -------- | ------------------------------------- |
| Sprint 0 | Project Setup, Architecture, Database |
| Sprint 1 | Core Modules Development              |
| Sprint 2 | Advanced Features                     |
| Sprint 3 | Integration & Testing                 |
| Sprint 4 | Bug Fixing & Deployment               |

---

## 🧪 Testing

* Unit Testing
* Integration Testing
* Manual Functional Testing
* Security Testing
* User Acceptance Testing (UAT)

---

## 📌 Important Notes

* Do not commit `application-dev.properties`
* Do not commit `target/`
* Do not commit `.class` files
* Follow coding conventions
* Use Pull Requests for all merges
* LOC is counted only from Java files inside `src/main/java`

---

## 📄 License

This project was developed for educational purposes as part of SWP391 at FPT University.
