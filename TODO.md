# TODO

## UC-8.2 Pay Borrowing Fees — Hoàn thiện nghiệp vụ “Thủ thư duyệt mới trừ tiền”

### Step 1: Update Borrow approve flow
- [ ] Edit `src/main/java/com/lms/service/impl/BorrowServiceImpl.java`
- Inject `FinancialService` vào `BorrowServiceImpl`
- Trong `approvePendingRequest(Integer borrowId, String staffUsername)`:
  - sau khi set `Borrow.status = "Active"` và `BorrowDetail.status = "Borrowed"`
  - gọi `financialService.payBorrowingFee(memberId, borrowId)`

### Step 2: Ensure correctness
- [ ] Verify `memberId` lấy từ `borrow.getMember().getMemberId()` là đúng
- [ ] Run `mvn test`

### Step 3: Validate UI behavior (manual)
- [ ] Member tạo phiếu mượn → không trừ ví
- [ ] Thủ thư duyệt → ví bị trừ và có `Transaction` type `BORROW_FEE`
- [ ] Member xem `fees.html`/`wallet` để thấy trạng thái phù hợp

