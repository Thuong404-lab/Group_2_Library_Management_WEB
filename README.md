# Library Management Web

A web-based library management system for administrators, librarians, and members.

## Key Features

- Account, role, catalog, and inventory management.
- Book reservations, borrowing, renewals, and returns.
- Fine management, payments, and PayOS reconciliation.
- Book reviews, notifications, reports, and PDF export.

## Technology Stack

- Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA
- Thymeleaf, Bootstrap 5, JavaScript
- Microsoft SQL Server
- Cloudinary, Spring Mail, PayOS, iText

## Setup

Requirements: Java 17 and SQL Server 2019 or later.

1. Initialize the database with `database/LMW.sql`.
2. Apply the required migration scripts from the `database` directory.
3. Configure the database connection and service credentials in `src/main/resources/application-dev.properties`.
4. Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8081` in your browser.

## Testing

```powershell
.\mvnw.cmd test
```

Do not commit passwords or API keys to the repository.
