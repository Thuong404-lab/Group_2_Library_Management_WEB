package com.lms.controller.member;

import com.lms.entity.Book;
import com.lms.entity.Member;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.BorrowService;
import com.lms.service.MemberFavoriteService;
import com.lms.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
@RequestMapping("/member/borrow")
public class BorrowController {

    private final BorrowService borrowService;
    private final MemberFavoriteService memberFavoriteService;
    private final BookService bookService;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final WalletRepository walletRepository;
    private final SystemSettingRepository systemSettingRepository;

    public BorrowController(BorrowService borrowService,
                            MemberFavoriteService memberFavoriteService,
                            BookService bookService,
                            MemberRepository memberRepository,
                            ReservationRepository reservationRepository,
                            WalletRepository walletRepository,
                            SystemSettingRepository systemSettingRepository) {
        this.borrowService = borrowService;
        this.memberFavoriteService = memberFavoriteService;
        this.bookService = bookService;
        this.memberRepository = memberRepository;
        this.reservationRepository = reservationRepository;
        this.walletRepository = walletRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    // UC-6.0: Hiển thị form tạo yêu cầu mượn sách trực tuyến
    @GetMapping("/create")
    public String showCreateRequestForm(@RequestParam(value = "bookId", required = false) Integer bookId, Model model, Principal principal,
                                        RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn sách trước khi gửi yêu cầu mượn.");
            return "redirect:/member/dashboard";
        }

        model.addAttribute("currentMemberName", principal.getName());
        model.addAttribute("selectedBookId", bookId);

        try {
            Book book = bookService.findBookById(bookId);
            model.addAttribute("selectedBook", book);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sách không hợp lệ. Vui lòng thử lại.");
            return "redirect:/member/dashboard";
        }
        return "member/borrow-create";
    }

    // UC-6.0: Xử lý submit đơn đăng ký mượn trực tuyến (Lưu trạng thái Pending vào bảng Borrows)
    @PostMapping("/request/submit")
    public String submitBorrowRequest(@RequestParam(value = "bookId", required = false) Integer bookId,
                                      @RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        if (bookId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa chọn sách để gửi yêu cầu mượn.");
            return "redirect:/member/dashboard";
        }
        try {
            borrowService.memberSubmitBorrowRequest(principal.getName(), bookId, numberOfDays);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Yêu cầu của bạn đã được gửi tới Thủ thư và đang chờ phê duyệt.");
            return "redirect:/member/dashboard?success=borrow";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo yêu cầu mượn: " + e.getMessage());
            return "redirect:/member/borrow/create?bookId=" + (bookId != null ? bookId : "") + "&error=borrow";
        }
    }

    // UC-6.2: Gọi xử lý đặt giữ chỗ sách trực tuyến (Reserve) -> Form trung gian
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
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể hiển thị phiếu cọc: " + e.getMessage());
            return "redirect:/member/dashboard";
        }
    }

    // FIX LỖI 1: Thực hiện gửi yêu cầu đặt chỗ lưu trực tiếp vào bảng Reservations (Không lỗi 500)
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Integer bookId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            memberFavoriteService.reserveBook(principal.getName(), bookId);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt trước sách và thanh toán tiền cọc thành công.");
            return "redirect:/member/dashboard?success=reserve";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đặt trước sách: " + e.getMessage());
            return "redirect:/member/dashboard?error=reserve";
        }
    }

    // UC-7.0: Độc giả gửi yêu cầu trả sách trực tuyến ra quầy
    @PostMapping("/return/{loanId}")
    public String returnBook(@PathVariable("loanId") Integer loanId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            borrowService.updateStatus(loanId, "Return_Pending");
            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu trả sách đã được gửi đi! Vui lòng đợi thủ thư tiếp nhận vật lý tại quầy.");
            return "redirect:/member/dashboard?success=return";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xử lý trả sách: " + e.getMessage());
            return "redirect:/member/dashboard";
        }
    }

    @GetMapping("/history")
    public String viewBorrowingHistory(Principal principal, Model model) {
        if (principal != null) {
            model.addAttribute("historyBorrows", borrowService.getAllBorrowHistoryByMember(principal.getName()));
        }
        return "member/borrow-history";
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
    public String viewCurrentBorrows() { return "member/current-borrows"; }

    @GetMapping("/returns")
    public String viewPendingReturns() { return "member/pending-returns"; }

    private Member getCurrentMember(Principal principal) {
        String usernameOrEmail = principal.getName();
        return memberRepository.findByUserEmail(usernameOrEmail)
                .or(() -> memberRepository.findByUserPhone(usernameOrEmail))
                .or(() -> memberRepository.findByAccountUsername(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thành viên hiện tại."));
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
}
