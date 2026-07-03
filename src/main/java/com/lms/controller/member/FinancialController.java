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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final com.lms.repository.SystemSettingRepository systemSettingRepository;

    public FinancialController(TransactionRepository transactionRepository,
                               MemberNotificationRepository memberNotificationRepository,
                               MemberRepository memberRepository,
                               WalletRepository walletRepository,
                               BorrowRepository borrowRepository,
                               BorrowDetailRepository borrowDetailRepository,
                               com.lms.service.FinancialService financialService,
                               com.lms.repository.SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;

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
        return viewTransactionHistory(principal, 0, "FINE", model);
    }

    @PostMapping("/fines/pay/{fineId}")
    public String payOverdueFine(@PathVariable Integer fineId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        Member member = getCurrentMember(principal);

        try {
            financialService.payOverdueFine(member.getMemberId(), fineId);
            redirectAttributes.addFlashAttribute("success", "Đã thanh toán phí phạt thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/member/financial/transactions";
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
            // Hệ thống đang dùng enum BorrowStatus: Active, Returned, Overdue
            // Nên coi Active/Overdue là đang cần thanh toán.
            if (!("Active".equalsIgnoreCase(status) || "Overdue".equalsIgnoreCase(status))) {
                continue;
            }


            List<com.lms.entity.BorrowDetail> details = borrowDetailRepository.findByBorrowId(borrow.getBorrowId());
            if (details == null || details.isEmpty()) continue;

            int quantity = (int) details.size();

            int days = 10; // số ngày dự kiến (theo logic hiện tại)

            // Lấy giá tiền mượn từ SystemSettings (ADMIN cấu hình). Nếu chưa có thì fallback 5000.
            // Quy ước: borrowFeePerBook = tiền 1 quyển / 1 ngày.
            BigDecimal amount = BigDecimal.valueOf(quantity)
                    .multiply(BigDecimal.valueOf(days))
                    .multiply(getBorrowFeePerBook());


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

    private BigDecimal getBorrowFeePerBook() {
        // borrowFeePerBook = tiền 1 quyển / 1 ngày
        try {
            java.util.Optional<com.lms.entity.SystemSetting> settingOpt = systemSettingRepository.findAll().stream()
                    .filter(s -> s.getSettingKey() != null && s.getSettingKey().equalsIgnoreCase("BORROW_FEE_PER_BOOK"))
                    .findFirst();

            if (settingOpt.isPresent() && settingOpt.get().getSettingValue() != null) {
                return BigDecimal.valueOf(Double.parseDouble(settingOpt.get().getSettingValue()));
            }
        } catch (Exception ignored) {
        }

        return BigDecimal.valueOf(5000d);
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
        Pageable pageable = PageRequest.of(page, 10);

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
        List<Transaction> unpaidFines = transactionRepository.findUnpaidFineTransactions(
                member.getMemberId(), List.of("FINE", "DAMAGE_FEE"));
        BigDecimal totalUnpaidFines = unpaidFines.stream()
                .map(transaction -> transaction.getAmount() == null
                        ? BigDecimal.ZERO
                        : transaction.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("unpaidFines", unpaidFines);
        model.addAttribute("totalUnpaidFines", totalUnpaidFines);
        model.addAttribute("transactionPage", transactionPage);
        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedType", type);

        return "member/wallet";
    }

    // UC-8.5: View Top-up Notifications
    @GetMapping("/topup-notifications")
    public String viewTopupNotifications(Principal principal, Model model) {
        Member member = getCurrentMember(principal);

        List<MemberNotification> titleMatches =
                memberNotificationRepository
                        .findByMemberMemberIdAndNotificationTitleContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
                                member.getMemberId(),
                                "nạp tiền"
                        );

        List<MemberNotification> contentMatches =
                memberNotificationRepository
                        .findByMemberMemberIdAndNotificationContentContainingIgnoreCaseOrderByNotificationCreatedDateDesc(
                                member.getMemberId(),
                                "nạp tiền"
                        );

        Map<Integer, MemberNotification> notificationMap = new LinkedHashMap<>();

        for (MemberNotification memberNotification : titleMatches) {
            notificationMap.put(
                    memberNotification.getNotification().getNotificationId(),
                    memberNotification
            );
        }

        for (MemberNotification memberNotification : contentMatches) {
            notificationMap.put(
                    memberNotification.getNotification().getNotificationId(),
                    memberNotification
            );
        }
        List<MemberNotification> notifications = new ArrayList<>(notificationMap.values());

        model.addAttribute("notifications", notifications);

        return "member/topup-notifications";
    }

    private Member getCurrentMember(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Bạn cần đăng nhập để xem thông tin tài chính");
        }

        String usernameOrEmail = principal.getName();

        return memberRepository.findByUserEmail(usernameOrEmail)
                .or(() -> memberRepository.findByUserPhone(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin member hiện tại"));
    }
}
