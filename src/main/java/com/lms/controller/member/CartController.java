package com.lms.controller.member;

import com.lms.exception.ResourceNotFoundException;
import com.lms.controller.LocalizedControllerSupport;
import com.lms.entity.Book;
import com.lms.entity.Member;
import com.lms.repository.MemberRepository;
import com.lms.repository.WalletRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.BookItemRepository;
import com.lms.service.BorrowService;
import com.lms.service.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/member/cart")
public class CartController extends LocalizedControllerSupport {

    private final CartService cartService;
    private final BorrowService borrowService;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final com.lms.service.PayOsPaymentService payOsPaymentService;
    private final SystemSettingRepository systemSettingRepository;
    private final BookItemRepository bookItemRepository;

    public CartController(CartService cartService, BorrowService borrowService,
                          MemberRepository memberRepository, WalletRepository walletRepository,
                          com.lms.service.PayOsPaymentService payOsPaymentService,
                          SystemSettingRepository systemSettingRepository,
                          BookItemRepository bookItemRepository) {
        this.cartService = cartService;
        this.borrowService = borrowService;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.payOsPaymentService = payOsPaymentService;
        this.systemSettingRepository = systemSettingRepository;
        this.bookItemRepository = bookItemRepository;
    }

    @PostMapping("/add")
    public Object addToCart(@RequestParam("bookId") Integer bookId, HttpSession session,
                            RedirectAttributes redirectAttributes,
                            @RequestHeader(value = "referer", required = false) String referer,
                            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        // 1. Kiểm tra số lượng bản sao khả dụng thực tế trong kho bằng hàm JpaRepository của bạn
        long availableStock = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, "Available");

        // 2. Lấy số lượng cuốn sách này hiện đã nằm trong giỏ sách Session
        int currentInCart = cartService.getQuantityInCart(session, bookId);

        // 3. Chặn nếu số lượng thêm vào vượt số lượng khả dụng trong kho
        if (currentInCart >= availableStock) {
            String errorMessage = "Không thể thêm sách! Số lượng trong giỏ hiện tại đã đạt giới hạn tối đa có sẵn trong kho (" + availableStock + " cuốn).";

            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body(java.util.Map.of("success", false, "message", errorMessage));
            }

            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.SEE_OTHER)
                    .location(java.net.URI.create(referer != null && !referer.isBlank() ? referer : "/books/" + bookId))
                    .build();
        }

        // 4. Đủ điều kiện, thêm vào giỏ hàng
        cartService.addToCart(session, bookId);
        String successMessage = message("backend.cart.added");

        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("success", true, "message", successMessage));
        }
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.SEE_OTHER)
                .location(java.net.URI.create(referer != null && !referer.isBlank() ? referer : "/books/" + bookId))
                .build();
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam("bookId") Integer bookId, HttpSession session) {
        cartService.removeFromCart(session, bookId);
        return "redirect:/member/cart";
    }

    @SuppressWarnings("unchecked")
    @GetMapping
    public String viewCart(HttpSession session, Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberRepository.findByAccountUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException(message("backend.cart.memberNotFound")));

        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                .orElse(BigDecimal.ZERO);

        List<Book> cartItems = cartService.getCartItems(session);
        List<Integer> cartIds = (List<Integer>) session.getAttribute("BOOK_CART");
        java.util.Map<Integer, Long> quantities = new java.util.HashMap<>();
        if (cartIds != null) {
            for (Integer id : cartIds) {
                quantities.put(id, quantities.getOrDefault(id, 0L) + 1L);
            }
        }

        double discountPercent = (member.getTier() != null && member.getTier().getDiscountPercent() != null)
                ? member.getTier().getDiscountPercent().doubleValue() : 0.0;

        java.util.Map<Integer, Long> availableStocks = new java.util.HashMap<>();
        for (Book book : cartItems) {
            long stock = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(book.getBookId(), "Available");
            availableStocks.put(book.getBookId(), stock);
        }

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("quantities", quantities);
        model.addAttribute("availableStocks", availableStocks);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("discountPercent", discountPercent);

        Integer maxBorrowDays = systemSettingRepository.findBySettingKey("MAX_BORROW_DAYS")
                .map(s -> { try { return Integer.parseInt(s.getSettingValue()); } catch (Exception e) { return 30; } })
                .orElse(30);
        model.addAttribute("maxBorrowDays", maxBorrowDays);
        return "member/cart";
    }

    @GetMapping("/checkout")
    public String checkoutCart(@RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
                               @RequestParam(value = "selectedBookIds", required = false) List<Integer> selectedBookIds,
                               HttpSession session, Principal principal, Model model, RedirectAttributes redirectAttributes,
                               HttpServletResponse response) {
        if (principal == null) return "redirect:/login";
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (selectedBookIds == null || selectedBookIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.selectionRequired"));
            return "redirect:/member/cart";
        }

        List<Book> allCartItems = cartService.getCartItems(session);
        List<Book> selectedCartItems = new java.util.ArrayList<>();
        for (Integer id : selectedBookIds) {
            allCartItems.stream()
                    .filter(item -> item.getBookId().equals(id))
                    .findFirst()
                    .ifPresent(selectedCartItems::add);
        }

        if (selectedCartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
            return "redirect:/member/cart";
        }

        BigDecimal previewFee = borrowService.calculateBorrowFeePreview(principal.getName(), selectedBookIds, numberOfDays);

        Member member = memberRepository.findByAccountUsername(principal.getName()).orElseThrow();
        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                .orElse(BigDecimal.ZERO);

        model.addAttribute("cartItems", selectedCartItems);
        model.addAttribute("selectedBookIds", selectedBookIds);
        model.addAttribute("numberOfDays", numberOfDays);
        model.addAttribute("totalFee", previewFee);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("afterBalance", walletBalance.subtract(previewFee));
        return "member/cart-checkout";
    }

    @PostMapping("/submit")
    public String submitCartRequest(@RequestParam("numberOfDays") Integer numberOfDays,
                                    @RequestParam(value = "selectedBookIds", required = false) List<Integer> selectedBookIds,
                                    @RequestParam(value = "paymentMethod", defaultValue = "WALLET") String paymentMethod,
                                    HttpSession session, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (selectedBookIds == null || selectedBookIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.missingSelection"));
            return "redirect:/member/cart";
        }

        List<Book> cartItems = cartService.getCartItems(session);
        List<Integer> cartBookIds = cartItems.stream().map(Book::getBookId).toList();
        boolean allInCart = selectedBookIds.stream().allMatch(cartBookIds::contains);

        if (!allInCart) {
            redirectAttributes.addFlashAttribute("errorMessage", "Quy trình lập phiếu thất bại: Sách đã được thanh toán hoặc không còn trong giỏ sách.");
            return "redirect:/member/cart";
        }

        try {
            Member member = memberRepository.findByAccountUsername(principal.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin độc giả!"));
            BigDecimal previewFee = borrowService.calculateBorrowFeePreview(principal.getName(), selectedBookIds, numberOfDays);
            BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                    .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                    .orElse(BigDecimal.ZERO);

            if ("WALLET".equalsIgnoreCase(paymentMethod)) {
                if (walletBalance.compareTo(previewFee) < 0) {
                    redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.insufficientWalletBalance"));
                    return "redirect:/member/cart";
                }

                borrowService.memberSubmitMultiBookBorrowRequest(principal.getName(), selectedBookIds, numberOfDays);
                selectedBookIds.forEach(bookId -> cartService.removeFromCart(session, bookId));

                redirectAttributes.addFlashAttribute("successMessage", message("backend.cart.created"));
                return "redirect:/member/borrow/management?tab=borrowing";
            }

            if ("BANK".equalsIgnoreCase(paymentMethod) && previewFee.compareTo(BigDecimal.ZERO) > 0) {
                com.lms.entity.PayOsPayment payment = payOsPaymentService.createTopUp(member, previewFee);
                redirectAttributes.addFlashAttribute("successMessage", message("backend.borrow.requestSubmittedWithBank"));
                return "redirect:/member/payments/payos/" + payment.getOrderCode();
            }

            borrowService.memberSubmitMultiBookBorrowRequest(principal.getName(), selectedBookIds, numberOfDays);
            selectedBookIds.forEach(bookId -> cartService.removeFromCart(session, bookId));

            redirectAttributes.addFlashAttribute("successMessage", message("backend.cart.created"));
            return "redirect:/member/borrow/management?tab=borrowing";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.cart.creationFailed", e));
            return "redirect:/member/cart";
        }
    }
}