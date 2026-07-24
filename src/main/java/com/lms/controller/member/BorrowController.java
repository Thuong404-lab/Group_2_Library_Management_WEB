package com.lms.controller.member;

import com.lms.exception.ApplicationException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.controller.LocalizedControllerSupport;

import com.lms.dto.response.MemberBorrowDTO;
import com.lms.entity.Book;
import com.lms.entity.Borrow;
import com.lms.entity.Member;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.WalletRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BookItemRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.lms.service.BookService;
import com.lms.service.BorrowService;
import com.lms.service.LoanService;
import com.lms.service.MemberFavoriteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.lms.service.PayOsPaymentService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/member/borrow")
public class BorrowController extends LocalizedControllerSupport {

    private final BorrowService borrowService;
    private final BookService bookService;
    private final LoanService loanService;
    private final MemberFavoriteService memberFavoriteService;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final WalletRepository walletRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final PayOsPaymentService payOsPaymentService;
    private final BookItemRepository bookItemRepository;

    public BorrowController(BorrowService borrowService,
            MemberFavoriteService memberFavoriteService,
            BookService bookService,
            LoanService loanService,
            MemberRepository memberRepository,
            ReservationRepository reservationRepository,
            WalletRepository walletRepository,
            SystemSettingRepository systemSettingRepository,
            BorrowDetailRepository borrowDetailRepository,
            PayOsPaymentService payOsPaymentService,
            BookItemRepository bookItemRepository) {
        this.borrowService = borrowService;
        this.bookService = bookService;
        this.loanService = loanService;
        this.memberFavoriteService = memberFavoriteService;
        this.memberRepository = memberRepository;
        this.reservationRepository = reservationRepository;
        this.walletRepository = walletRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.payOsPaymentService = payOsPaymentService;
        this.bookItemRepository = bookItemRepository;
    }

    @GetMapping("/management")
    public String viewBorrowManagement(@RequestParam(value = "tab", defaultValue = "borrowing") String tab,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Principal principal,
            Model model) {
        if (principal == null)
            return "redirect:/login";

        String username = principal.getName();
        model.addAttribute("activeTab", tab);

        List<MemberBorrowDTO> currentBorrows = borrowService.getMemberCurrentBorrows(username);
        List<MemberBorrowDTO> reservations = borrowService.getMemberReservations(username);
        List<MemberBorrowDTO> history = borrowService.getMemberOneMonthHistory(username);

        model.addAttribute("borrowingCount", currentBorrows.size());
        model.addAttribute("reservationCount", reservations.size());
        Member member = getCurrentMember(principal);
        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance()).orElse(BigDecimal.ZERO);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("maxRenewalDays", getPositiveIntSetting("Max_Renewal_Days", 7));
        model.addAttribute("maxRenewals", getPositiveIntSetting("MAX_RENEWALS", 2));
        model.addAttribute("maxRenewalRequests", getPositiveIntSetting("MAX_RENEWAL_REQUESTS_PER_LOAN", 3));
        model.addAttribute("renewalApprovalTimeoutHours", getPositiveIntSetting("RENEWAL_APPROVAL_TIMEOUT_HOURS", 12));
        model.addAttribute("renewalFeePerDay", BigDecimal.valueOf(getPositiveIntSetting("FEE_PER_BOOK_PER_DAY", 5000)));

        java.time.LocalDate minDate = java.time.LocalDate.now().minusMonths(6);
        java.time.LocalDate maxDate = java.time.LocalDate.now();

        for (MemberBorrowDTO b : currentBorrows) {
            if (b.getActionDate() != null) {
                java.time.LocalDate d = b.getActionDate().toLocalDate();
                if (d.isBefore(minDate))
                    minDate = d;
            }
            if (b.getDueDate() != null) {
                java.time.LocalDate d = b.getDueDate().toLocalDate();
                if (d.isAfter(maxDate))
                    maxDate = d;
            }
        }
        for (MemberBorrowDTO r : reservations) {
            if (r.getActionDate() != null) {
                java.time.LocalDate d = r.getActionDate().toLocalDate();
                if (d.isBefore(minDate))
                    minDate = d;
            }
            if (r.getDueDate() != null) {
                java.time.LocalDate d = r.getDueDate().toLocalDate();
                if (d.isAfter(maxDate))
                    maxDate = d;
            }
        }
        for (MemberBorrowDTO h : history) {
            if (h.getActionDate() != null) {
                java.time.LocalDate d = h.getActionDate().toLocalDate();
                if (d.isBefore(minDate))
                    minDate = d;
            }
            if (h.getReturnDate() != null) {
                java.time.LocalDate d = h.getReturnDate().toLocalDate();
                if (d.isAfter(maxDate))
                    maxDate = d;
            }
        }

        model.addAttribute("minDate", minDate.toString());
        model.addAttribute("maxDate", maxDate.toString());

        List<StatusOption> statusOptions = new java.util.ArrayList<>();
        if ("reserved".equalsIgnoreCase(tab)) {
            statusOptions.add(new StatusOption("", "common.allStatuses"));
            statusOptions.add(new StatusOption("Pending", "reservation.status.preparing"));
            statusOptions.add(new StatusOption("Deposit_Paid", "reservation.status.depositPaid"));
            statusOptions.add(new StatusOption("Refund_Pending", "reservation.status.refundPending"));
            statusOptions.add(new StatusOption("Ready", "reservation.status.ready"));
            statusOptions.add(new StatusOption("Canceled", "reservation.status.canceled"));
            statusOptions.add(new StatusOption("Completed", "reservation.status.completed"));
        } else if ("history".equalsIgnoreCase(tab)) {
            statusOptions.add(new StatusOption("", "common.allStatuses"));
            statusOptions.add(new StatusOption("Returned", "loan.status.returned"));
            statusOptions.add(new StatusOption("Lost", "loan.status.lost"));
            statusOptions.add(new StatusOption("Canceled", "reservation.status.canceled"));
        } else { // borrowing
            statusOptions.add(new StatusOption("", "common.allStatuses"));
            statusOptions.add(new StatusOption("Borrowed", "loan.status.borrowed"));
            statusOptions.add(new StatusOption("Due_Soon", "loan.status.dueSoon"));
            statusOptions.add(new StatusOption("Overdue", "loan.status.overdue"));
            statusOptions.add(new StatusOption("Pending", "loan.status.pendingBorrow"));
            statusOptions.add(new StatusOption("Waiting_Pickup", "loan.status.waitingPickup"));
            statusOptions.add(new StatusOption("Return_Pending", "loan.status.pendingReturn"));
            statusOptions.add(new StatusOption("Renew_Pending", "loan.status.pendingRenewal"));
        }
        model.addAttribute("statusOptions", statusOptions);

        List<MemberBorrowDTO> rawList;
        if ("reserved".equalsIgnoreCase(tab)) {
            rawList = reservations;
        } else if ("history".equalsIgnoreCase(tab)) {
            rawList = history;
        } else {
            rawList = currentBorrows;
        }

        // Parse dates
        java.time.LocalDate from = null;
        java.time.LocalDate to = null;
        try {
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                from = java.time.LocalDate.parse(fromDate.trim());
                if (from.isBefore(minDate)) {
                    from = minDate;
                    fromDate = minDate.toString();
                }
                if (from.isAfter(maxDate)) {
                    from = maxDate;
                    fromDate = maxDate.toString();
                }
            }
            if (toDate != null && !toDate.trim().isEmpty()) {
                to = java.time.LocalDate.parse(toDate.trim());
                if (to.isBefore(minDate)) {
                    to = minDate;
                    toDate = minDate.toString();
                }
                if (to.isAfter(maxDate)) {
                    to = maxDate;
                    toDate = maxDate.toString();
                }
            }
        } catch (Exception ignored) {
        }

        List<MemberBorrowDTO> filteredList = new java.util.ArrayList<>();
        for (MemberBorrowDTO item : rawList) {
            boolean matchesSearch = true;
            if (search != null && !search.trim().isEmpty()) {
                String title = item.getBookTitle();
                if (title == null || !title.toLowerCase(java.util.Locale.ROOT)
                        .contains(search.trim().toLowerCase(java.util.Locale.ROOT))) {
                    matchesSearch = false;
                }
            }

            boolean matchesStatus = true;
            if (status != null && !status.trim().isEmpty()) {
                if ("borrowing".equalsIgnoreCase(tab) || tab == null || tab.isEmpty()) {
                    if ("Due_Soon".equalsIgnoreCase(status)) {
                        matchesStatus = "Borrowed".equalsIgnoreCase(item.getStatus()) && item.getDaysLeft() <= 2;
                    } else if ("Borrowed".equalsIgnoreCase(status)) {
                        matchesStatus = "Borrowed".equalsIgnoreCase(item.getStatus()) && item.getDaysLeft() > 2;
                    } else {
                        matchesStatus = status.equalsIgnoreCase(item.getStatus());
                    }
                } else if ("reserved".equalsIgnoreCase(tab)) {
                    if ("Ready".equalsIgnoreCase(status)) {
                        matchesStatus = "Ready".equalsIgnoreCase(item.getStatus())
                                || "Active".equalsIgnoreCase(item.getStatus());
                    } else {
                        matchesStatus = status.equalsIgnoreCase(item.getStatus());
                    }
                } else {
                    matchesStatus = status.equalsIgnoreCase(item.getStatus());
                }
            }

            boolean matchesDate = true;
            if (from != null || to != null) {
                boolean actionMatch = true;
                boolean returnMatch = true;

                if (item.getActionDate() != null) {
                    java.time.LocalDate actionDate = item.getActionDate().toLocalDate();
                    if (from != null && actionDate.isBefore(from)) {
                        actionMatch = false;
                    }
                    if (to != null && actionDate.isAfter(to)) {
                        actionMatch = false;
                    }
                } else {
                    actionMatch = false;
                }

                if (item.getReturnDate() != null) {
                    java.time.LocalDate returnDate = item.getReturnDate().toLocalDate();
                    if (from != null && returnDate.isBefore(from)) {
                        returnMatch = false;
                    }
                    if (to != null && returnDate.isAfter(to)) {
                        returnMatch = false;
                    }
                } else {
                    returnMatch = false;
                }

                if ("history".equalsIgnoreCase(tab)) {
                    matchesDate = actionMatch || returnMatch;
                } else {
                    matchesDate = actionMatch;
                }
            }

            if (matchesSearch && matchesStatus && matchesDate) {
                filteredList.add(item);
            }
        }

        int pageSize = 5;
        int totalItems = filteredList.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        if (page < 0)
            page = 0;
        if (totalPages > 0 && page >= totalPages)
            page = totalPages - 1;

        List<MemberBorrowDTO> pagedList = new java.util.ArrayList<>();
        if (totalItems > 0) {
            int start = page * pageSize;
            int end = Math.min(start + pageSize, totalItems);
            pagedList = filteredList.subList(start, end);
        }

        model.addAttribute("booksData", pagedList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "member/borrow";
    }

    @GetMapping("/create")
    public String showCreateRequestForm(@RequestParam(value = "bookId", required = false) Integer bookId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes,
            HttpServletResponse response,
            HttpSession session) {
        if (principal == null)
            return "redirect:/login";
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.selectBookFirst"));
            return "redirect:/";
        }

        Member member = null;
        try {
            member = memberRepository.findByAccountUsername(principal.getName())
                    .orElseThrow(() -> new ResourceNotFoundException(message("backend.member.currentNotFound")));
            // Kiểm tra số lượng bản vật lý khả dụng trong kho
            long availableCount = getAvailableCopyCount(bookId);
            if (availableCount == 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        message("backend.borrow.stockUnavailable"));
                return "redirect:/books/" + bookId;
            }
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        } catch (Exception e) {
            // ignore
        }

        if (member == null) {
            return "redirect:/login";
        }

        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                .orElse(BigDecimal.ZERO);
        double discountPercent = (member.getTier() != null && member.getTier().getDiscountPercent() != null)
                ? member.getTier().getDiscountPercent().doubleValue()
                : 0.0;
        BigDecimal feePerBookPerDay = BigDecimal
                .valueOf(systemSettingRepository.findBySettingKey("BORROW_FEE_PER_BOOK")
                        .map(s -> {
                            try {
                                return Integer.parseInt(s.getSettingValue());
                            } catch (Exception e) {
                                return 5000;
                            }
                        }).orElse(5000));

        model.addAttribute("member", member);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("feePerBookPerDay", feePerBookPerDay);

        model.addAttribute("currentMemberName", principal.getName());
        model.addAttribute("selectedBookId", bookId);

        // Truyền dữ liệu số ngày tối đa cho phép xuống giao diện
        model.addAttribute("maxBorrowDays", getMaxBorrowDays());
        long currentBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        int remainingBorrowLimit = Math.max(0, getEffectiveBorrowLimit(member) - Math.toIntExact(currentBorrowed));
        if (remainingBorrowLimit == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.tierLimitExceeded"));
            return "redirect:/member/borrow/management?tab=borrowing";
        }
        model.addAttribute("remainingBorrowLimit", remainingBorrowLimit);
        String submissionToken = UUID.randomUUID().toString();
        session.setAttribute("memberBorrowSubmissionToken", submissionToken);
        model.addAttribute("borrowSubmissionToken", submissionToken);

        try {
            Book book = bookService.findBookById(bookId);
            if ("Inactive".equalsIgnoreCase(book.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.bookUnavailable"));
                return "redirect:/";
            }
            // Keep physical inventory and the member's selectable quantity separate.
            // `availableCount` must mean the same thing as it does on book details
            // and librarian inventory: physical BookItems whose status is Available.
            long availableCount = getAvailableCopyCount(bookId);
            long maxRequestQuantity = Math.min(availableCount, remainingBorrowLimit);
            model.addAttribute("selectedBook", book);
            model.addAttribute("availableCount", availableCount);
            model.addAttribute("maxRequestQuantity", maxRequestQuantity);
            List<BigDecimal> cumulativeDailyFees = new java.util.ArrayList<>();
            cumulativeDailyFees.add(BigDecimal.ZERO);
            BigDecimal runningDailyFee = BigDecimal.ZERO;
            List<com.lms.entity.BookItem> availableItems = bookItemRepository
                    .findByBook_BookIdOrderByBookItemIdAsc(bookId).stream()
                    .filter(item -> "Available".equalsIgnoreCase(item.getStatus()))
                    .limit(maxRequestQuantity)
                    .toList();
            for (com.lms.entity.BookItem item : availableItems) {
                runningDailyFee = runningDailyFee.add(getBorrowFeeForCondition(item.getBookCondition()));
                cumulativeDailyFees.add(runningDailyFee);
            }
            model.addAttribute("cumulativeDailyFees", cumulativeDailyFees);
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.invalidBook"));
            return "redirect:/";
        }
        return "member/borrow-create";
    }

    @PostMapping("/request/submit")
    public String submitBorrowRequest(@RequestParam(value = "bookId", required = false) Integer bookId,
            @RequestParam(value = "submissionToken", required = false) String submissionToken,
            @RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
            @RequestParam(value = "paymentMethod", defaultValue = "WALLET") String paymentMethod,
            Principal principal,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (principal == null)
            return "redirect:/login";
        synchronized (session) {
            Object expectedToken = session.getAttribute("memberBorrowSubmissionToken");
            if (submissionToken == null || !submissionToken.equals(expectedToken)) {
                return "redirect:/member/borrow/management?tab=borrowing";
            }
            session.removeAttribute("memberBorrowSubmissionToken");
        }
        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.selectBookFirst"));
            return "redirect:/";
        }

        // 1. Validate số ngày mượn hợp lệ
        Integer maxDaysAllowed = getMaxBorrowDays();
        if (numberOfDays < 1 || numberOfDays > maxDaysAllowed) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    message("backend.borrow.invalidBorrowDays", maxDaysAllowed));
            return "redirect:/member/borrow/create?bookId=" + bookId;
        }

        // 2. Chuẩn hóa số lượng
        if (quantity == null || quantity < 1)
            quantity = 1;

        try {
            Member member = memberRepository.findByAccountUsername(principal.getName())
                    .orElseThrow(() -> new ResourceNotFoundException(message("backend.member.currentNotFound")));
                    
            // 3. Kiểm tra số lượng bản vật lý thực tế trong kho trước khi thực hiện
            long availableStock = getAvailableCopyCount(bookId);
            if (availableStock == 0) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.stockUnavailable"));
                return "redirect:/books/" + bookId;
            }
            if (quantity > availableStock) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        message("backend.borrow.stockExceeded", availableStock));
                return "redirect:/member/borrow/create?bookId=" + bookId;
            }

            // 4. Tính toán chi phí mượn sách có áp dụng giảm giá thành viên
            BigDecimal finalFee = borrowService.calculateBorrowFeePreview(
                    principal.getName(), java.util.Collections.nCopies(quantity, bookId), numberOfDays);

            BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                    .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                    .orElse(BigDecimal.ZERO);

            // 5. Xử lý luồng thanh toán qua Ví thành viên (WALLET)
            if ("WALLET".equalsIgnoreCase(paymentMethod)) {
                if (walletBalance.compareTo(finalFee) < 0) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            message("backend.borrow.insufficientWalletBalance"));
                    return "redirect:/member/borrow/create?bookId=" + bookId;
                }

                // Create one pending request; the fee is charged atomically when the librarian approves it.
                borrowService.memberSubmitMultiBookBorrowRequest(principal.getName(),
                        java.util.Collections.nCopies(quantity, bookId), numberOfDays);

                redirectAttributes.addFlashAttribute("successMessage",
                        message("backend.borrow.requestSubmittedQuantity", quantity));
                return "redirect:/member/borrow/management?tab=borrowing";
            }

            // 6. Xử lý luồng thanh toán qua Chuyển khoản (BANK - PayOS)
            if ("BANK".equalsIgnoreCase(paymentMethod) && finalFee.compareTo(BigDecimal.ZERO) > 0) {
                java.util.List<Integer> requestedBookIds = java.util.Collections.nCopies(quantity, bookId);
                com.lms.entity.Borrow pendingBorrow = null;
                try {
                    pendingBorrow = borrowService.memberSubmitBankMultiBookBorrowRequest(principal.getName(),
                            requestedBookIds, numberOfDays);
                    com.lms.entity.PayOsPayment payment = payOsPaymentService.createBorrowFeePayment(member,
                            pendingBorrow.getBorrowId());
                    return "redirect:/member/payments/payos/" + payment.getOrderCode();
                } catch (Exception paymentError) {
                    if (pendingBorrow != null && pendingBorrow.getBorrowId() != null) {
                        borrowService.cancelPendingBankBorrow(pendingBorrow.getBorrowId(), "CREATE_FAILED");
                    }
                    throw paymentError;
                }
            }

            // Zero-fee loans can be submitted immediately without opening a payment link.
            borrowService.memberSubmitMultiBookBorrowRequest(principal.getName(),
                    java.util.Collections.nCopies(quantity, bookId), numberOfDays);
            redirectAttributes.addFlashAttribute("successMessage",
                    message("backend.borrow.requestSubmittedQuantity", quantity));
            return "redirect:/member/borrow/management?tab=borrowing";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.borrow.requestFailed", e));
            return "redirect:/member/borrow/create?bookId=" + bookId + "&error=borrow";
        }
    }

    @GetMapping("/reserve/form/{bookId}")
    public String showReserveForm(@PathVariable Integer bookId,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/login";
        try {
            Book book = bookService.findBookById(bookId);
            Member member = getCurrentMember(principal);
            BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                    .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                    .orElse(BigDecimal.ZERO);
            BigDecimal depositAmount = getDepositAmount();

            model.addAttribute("book", book);
            model.addAttribute("username", principal.getName());
            model.addAttribute("walletBalance", walletBalance);
            model.addAttribute("depositAmount", depositAmount);
            model.addAttribute("remainingBalance", walletBalance.subtract(depositAmount));
            model.addAttribute("canPayDeposit", walletBalance.compareTo(depositAmount) >= 0);
            return "member/reserve-confirm";
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageWithDetail("backend.borrow.depositViewFailed", e));
            return "redirect:/member/borrow/management?tab=reserved";
        }
    }

    // FIX CHÍNH TẠI ĐÂY: Đồng bộ gọi chính xác qua borrowService để tạo bản ghi đặt
    // trước và lưu vết hệ thống
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Integer bookId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/login";
        try {
            borrowService.memberSubmitReservationRequest(principal.getName(), bookId);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.borrow.reservationSubmitted"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageWithDetail("backend.borrow.reservationFailed", e));
        }
        return "redirect:/member/borrow/management?tab=reserved";
    }

    @PostMapping("/cancel-reservation/{reservationId}")
    public String cancelReservation(@PathVariable Integer reservationId, Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/login";
        try {
            borrowService.memberCancelReservation(principal.getName(), reservationId);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.borrow.reservationCancelled"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageWithDetail("backend.borrow.reservationCancelFailed", e));
        }
        return "redirect:/member/borrow/management?tab=reserved";
    }

    @PostMapping("/renew/{borrowDetailId}")
    public String renewBook(@PathVariable("borrowDetailId") Integer borrowDetailId,
            @RequestParam("renewalDays") Integer renewalDays,
            Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/login";
        try {
            borrowService.memberSubmitRenewRequest(principal.getName(), borrowDetailId, renewalDays);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.borrow.renewalSubmitted"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageWithDetail("backend.borrow.renewalSubmitFailed", e));
        }
        return "redirect:/member/borrow/management?tab=borrowing";
    }

    @PostMapping("/renew/{borrowDetailId}/cancel")
    public String cancelRenewRequest(@PathVariable("borrowDetailId") Integer borrowDetailId,
                                     Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.memberCancelRenewRequest(principal.getName(), borrowDetailId);
            redirectAttributes.addFlashAttribute("successMessage", message("backend.renewal.cancelled"));
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.renewal.cancelFailed", e));
        }
        return "redirect:/member/borrow/management?tab=borrowing";
    }

    @GetMapping("/history")
    public String viewBorrowingHistory() {
        return "redirect:/member/borrow/management?tab=history";
    }

    @GetMapping("/reservations")
    public String viewReservations(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Member member = getCurrentMember(principal);
        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);

        model.addAttribute("reservations",
                reservationRepository.findByMemberMemberIdOrderByReservationDateDesc(member.getMemberId()));
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("depositAmount", getDepositAmount());

        return "member/reservations";
    }

    @GetMapping("/current")
    public String viewCurrentBorrows() {
        return "redirect:/member/borrow/management?tab=borrowing";
    }

    @GetMapping("/returns")
    public String viewPendingReturns() {
        return "redirect:/member/borrow/management?tab=borrowing";
    }

    private Member getCurrentMember(Principal principal) {
        String usernameOrEmail = principal.getName();
        return memberRepository.findByUserEmail(usernameOrEmail)
                .or(() -> memberRepository.findByUserPhone(usernameOrEmail))
                .or(() -> memberRepository.findByAccountUsername(usernameOrEmail))
                .orElseThrow(() -> new ResourceNotFoundException(message("backend.member.currentNotFound")));
    }

    private BigDecimal getDepositAmount() {
        return systemSettingRepository.findAll().stream()
                .filter(setting -> setting.getSettingKey() != null)
                .filter(setting -> "Deposit_Amount".equalsIgnoreCase(setting.getSettingKey()))
                .map(setting -> setting.getSettingValue())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(BigDecimal::new)
                .filter(value -> value.signum() > 0)
                .findFirst()
                .orElse(BigDecimal.valueOf(50000));
    }

    private int getPositiveIntSetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key).map(setting -> setting.getSettingValue())
                    .filter(v -> v != null && !v.isBlank()).map(String::trim).map(Integer::parseInt)
                    .filter(v -> v > 0).orElse(defaultValue);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private int getEffectiveBorrowLimit(Member member) {
        int configuredLimit = getPositiveIntSetting("Max_Books_Per_Member",
                getPositiveIntSetting("MAX_BOOKS_PER_MEMBER", 10));
        Integer tierLimit = member != null && member.getMemberId() != null
                ? memberRepository.findCurrentBorrowLimitByMemberId(member.getMemberId())
                        .orElse(member.getTier() != null ? member.getTier().getBorrowLimit() : null)
                : null;
        return Math.max(1, tierLimit != null ? tierLimit : configuredLimit);
    }

    private long getAvailableCopyCount(Integer bookId) {
        return bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, "Available");
    }

    private BigDecimal getBorrowFeeForCondition(String bookCondition) {
        String condition = bookCondition == null ? "" : bookCondition.trim().toLowerCase(java.util.Locale.ROOT);
        if (condition.contains("severely")) {
            return getMoneySetting("SEVERE_DAMAGE_BORROW_FEE", 3000);
        }
        if (condition.contains("minor")) {
            return getMoneySetting("MINOR_DAMAGE_BORROW_FEE", 4000);
        }
        return getMoneySetting("BORROW_FEE_PER_BOOK", 5000);
    }

    private BigDecimal getMoneySetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                    .map(setting -> setting.getSettingValue())
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(BigDecimal::new)
                    .filter(value -> value.signum() >= 0)
                    .orElse(BigDecimal.valueOf(defaultValue));
        } catch (NumberFormatException ignored) {
            return BigDecimal.valueOf(defaultValue);
        }
    }

    private Integer getMaxBorrowDays() {
        return systemSettingRepository.findAll().stream()
                .filter(setting -> setting.getSettingKey() != null)
                .filter(setting -> "Max_Borrow_Days".equalsIgnoreCase(setting.getSettingKey()))
                .map(setting -> setting.getSettingValue())
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(Integer::parseInt)
                .filter(value -> value > 0)
                .findFirst()
                .orElse(14); // Giá trị mặc định nếu database chưa có key này
    }

    public static class StatusOption {
        private final String value;
        private final String labelKey;

        public StatusOption(String value, String labelKey) {
            this.value = value;
            this.labelKey = labelKey;
        }

        public String getValue() {
            return value;
        }

        public String getLabelKey() {
            return labelKey;
        }
    }
}
