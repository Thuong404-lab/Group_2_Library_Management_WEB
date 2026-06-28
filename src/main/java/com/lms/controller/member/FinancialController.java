package com.lms.controller.member;

import com.lms.entity.Member;
import com.lms.entity.MemberNotification;
import com.lms.entity.Transaction;
import com.lms.repository.MemberNotificationRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    public FinancialController(TransactionRepository transactionRepository,
                               MemberNotificationRepository memberNotificationRepository,
                               MemberRepository memberRepository) {
        this.transactionRepository = transactionRepository;
        this.memberNotificationRepository = memberNotificationRepository;
        this.memberRepository = memberRepository;
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
        // TODO: Implement - Hiển thị phí mượn sách chưa thanh toán
        return "member/fees";
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
