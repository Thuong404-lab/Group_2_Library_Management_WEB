package com.lms.controller.member;

import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.Transaction;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import com.lms.dto.response.BorrowFeeViewData;
import com.lms.repository.BorrowRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * FinancialController - Giao dịch Tài chính (Phía Member)
 * Người phụ trách: Phạm Kiến Quốc (CE201286)
 */
@Controller
@RequestMapping("/member/financial")
public class FinancialController {

    private final TransactionRepository transactionRepository;
    private final MemberNotificationRepository memberNotificationRepository;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final com.lms.service.FinancialService financialService;

    public FinancialController(TransactionRepository transactionRepository,
                               MemberNotificationRepository memberNotificationRepository,
                               MemberRepository memberRepository,
                               WalletRepository walletRepository,
                               BorrowRepository borrowRepository,
                               BorrowDetailRepository borrowDetailRepository,
                               com.lms.service.FinancialService financialService) {
        this.transactionRepository = transactionRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.financialService = financialService;
    }

    // UC-8.1: Pay Overdue Fines
    @GetMapping("/fines")
    public String viewOverdueFines(Principal principal, Model model) {
        // TODO: Implement - Lấy danh sách khoản phạt chưa thanh toán
        // TODO: Tính tổng tiền phạt quá hạn
        return "member/fines";
    }

    @PostMapping("/fines/pay/{fineId}")
    public String payOverdueFine(@PathVariable Integer fineId,
                                 Principal principal,
                                 Model model) {
        // TODO: Implement - Trừ tiền phạt từ Wallet
        // TODO: Nếu Wallet không đủ → cho phép thẻ âm (theo nghiệp vụ)
        // TODO: Tạo Transaction (type = FINE_PAYMENT)
        return "redirect:/member/financial/fines?paid";
    }

    // UC-8.2: Pay Borrowing Fees
    @GetMapping("/fees")
    public String viewBorrowingFees(Principal principal, Model model) {
        Member member = getCurrentMember(principal);

        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);

        // Theo quy ước: phí mượn = số quyển * 10 ngày * (giá trị phí trên 1 ngày của “quyển”)
        // Vì repo hiện chưa có đơn giá, mình triển khai tối thiểu theo số ngày dự kiến và “1 ngày = 1 đ”.
        // Nếu bạn có đơn giá cụ thể, chỉ cần sửa chỗ computeBorrowFeeAmount().
        List<BorrowFeeViewData> fees = new java.util.ArrayList<>();

        // Lấy toàn bộ phiếu mượn của member và chỉ lấy các phiếu đang Pending/Borrowed/Overdue
        // (tránh tính lại các phiếu đã được trả/đã thanh toán).
        List<com.lms.entity.Borrow> borrows = borrowRepository.findByMember_MemberIdOrderByBorrowDateDesc(member.getMemberId());

        for (com.lms.entity.Borrow borrow : borrows) {
            if (borrow == null) continue;
            String status = borrow.getStatus();
            if (status == null) continue;
            if (!("Pending".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status) || "Borrowing".equalsIgnoreCase(status))) {
                continue;
            }

            List<com.lms.entity.BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrow.getBorrowId());
            if (details == null || details.isEmpty()) continue;

            int quantity = (int) details.size();
            int days = 10; // mặc định 1 quyển là 10 ngày theo yêu cầu

            BigDecimal amount = computeBorrowFeeAmount(quantity, days);

            BorrowFeeViewData feeViewData = new BorrowFeeViewData();
            feeViewData.setBorrowId(borrow.getBorrowId());
            feeViewData.setQuantity(quantity);
            feeViewData.setDays(days);
            feeViewData.setAmount(amount);
            fees.add(feeViewData);
        }

        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("fees", fees);
        return "member/fees";
    }

    private BigDecimal computeBorrowFeeAmount(int quantity, int days) {
        // Mặc định: 1 ngày * 1 đ / quyển
        // -> tổng = quantity * days.
        return BigDecimal.valueOf((long) quantity).multiply(BigDecimal.valueOf((long) days));
    }

    // UC-8.2: Pay Borrowing Fees
    @PostMapping("/fees/pay/{borrowId}")
    public String payBorrowingFee(@PathVariable Integer borrowId,
                                   Principal principal,
                                   Model model) {
        Member member = getCurrentMember(principal);

        try {
            // amount được tính trong service dựa trên BorrowDetail (quantity * 10 ngày * 1đ/ngày)
            // trừ ví + tạo Transaction BORROW_FEE
            financialService.payBorrowingFee(member.getMemberId(), borrowId);
            model.addAttribute("message", "Đã thanh toán phí mượn thành công!");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        // reload page
        return viewBorrowingFees(principal, model);
    }



    // UC-8.3: Pay Reservation Deposit
    @PostMapping("/deposit/{reservationId}")
    public String payReservationDeposit(@PathVariable Integer reservationId,
                                        Principal principal,
                                        Model model) {
        // TODO: Implement - Trừ tiền cọc đặt trước từ Wallet
        // TODO: Tạo Transaction (type = RESERVATION_DEPOSIT)
        return "redirect:/member/borrow/reservations?depositPaid";
    }

    // UC-8.4: View Transaction History
    @GetMapping("/transactions")
    public String viewTransactionHistory(Principal principal,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(required = false) String type,
                                         Model model) {
        Member member = getCurrentMember(principal);
        int currentPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(currentPage, 10);

        Page<Transaction> transactionPage;
        if (type == null || type.trim().isEmpty()) {
            transactionPage = transactionRepository
                    .findByWalletMemberMemberIdOrderByTransactionDateDesc(
                            member.getMemberId(),
                            pageable
                    );
        } else {
            transactionPage = transactionRepository
                    .findByWalletMemberMemberIdAndTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
                            member.getMemberId(),
                            type.trim(),
                            pageable
                    );
        }

        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(wallet -> wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance())
                .orElse(BigDecimal.ZERO);

        transactionRepository
                .findTopByWalletMemberMemberIdAndTransactionTypeContainingIgnoreCaseOrderByTransactionDateDesc(
                        member.getMemberId(),
                        "TOP_UP"
                )
                .ifPresent(transaction -> model.addAttribute(
                        "topupPopupMessage",
                        buildTopupPopupMessage(transaction)
                ));

        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("selectedType", type);

        return "member/wallet";
    }

    // UC-8.5: View Top-up Notifications
    @GetMapping("/topup-notifications")
    public String viewTopupNotifications(Principal principal, Model model) {
        Member member = getCurrentMember(principal);

        List<MemberNotification> notifications = memberNotificationRepository
                .findByMember_MemberIdOrderByNotification_CreatedDateDesc(member.getMemberId())
                .stream()
                .filter(this::isTopupNotification)
                .collect(Collectors.toList());

        model.addAttribute("notifications", notifications);
        model.addAttribute("showNotificationBell", false);

        return "member/topup-notifications";
    }

    @GetMapping("/top-up-notifications")
    public String redirectTopUpNotifications() {
        return "redirect:/member/financial/topup-notifications";
    }

    private Member getCurrentMember(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Bạn cần đăng nhập để xem thông tin tài chính");
        }

        String usernameOrEmail = principal.getName();

        return memberRepository.findByUserEmail(usernameOrEmail)
                .or(() -> memberRepository.findByUserPhone(usernameOrEmail))
                .or(() -> memberRepository.findByAccountUsername(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin member hiện tại"));
    }

    private boolean isTopupNotification(MemberNotification memberNotification) {
        if (memberNotification == null || memberNotification.getNotification() == null) {
            return false;
        }

        String title = Objects.toString(memberNotification.getNotification().getTitle(), "");
        String content = Objects.toString(memberNotification.getNotification().getContent(), "");
        String searchableText = normalizeText(title + " " + content);

        // Chỉ coi là "Thông báo nạp tiền" khi đúng title mà librarian tạo ra.
        // Điều này giúp tránh bị trùng với các thông báo khác.
        return "Nạp tiền vào ví thành công".equalsIgnoreCase(title);
    }

    private String buildTopupPopupMessage(Transaction transaction) {
        BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount().abs();
        NumberFormat currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        currencyFormatter.setMaximumFractionDigits(0);

        return "Bạn có giao dịch nạp tiền "
                + currencyFormatter.format(amount)
                + " đ trong lịch sử giao dịch.";
    }

    private String normalizeText(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                        .replace('ạ', 'a')
                        .replace('ả', 'a')
                        .replace('ã', 'a')
                        .replace('á', 'a')
                        .replace('à', 'a')
                        .replace('ậ', 'a')
                        .replace('ẩ', 'a')
                        .replace('ẫ', 'a')
                        .replace('ấ', 'a')
                        .replace('ầ', 'a')
                        .replace('ắ', 'a')
                        .replace('ằ', 'a')
                        .replace('ẳ', 'a')
                        .replace('ẵ', 'a')
                        .replace('ặ', 'a')
                        .replace('ă', 'a')
                        .replace('â', 'a')
                        .replace('ẹ', 'e')
                        .replace('ẻ', 'e')
                        .replace('ẽ', 'e')
                        .replace('é', 'e')
                        .replace('è', 'e')
                        .replace('ệ', 'e')
                        .replace('ể', 'e')
                        .replace('ễ', 'e')
                        .replace('ế', 'e')
                        .replace('ề', 'e')
                        .replace('ê', 'e')
                        .replace('ị', 'i')
                        .replace('ỉ', 'i')
                        .replace('ĩ', 'i')
                        .replace('í', 'i')
                        .replace('ì', 'i')
                        .replace('ọ', 'o')
                        .replace('ỏ', 'o')
                        .replace('õ', 'o')
                        .replace('ó', 'o')
                        .replace('ò', 'o')
                        .replace('ộ', 'o')
                        .replace('ổ', 'o')
                        .replace('ỗ', 'o')
                        .replace('ố', 'o')
                        .replace('ồ', 'o')
                        .replace('ơ', 'o')
                        .replace('ợ', 'o')
                        .replace('ở', 'o')
                        .replace('ỡ', 'o')
                        .replace('ớ', 'o')
                        .replace('ờ', 'o')
                        .replace('ô', 'o')
                        .replace('ụ', 'u')
                        .replace('ủ', 'u')
                        .replace('ũ', 'u')
                        .replace('ú', 'u')
                        .replace('ù', 'u')
                        .replace('ư', 'u')
                        .replace('ự', 'u')
                        .replace('ử', 'u')
                        .replace('ữ', 'u')
                        .replace('ứ', 'u')
                        .replace('ừ', 'u')
                        .replace('ỵ', 'y')
                        .replace('ỷ', 'y')
                        .replace('ỹ', 'y')
                        .replace('ý', 'y')
                        .replace('ỳ', 'y')
                        .replace('đ', 'd');
    }
}
