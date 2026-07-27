package com.lms.controller.member;

import com.lms.exception.ResourceNotFoundException;
import com.lms.controller.LocalizedControllerSupport;
import com.lms.entity.Book;
import com.lms.entity.Member;
import com.lms.repository.MemberRepository;
import com.lms.repository.WalletRepository;
import com.lms.repository.SystemSettingRepository;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BorrowDetailRepository;
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

    private static final String CART_SUBMISSION_TOKEN = "memberCartSubmissionToken";

    private final CartService cartService;
    private final BorrowService borrowService;
    private final BookService bookService;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;
    private final com.lms.service.PayOsPaymentService payOsPaymentService;
    private final SystemSettingRepository systemSettingRepository;
    private final BookItemRepository bookItemRepository;
    private final BorrowDetailRepository borrowDetailRepository;

    public CartController(CartService cartService, BorrowService borrowService, BookService bookService,
            MemberRepository memberRepository, WalletRepository walletRepository,
            com.lms.service.PayOsPaymentService payOsPaymentService,
            SystemSettingRepository systemSettingRepository,
            BookItemRepository bookItemRepository,
            BorrowDetailRepository borrowDetailRepository) {
        this.cartService = cartService;
        this.borrowService = borrowService;
        this.bookService = bookService;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
        this.payOsPaymentService = payOsPaymentService;
        this.systemSettingRepository = systemSettingRepository;
        this.bookItemRepository = bookItemRepository;
        this.borrowDetailRepository = borrowDetailRepository;
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
            String errorMessage = message("backend.cart.invalidBook");
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body(java.util.Map.of("success", false, "message", errorMessage));
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/books";
        }
        if (!"Active".equalsIgnoreCase(book.getStatus())) {
            String errorMessage = message("backend.cart.unavailable");
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return org.springframework.http.ResponseEntity
                        .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body(java.util.Map.of("success", false, "message", errorMessage));
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/books/" + bookId;
        }

        // 1. Kiểm tra số lượng bản sao khả dụng thực tế trong kho bằng hàm
        // JpaRepository của bạn
        long availableStock = bookItemRepository.countByBook_BookIdAndStatusIgnoreCase(bookId, "Available");
        Member member = memberRepository.findByAccountUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException(message("backend.cart.memberNotFound")));
        int remainingBorrowLimit = getRemainingBorrowLimit(member);

        // 2. Lấy số lượng cuốn sách này hiện đã nằm trong giỏ sách Session
        int currentInCart = cartService.getQuantityInCart(session, bookId);
        if (cartService.getCartCount(session) >= remainingBorrowLimit) {
            return cartLimitError(requestedWith, redirectAttributes, referer, bookId, remainingBorrowLimit);
        }

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
                    .location(java.net.URI.create(localRedirectTarget(referer, "/books/" + bookId)))
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
                .location(java.net.URI.create(localRedirectTarget(referer, "/books/" + bookId)))
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
                .orElseThrow(() -> new ResourceNotFoundException(message("backend.cart.memberNotFound")));
        int remainingBorrowLimit = getRemainingBorrowLimit(member);
        trimCartToBorrowLimit(session, remainingBorrowLimit);

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
        model.addAttribute("remainingBorrowLimit", remainingBorrowLimit);
        BigDecimal borrowFeePerBook = systemSettingRepository.findBySettingKeyIgnoreCase("BORROW_FEE_PER_BOOK")
                .map(setting -> {
                    try {
                        BigDecimal value = new BigDecimal(setting.getSettingValue().trim());
                        return value.signum() >= 0 ? value : BigDecimal.valueOf(5000);
                    } catch (Exception ignored) {
                        return BigDecimal.valueOf(5000);
                    }
                })
                .orElse(BigDecimal.valueOf(5000));
        model.addAttribute("borrowFeePerBook", borrowFeePerBook);

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
        Member member = memberRepository.findByAccountUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException(message("backend.cart.memberNotFound")));
        if (requestedBookIds.size() > getRemainingBorrowLimit(member)) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.tierLimitExceeded"));
            return "redirect:/member/cart";
        }
        @SuppressWarnings("unchecked")
        List<Integer> cartBookIds = session.getAttribute("BOOK_CART") instanceof List<?>
                ? (List<Integer>) session.getAttribute("BOOK_CART")
                : List.of();
        java.util.Map<Integer, Long> requestedCounts = requestedBookIds.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        java.util.function.Function.identity(),
                        java.util.stream.Collectors.counting()));
        boolean selectionMatchesCart = selectionMatchesCart(requestedCounts, cartBookIds);
        if (!selectionMatchesCart) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
            return "redirect:/member/cart";
        }
        if (!hasAvailableStock(requestedCounts, redirectAttributes)) {
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

        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                .orElse(BigDecimal.ZERO);

        model.addAttribute("cartItems", selectedCartItems);
        model.addAttribute("selectedBookIds", validSelectedBookIds);
        model.addAttribute("numberOfDays", numberOfDays);
        model.addAttribute("totalFee", previewFee);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("afterBalance", walletBalance.subtract(previewFee));
        String submissionToken = java.util.UUID.randomUUID().toString();
        session.setAttribute(CART_SUBMISSION_TOKEN, submissionToken);
        model.addAttribute("cartSubmissionToken", submissionToken);
        return "member/cart-checkout";
    }

    @PostMapping("/submit")
    public String submitCartRequest(@RequestParam("numberOfDays") Integer numberOfDays,
            @RequestParam(value = "selectedBookIds", required = false) List<Integer> selectedBookIds,
            @RequestParam(value = "paymentMethod", defaultValue = "WALLET") String paymentMethod,
            @RequestParam(value = "submissionToken", required = false) String submissionToken,
            HttpSession session, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/login";

        if (selectedBookIds == null || selectedBookIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.missingSelection"));
            return "redirect:/member/cart";
        }
        synchronized (session) {
            Object expectedToken = session.getAttribute(CART_SUBMISSION_TOKEN);
            if (submissionToken == null || !submissionToken.equals(expectedToken)) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.submissionExpired"));
                return "redirect:/member/cart";
            }
            session.removeAttribute(CART_SUBMISSION_TOKEN);
        }
        List<Integer> normalizedSelectedBookIds = selectedBookIds.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
        if (normalizedSelectedBookIds.size() != selectedBookIds.size()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
            return "redirect:/member/cart";
        }
        Member member = memberRepository.findByAccountUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException(message("backend.cart.memberNotFound")));
        if (normalizedSelectedBookIds.size() > getRemainingBorrowLimit(member)) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.borrow.tierLimitExceeded"));
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
        java.util.Map<Integer, Long> selectedCounts = normalizedSelectedBookIds.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        java.util.function.Function.identity(),
                        java.util.stream.Collectors.counting()));
        boolean allInCart = selectionMatchesCart(selectedCounts, cartBookIds);

        if (!allInCart) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
            return "redirect:/member/cart";
        }
        if (!hasAvailableStock(selectedCounts, redirectAttributes)) {
            return "redirect:/member/cart";
        }

        try {
            BigDecimal previewFee = borrowService.calculateBorrowFeePreview(
                    principal.getName(), normalizedSelectedBookIds, numberOfDays);
            BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                    .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                    .orElse(BigDecimal.ZERO);

            if ("WALLET".equals(normalizedPaymentMethod)) {
                if (walletBalance.compareTo(previewFee) < 0) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            message("backend.borrow.insufficientWalletBalance"));
                    return "redirect:/member/cart";
                }

                borrowService.memberSubmitMultiBookBorrowRequest(
                        principal.getName(), normalizedSelectedBookIds, numberOfDays);
                normalizedSelectedBookIds.forEach(bookId -> cartService.removeFromCart(session, bookId));

                redirectAttributes.addFlashAttribute("successMessage", message("backend.cart.created"));
                return "redirect:/member/borrow/management?tab=borrowing";
            }

            if ("BANK".equals(normalizedPaymentMethod) && previewFee.compareTo(BigDecimal.ZERO) > 0) {
                com.lms.entity.Borrow pendingBorrow = null;
                try {
                    pendingBorrow = borrowService.memberSubmitBankMultiBookBorrowRequest(
                            principal.getName(), normalizedSelectedBookIds, numberOfDays);
                    com.lms.entity.PayOsPayment payment = payOsPaymentService.createBorrowFeePayment(member, pendingBorrow.getBorrowId());
                    normalizedSelectedBookIds.forEach(bookId -> cartService.removeFromCart(session, bookId));
                    return "redirect:/member/payments/payos/" + payment.getOrderCode();
                } catch (Exception paymentError) {
                    if (pendingBorrow != null && pendingBorrow.getBorrowId() != null) {
                        borrowService.cancelPendingBankBorrow(pendingBorrow.getBorrowId(), "CREATE_FAILED");
                    }
                    throw paymentError;
                }
            }

            borrowService.memberSubmitMultiBookBorrowRequest(
                    principal.getName(), normalizedSelectedBookIds, numberOfDays);
            normalizedSelectedBookIds.forEach(bookId -> cartService.removeFromCart(session, bookId));

            redirectAttributes.addFlashAttribute("successMessage", message("backend.cart.created"));
            return "redirect:/member/borrow/management?tab=borrowing";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.cart.creationFailed", e));
            return "redirect:/member/cart";
        }
    }

    private boolean hasAvailableStock(java.util.Map<Integer, Long> requestedCounts,
            RedirectAttributes redirectAttributes) {
        for (java.util.Map.Entry<Integer, Long> entry : requestedCounts.entrySet()) {
            long availableCopies = bookItemRepository
                    .countByBook_BookIdAndStatusIgnoreCase(entry.getKey(), "Available");
            if (entry.getValue() > availableCopies) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        message("backend.borrow.stockExceeded", availableCopies));
                return false;
            }
        }
        return true;
    }

    static boolean selectionMatchesCart(java.util.Map<Integer, Long> requestedCounts, List<Integer> cartBookIds) {
        if (requestedCounts.isEmpty() || cartBookIds == null || cartBookIds.isEmpty()) {
            return false;
        }
        java.util.Map<Integer, Long> cartCounts = cartBookIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(
                        java.util.function.Function.identity(),
                        java.util.stream.Collectors.counting()));
        return requestedCounts.entrySet().stream()
                .allMatch(entry -> entry.getValue() <= cartCounts.getOrDefault(entry.getKey(), 0L));
    }

    static String localRedirectTarget(String referer, String fallback) {
        if (referer == null || referer.isBlank()) {
            return fallback;
        }
        try {
            java.net.URI uri = java.net.URI.create(referer);
            String path = uri.getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return fallback;
            }
            StringBuilder target = new StringBuilder(path);
            if (uri.getQuery() != null) {
                target.append('?').append(uri.getQuery());
            }
            if (uri.getFragment() != null) {
                target.append('#').append(uri.getFragment());
            }
            return target.toString();
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private int getRemainingBorrowLimit(Member member) {
        int configuredLimit = getPositiveIntSetting("Max_Books_Per_Member",
                getPositiveIntSetting("MAX_BOOKS_PER_MEMBER", 10));
        Integer tierLimit = memberRepository.findCurrentBorrowLimitByMemberId(member.getMemberId())
                .orElse(member.getTier() != null ? member.getTier().getBorrowLimit() : null);
        int effectiveLimit = Math.max(1, tierLimit != null ? tierLimit : configuredLimit);
        long currentlyBorrowed = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        return Math.max(0, effectiveLimit - Math.toIntExact(currentlyBorrowed));
    }

    private int getPositiveIntSetting(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyIgnoreCase(key)
                    .map(setting -> setting.getSettingValue())
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .filter(value -> value > 0)
                    .orElse(defaultValue);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private void trimCartToBorrowLimit(HttpSession session, int remainingBorrowLimit) {
        Object storedCart = session.getAttribute("BOOK_CART");
        if (!(storedCart instanceof List<?> rawCart)) {
            return;
        }
        List<Integer> safeCart = rawCart.stream()
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .limit(remainingBorrowLimit)
                .toList();
        session.setAttribute("BOOK_CART", new java.util.ArrayList<>(safeCart));
    }

    private Object cartLimitError(String requestedWith, RedirectAttributes redirectAttributes,
            String referer, Integer bookId, int remainingBorrowLimit) {
        String errorMessage = message("backend.cart.borrowLimitReached", remainingBorrowLimit);
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("success", false, "message", errorMessage));
        }
        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        redirectAttributes.addFlashAttribute("error", errorMessage);
        return org.springframework.http.ResponseEntity
                .status(org.springframework.http.HttpStatus.SEE_OTHER)
                .location(java.net.URI.create(localRedirectTarget(referer, "/books/" + bookId)))
                .build();
    }
}
