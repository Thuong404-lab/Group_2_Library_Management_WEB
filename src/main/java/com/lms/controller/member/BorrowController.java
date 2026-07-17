package com.lms.controller.member;
import com.lms.exception.ApplicationException;
import com.lms.exception.ResourceNotFoundException;

import com.lms.dto.response.MemberBorrowDTO;
import com.lms.entity.Book;
import com.lms.entity.Member;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.WalletRepository;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/member/borrow")
public class BorrowController {

    private final BorrowService borrowService;
    private final BookService bookService;
    private final LoanService loanService;
    private final MemberFavoriteService memberFavoriteService;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final WalletRepository walletRepository;
    private final SystemSettingRepository systemSettingRepository;

    public BorrowController(BorrowService borrowService,
                            MemberFavoriteService memberFavoriteService,
                            BookService bookService,
                            LoanService loanService,
                            MemberRepository memberRepository,
                            ReservationRepository reservationRepository,
                            WalletRepository walletRepository,
                            SystemSettingRepository systemSettingRepository) {
        this.borrowService = borrowService;
        this.bookService = bookService;
        this.loanService = loanService;
        this.memberFavoriteService = memberFavoriteService;
        this.memberRepository = memberRepository;
        this.reservationRepository = reservationRepository;
        this.walletRepository = walletRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @GetMapping("/management")
    public String viewBorrowManagement(@RequestParam(value = "tab", defaultValue = "borrowing") String tab,
                                       Principal principal,
                                       Model model) {
        if (principal == null) return "redirect:/login";

        String username = principal.getName();
        model.addAttribute("activeTab", tab);

        List<MemberBorrowDTO> currentBorrows = borrowService.getMemberCurrentBorrows(username);
        List<MemberBorrowDTO> reservations = borrowService.getMemberReservations(username);

        model.addAttribute("borrowingCount", currentBorrows.size());
        model.addAttribute("reservationCount", reservations.size());

        if ("reserved".equalsIgnoreCase(tab)) {
            model.addAttribute("booksData", reservations);
        } else if ("history".equalsIgnoreCase(tab)) {
            model.addAttribute("booksData", borrowService.getMemberOneMonthHistory(username));
        } else {
            model.addAttribute("booksData", currentBorrows);
        }

        return "member/borrow";
    }

    @GetMapping("/create")
    public String showCreateRequestForm(@RequestParam(value = "bookId", required = false) Integer bookId,
                                        Model model,
                                        Principal principal,
                                        RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sách trước khi gửi yêu cầu mượn.");
            return "redirect:/";
        }

        model.addAttribute("currentMemberName", principal.getName());
        model.addAttribute("selectedBookId", bookId);

        // --- THÊM DÒNG NÀY ĐỂ TRUYỀN DỮ LIỆU SỐ NGÀY MAX XUỐNG GIAO DIỆN ---
        model.addAttribute("maxBorrowDays", getMaxBorrowDays());

        try {
            Book book = bookService.findBookById(bookId);
            if ("Inactive".equalsIgnoreCase(book.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Sách này hiện không có sẵn để mượn!");
                return "redirect:/";
            }
            model.addAttribute("selectedBook", book);
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sách không hợp lệ. Vui lòng thử lại.");
            return "redirect:/";
        }
        return "member/borrow-create";
    }

    @PostMapping("/request/submit")
    public String submitBorrowRequest(@RequestParam(value = "bookId", required = false) Integer bookId,
                                      @RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa chọn sách để gửi yêu cầu mượn.");
            return "redirect:/";
        }

        // --- THÊM ĐOẠN VALIDATION CHẶN LỖI NHẬP QUÁ NGÀY Ở ĐÂY ---
        Integer maxDaysAllowed = getMaxBorrowDays();
        if (numberOfDays < 1 || numberOfDays > maxDaysAllowed) {
            redirectAttributes.addFlashAttribute("errorMessage", "Số ngày mượn không hợp lệ! Bạn chỉ được phép nhập từ 1 đến " + maxDaysAllowed + " ngày.");
            return "redirect:/member/borrow/create?bookId=" + bookId;
        }

        try {
            borrowService.memberSubmitBorrowRequest(principal.getName(), bookId, numberOfDays);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Yêu cầu của bạn đang chờ phê duyệt.");
            return "redirect:/member/borrow/management?tab=borrowing";
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo yêu cầu mượn: " + e.getMessage());
            return "redirect:/member/borrow/create?bookId=" + bookId + "&error=borrow";
        }
    }

    @GetMapping("/reserve/form/{bookId}")
    public String showReserveForm(@PathVariable Integer bookId,
                                  Model model,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
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
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hiển thị phiếu cọc: " + e.getMessage());
            return "redirect:/member/borrow/management?tab=reserved";
        }
    }

    // FIX CHÍNH TẠI ĐÂY: Đồng bộ gọi chính xác qua borrowService để tạo bản ghi đặt trước và lưu vết hệ thống
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Integer bookId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.memberSubmitReservationRequest(principal.getName(), bookId);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đặt trước thành công. Tiền cọc đã được trừ từ ví và ghi nhận trong phiếu đặt.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đặt trước sách: " + e.getMessage());
        }
        return "redirect:/member/borrow/management?tab=reserved";
    }

    @PostMapping("/cancel-reservation/{reservationId}")
    public String cancelReservation(@PathVariable Integer reservationId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.memberCancelReservation(principal.getName(), reservationId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy yêu cầu đặt giữ chỗ thành công.");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hủy đơn đặt chỗ: " + e.getMessage());
        }
        return "redirect:/member/borrow/management?tab=reserved";
    }

    @PostMapping("/renew/{borrowDetailId}")
    public String renewBook(@PathVariable("borrowDetailId") Integer borrowDetailId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.memberSubmitRenewRequest(borrowDetailId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu gia hạn tới thủ thư. Vui lòng chờ phê duyệt!");
        } catch (ApplicationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi yêu cầu gia hạn: " + e.getMessage());
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên hiện tại."));
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


}
