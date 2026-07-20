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
import com.lms.service.BookService;
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
import java.util.Locale;

@Controller
@RequestMapping("/member/cart")
public class CartController extends LocalizedControllerSupport {

    private final CartService cartService;
    private final BorrowService borrowService;
    private final BookService bookService;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final com.lms.service.PayOsPaymentService payOsPaymentService;
    private final SystemSettingRepository systemSettingRepository;
    private final BookItemRepository bookItemRepository;

    public CartController(CartService cartService, BorrowService borrowService, BookService bookService,
            MemberRepository memberRepository, WalletRepository walletRepository,
            com.lms.service.PayOsPaymentService payOsPaymentService,
            SystemSettingRepository systemSettingRepository,
            BookItemRepository bookItemRepository) {
        this.cartService = cartService;
        this.borrowService = borrowService;
        this.bookService = bookService;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.payOsPaymentService = payOsPaymentService;
        this.systemSettingRepository = systemSettingRepository;
        this.bookItemRepository = bookItemRepository;
    }

    @PostMapping("/add")
    public Object addToCart(@RequestParam("bookId") Integer bookId, HttpSession session,
            Principal principal,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "referer", required = false) String referer,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        if (principal == null)
            return "redirect:/login";

        Book book;
        try {
            book = bookService.findBookById(bookId);
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidBook"));
            redirectAttributes.addFlashAttribute("error", message("backend.cart.invalidBook"));
            return "redirect:/books";
        }
        if (!"Active".equalsIgnoreCase(book.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.unavailable"));
            redirectAttributes.addFlashAttribute("error", message("backend.cart.unavailable"));
            return "redirect:/books/" + bookId;
        }

        // 1. Kiểm tra số lượng bản sao khả dụng thực tế trong kho bằng hàm
        // JpaRepository của bạn
        long availableStock = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, "Available");

        // 2. Lấy số lượng cuốn sách này hiện đã nằm trong giỏ sách Session
        int currentInCart = cartService.getQuantityInCart(session, bookId);

        // 3. Chặn nếu số lượng thêm vào vượt số lượng khả dụng trong kho
        if (currentInCart >= availableStock) {
            String errorMessage = message("backend.cart.stockLimitReached", availableStock);

            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body(java.util.Map.of("success", false, "message", errorMessage));
            }

            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.SEE_OTHER)
                    .location(java.net.URI.create(referer != null && !referer.isBlank() ? referer : "/books/" + bookId))
                    .build();
        }

        // 4. Đủ điều kiện, thêm vào giỏ hàng
        cartService.addToCart(session, bookId);
        String successMessage = message("backend.cart.added");

        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return org.springframework.http.ResponseEntity
                    .ok(java.util.Map.of("success", true, "message", successMessage));
        }
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        redirectAttributes.addFlashAttribute("success", successMessage);
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
        if (principal == null)
            return "redirect:/login";

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
                ? member.getTier().getDiscountPercent().doubleValue()
                : 0.0;

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
                .map(s -> {
                    try {
                        return Integer.parseInt(s.getSettingValue());
                    } catch (Exception e) {
                        return 30;
                    }
                })
                .orElse(30);
        model.addAttribute("maxBorrowDays", maxBorrowDays);
        return "member/cart";
    }

    @GetMapping("/checkout")
    public String checkoutCart(@RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
            @RequestParam(value = "selectedBookIds", required = false) List<Integer> selectedBookIds,
            HttpSession session, Principal principal, Model model, RedirectAttributes redirectAttributes,
            HttpServletResponse response) {
        if (principal == null)
            return "redirect:/login";
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (selectedBookIds == null || selectedBookIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.selectionRequired"));
            return "redirect:/member/cart";
        }

        List<Integer> requestedBookIds = selectedBookIds.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
        @SuppressWarnings("unchecked")
        List<Integer> cartBookIds = session.getAttribute("BOOK_CART") instanceof List<?>
                ? (List<Integer>) session.getAttribute("BOOK_CART")
                : List.of();
        java.util.Map<Integer, Long> requestedCounts = requestedBookIds.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        java.util.function.Function.identity(),
                        java.util.stream.Collectors.counting()));
        boolean selectionMatchesCart = !requestedCounts.isEmpty() && requestedCounts.entrySet().stream()
                .allMatch(entry -> java.util.Collections.frequency(cartBookIds, entry.getKey()) >= entry.getValue());
        if (!selectionMatchesCart) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
            return "redirect:/member/cart";
        }

        List<Book> allCartItems = cartService.getCartItems(session);
        List<Book> selectedCartItems = new java.util.ArrayList<>();
        for (Integer id : requestedBookIds) {
            allCartItems.stream()
                    .filter(item -> item.getBookId().equals(id))
                    .findFirst()
                    .ifPresent(selectedCartItems::add);
        }

        if (selectedCartItems.isEmpty() || selectedCartItems.size() != requestedBookIds.size()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
            return "redirect:/member/cart";
        }

        List<Integer> validSelectedBookIds = selectedCartItems.stream().map(Book::getBookId).toList();
        BigDecimal previewFee = borrowService.calculateBorrowFeePreview(principal.getName(), validSelectedBookIds,
                numberOfDays);

        Member member = memberRepository.findByAccountUsername(principal.getName()).orElseThrow();
        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                .orElse(BigDecimal.ZERO);

        model.addAttribute("cartItems", selectedCartItems);
        model.addAttribute("selectedBookIds", validSelectedBookIds);
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
        if (principal == null)
            return "redirect:/login";

        if (selectedBookIds == null || selectedBookIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.missingSelection"));
            return "redirect:/member/cart";
        }

        String normalizedPaymentMethod = paymentMethod == null
                ? ""
                : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!"WALLET".equals(normalizedPaymentMethod) && !"BANK".equals(normalizedPaymentMethod)) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidPaymentMethod"));
            return "redirect:/member/cart";
        }

        @SuppressWarnings("unchecked")
        List<Integer> cartBookIds = session.getAttribute("BOOK_CART") instanceof List<?>
                ? (List<Integer>) session.getAttribute("BOOK_CART")
                : List.of();
        java.util.Map<Integer, Long> selectedCounts = selectedBookIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(
                        java.util.function.Function.identity(),
                        java.util.stream.Collectors.counting()));
        boolean allInCart = !selectedCounts.isEmpty() && selectedCounts.entrySet().stream()
                .allMatch(entry -> java.util.Collections.frequency(cartBookIds, entry.getKey()) >= entry.getValue());

        if (!allInCart) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
            return "redirect:/member/cart";
        }

        try {
            Member member = memberRepository.findByAccountUsername(principal.getName())
                    .orElseThrow(() -> new ResourceNotFoundException(message("backend.cart.memberNotFound")));
            BigDecimal previewFee = borrowService.calculateBorrowFeePreview(principal.getName(), selectedBookIds, numberOfDays);
            BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                    .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                    .orElse(BigDecimal.ZERO);

            if ("WALLET".equals(normalizedPaymentMethod)) {
                if (walletBalance.compareTo(previewFee) < 0) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            message("backend.borrow.insufficientWalletBalance"));
                    return "redirect:/member/cart";
                }

                borrowService.memberSubmitMultiBookBorrowRequest(principal.getName(), selectedBookIds, numberOfDays);
                selectedBookIds.forEach(bookId -> cartService.removeFromCart(session, bookId));

                redirectAttributes.addFlashAttribute("successMessage", message("backend.cart.created"));
                return "redirect:/member/borrow/management?tab=borrowing";
            }

            if ("BANK".equals(normalizedPaymentMethod) && previewFee.compareTo(BigDecimal.ZERO) > 0) {
                com.lms.entity.Borrow pendingBorrow = null;
                try {
                    pendingBorrow = borrowService.memberSubmitBankMultiBookBorrowRequest(principal.getName(), selectedBookIds, numberOfDays);
                    com.lms.entity.PayOsPayment payment = payOsPaymentService.createBorrowFeePayment(member, pendingBorrow.getBorrowId());
                    selectedBookIds.forEach(bookId -> cartService.removeFromCart(session, bookId));
                    return "redirect:/member/payments/payos/" + payment.getOrderCode();
                } catch (Exception paymentError) {
                    if (pendingBorrow != null && pendingBorrow.getBorrowId() != null) {
                        borrowService.cancelPendingBankBorrow(pendingBorrow.getBorrowId(), "CREATE_FAILED");
                    }
                    throw paymentError;
                }
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
