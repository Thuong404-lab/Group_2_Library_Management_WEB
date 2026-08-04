# Library Management Web

A web-based library management system for administrators, librarians, and members.

## Key Features

- Account, role, catalog, and inventory management.
- Book reservations, borrowing, renewals, and returns.
- Fine management, payments, and PayOS reconciliation.
- Book reviews, notifications, reports, and PDF export.

## Technology Stack

- Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA
- Thymeleaf, Bootstrap 5, JavaScript
- Microsoft SQL Server
- Cloudinary, Spring Mail, PayOS, iText

## Setup

Requirements: Java 17 and SQL Server 2019 or later.

1. Create an empty SQL Server database named `LibraryManagementWeb`.
2. Copy `src/main/resources/application-dev.properties.example` to
   `src/main/resources/application-dev.properties` and configure the database and service credentials.
3. Start the application. Flyway automatically creates and upgrades the schema from
   `src/main/resources/db/migration`:

```powershell
.\mvnw.cmd spring-boot:run
```

4. Optionally load the demonstration accounts and data by running
   `src/main/resources/db/seed/data.sql` after Flyway finishes.

Open `http://localhost:8080` in your browser.

## Testing

```powershell
.\mvnw.cmd test
```

Do not commit passwords or API keys to the repository.
