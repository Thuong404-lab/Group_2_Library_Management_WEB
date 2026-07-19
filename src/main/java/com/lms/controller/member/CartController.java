package com.lms.controller.member;

import com.lms.controller.LocalizedControllerSupport;
import com.lms.entity.Book;
import com.lms.entity.Member;
import com.lms.repository.MemberRepository;
import com.lms.repository.WalletRepository;
import com.lms.service.BorrowService;
import com.lms.service.BookService;
import com.lms.service.CartService;
import jakarta.servlet.http.HttpSession;
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
    private final BookService bookService;
    private final MemberRepository memberRepository;
    private final WalletRepository walletRepository;

    public CartController(CartService cartService, BorrowService borrowService, BookService bookService,
                          MemberRepository memberRepository, WalletRepository walletRepository) {
        this.cartService = cartService;
        this.borrowService = borrowService;
        this.bookService = bookService;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("bookId") Integer bookId, HttpSession session,
                            Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        Book book;
        try {
            book = bookService.findBookById(bookId);
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", message("backend.cart.invalidBook"));
            return "redirect:/books";
        }

        if (!"Active".equalsIgnoreCase(book.getStatus())) {
            redirectAttributes.addFlashAttribute("error", message("backend.cart.unavailable"));
            return "redirect:/books/" + bookId;
        }

        boolean added = cartService.addToCart(session, bookId);
        redirectAttributes.addFlashAttribute("success",
                message(added ? "backend.cart.added" : "backend.cart.alreadyAdded"));
        return "redirect:/books/" + bookId;
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam("bookId") Integer bookId, HttpSession session) {
        cartService.removeFromCart(session, bookId);
        return "redirect:/member/cart";
    }

    @GetMapping
    public String viewCart(HttpSession session, Principal principal, Model model) {
        if (principal == null) return "redirect:/login";

        Member member = memberRepository.findByAccountUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException(message("backend.cart.memberNotFound")));

        BigDecimal walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance())
                .orElse(BigDecimal.ZERO);

        List<Book> cartItems = cartService.getCartItems(session);
        double discountPercent = (member.getTier() != null && member.getTier().getDiscountPercent() != null)
                ? member.getTier().getDiscountPercent().doubleValue() : 0.0;

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("discountPercent", discountPercent);
        return "member/cart";
    }

    @GetMapping("/checkout")
    public String checkoutCart(@RequestParam(value = "numberOfDays", defaultValue = "14") Integer numberOfDays,
                               @RequestParam(value = "selectedBookIds", required = false) List<Integer> selectedBookIds,
                               HttpSession session, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (selectedBookIds == null || selectedBookIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.selectionRequired"));
            return "redirect:/member/cart";
        }

        List<Integer> normalizedBookIds = selectedBookIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        java.util.Map<Integer, Book> cartItemsById = cartService.getCartItems(session).stream()
                .collect(java.util.stream.Collectors.toMap(Book::getBookId, item -> item));
        List<Book> selectedCartItems = normalizedBookIds.stream()
                .map(cartItemsById::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (selectedCartItems.isEmpty() || selectedCartItems.size() != normalizedBookIds.size()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
            return "redirect:/member/cart";
        }

        List<Integer> validSelectedBookIds = selectedCartItems.stream().map(Book::getBookId).toList();
        BigDecimal previewFee = borrowService.calculateBorrowFeePreview(principal.getName(), validSelectedBookIds, numberOfDays);

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
                                    HttpSession session, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";

        if (selectedBookIds == null || selectedBookIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.missingSelection"));
            return "redirect:/member/cart";
        }

        try {
            List<Integer> normalizedBookIds = selectedBookIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            java.util.Set<Integer> cartBookIds = cartService.getCartItems(session).stream()
                    .map(Book::getBookId)
                    .collect(java.util.stream.Collectors.toSet());
            if (normalizedBookIds.isEmpty() || !cartBookIds.containsAll(normalizedBookIds)) {
                redirectAttributes.addFlashAttribute("errorMessage", message("backend.cart.invalidSelection"));
                return "redirect:/member/cart";
            }

            borrowService.memberSubmitMultiBookBorrowRequest(principal.getName(), normalizedBookIds, numberOfDays);
            // Đơn mượn lập thành công -> chỉ xóa những sách đã đăng ký khỏi giỏ hàng
            normalizedBookIds.forEach(bookId -> cartService.removeFromCart(session, bookId));
            redirectAttributes.addFlashAttribute("successMessage", message("backend.cart.created"));
            return "redirect:/member/borrow/management?tab=borrowing";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", messageWithDetail("backend.cart.creationFailed", e));
            return "redirect:/member/cart";
        }
    }
}
