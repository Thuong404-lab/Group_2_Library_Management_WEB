# TODO

## UC-8.2 Pay Borrowing Fees - Done

### Step 1: Update borrow approve flow
- [x] `BorrowServiceImpl.approvePendingRequest(...)` deducts the borrowing fee after the request is approved.
- [x] `BorrowServiceImpl.processBorrowing(...)` also deducts the borrowing fee for direct librarian borrow flows.
- [x] `FinancialService.payBorrowingFee(memberId, borrowId)` creates a `BORROW_FEE` transaction.

### Step 2: Ensure correctness
- [x] `memberId` is validated against `borrow.getMember().getMemberId()`.
- [x] Duplicate payment is blocked by checking existing completed `BORROW_FEE` transactions for the same borrow.
- [x] Borrowing fee amount is calculated in one service method.
- [x] `mvnw test` passes.

### Step 3: UI behavior
- [x] Pending borrow requests are shown as waiting for librarian approval.
- [x] Approved but unpaid borrow fees are shown separately.
- [x] Paid borrow fees show transaction id and paid date.
