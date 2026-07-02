# TODO - UC-14.3 View Transaction History (Librarian)

## Plan implementation
- [ ] Implement endpoint `GET /librarian/members/transactions` to return real transaction history for the whole system
- [ ] Add repository queries in `TransactionRepository` to fetch all transactions with pagination and optional filtering by `type`
- [ ] Fix controller to pass model attributes required by the template (transactions, transactionPage, currentPage, selectedType, etc.)
- [ ] Fix Thymeleaf template `librarian/transaction_history.html` to use correct action URL and correct layout
- [ ] Ensure controller returns view name `librarian/transaction_history` (to match existing file)
- [x] Implement endpoint pagination + type filter (whole system)
- [x] Add repository queries for all transactions with pagination + type filter
- [x] Fix librarian template action URL to `/librarian/members/transactions`
- [x] Ensure controller returns `librarian/transaction_history`
- [x] Run `mvn test` (or `mvn spring-boot:run`) to confirm compile



