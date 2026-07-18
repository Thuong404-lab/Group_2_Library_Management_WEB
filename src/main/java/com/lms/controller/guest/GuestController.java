package com.lms.controller.guest;
import com.lms.exception.ApplicationException;
import com.lms.controller.LocalizedControllerSupport;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.lms.service.BookService;
import com.lms.repository.GenreRepository;
import com.lms.service.MemberFavoriteService;
import com.lms.service.MemberReviewService;
import com.lms.service.MembershipService;
import com.lms.service.SystemService;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.lms.entity.Book;
import com.lms.entity.Feedback;
import com.lms.dto.request.MemberReviewSubmitRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
/**
 * GuestController - Trang công khai cho Khách (Guest)
 * Người phụ trách: Nguyễn Tiến Thương (CE191329)
 */
@Controller
public class GuestController extends LocalizedControllerSupport {

    private final BookService bookService;
    private final GenreRepository genreRepository;
    private final MemberFavoriteService memberFavoriteService;
    private final MemberReviewService memberReviewService;
    private final MembershipService membershipService;
    private final SystemService systemService;

    public GuestController(BookService bookService,
                           GenreRepository genreRepository,
                           MemberFavoriteService memberFavoriteService,
                           MemberReviewService memberReviewService,
                           MembershipService membershipService,
                           SystemService systemService) {
        this.bookService = bookService;
        this.genreRepository = genreRepository;
        this.memberFavoriteService = memberFavoriteService;
        this.memberReviewService = memberReviewService;
        this.membershipService = membershipService;
        this.systemService = systemService;
    }

    // Trang chủ
    @GetMapping("/")
    public String homePage(Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                return "redirect:/admin/dashboard";
            }
            boolean isLibrarian = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
            if (isLibrarian) {
                return "redirect:/librarian/dashboard";
            }
        }

        List<Book> books = bookService.getRecentBooks(6);
        List<Book> trendingBooks = bookService.getTrendingBooks(6);
        model.addAttribute("books", books);
        model.addAttribute("trendingBooks", trendingBooks);
        addFavoriteBookIds(model, authentication);
        return "index";
    }

    // UC-1 & UC-3: Xem danh sách sách và Tìm kiếm
    @GetMapping("/books")
    public String viewBookList(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Integer genreId,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false, defaultValue = "newest") String sort,
                               @RequestParam(defaultValue = "0") int page,
                               Model model, java.security.Principal principal) {
        
        Sort sorting;
        if ("title_asc".equals(sort)) sorting = Sort.by("title").ascending();
        else if ("title_desc".equals(sort)) sorting = Sort.by("title").descending();
        else if ("oldest".equals(sort)) sorting = Sort.by("bookId").ascending();
        else sorting = Sort.by(Sort.Direction.DESC, "bookId");

        Pageable pageable = PageRequest.of(page, 12, sorting);
        
        Page<Book> bookPage;
        if ((keyword != null && !keyword.trim().isEmpty()) || genreId != null || (status != null && !status.trim().isEmpty())) {
            bookPage = bookService.searchBooks(keyword, genreId, status, pageable);
        } else {
            bookPage = bookService.findAllBooks(pageable);
        }
        
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("genres", genreRepository.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedGenreId", genreId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("sort", sort);
        
        addFavoriteBookIds(model, principal);
        
        return "guest/books";
    }

    // Giới thiệu thư viện
    @GetMapping("/about")
    public String aboutPage(Model model) {
        Map<String, String> settings = systemService.getSettingMap();

        // Pass string values or default fallbacks
        model.addAttribute("maxBorrowDays", settings.getOrDefault("Max_Borrow_Days", "14"));
        model.addAttribute("maxRenewalDays", settings.getOrDefault("Max_Renewal_Days", "7"));
        model.addAttribute("damageCompensationThreshold", settings.getOrDefault("Damage_Compensation_Threshold", "50"));
        model.addAttribute("overdueViolationLockLimit", settings.getOrDefault("Overdue_Violation_Lock_Limit", "3"));

        // Parse numeric currency values
        try {
            model.addAttribute("finePerDay", Long.parseLong(settings.getOrDefault("Fine_Per_Day", "5000")));
        } catch (NumberFormatException e) {
            model.addAttribute("finePerDay", 5000L);
        }

        try {
            model.addAttribute("borrowFeePerBook", Long.parseLong(settings.getOrDefault("Borrow_Fee_Per_Book", "5000")));
        } catch (NumberFormatException e) {
            model.addAttribute("borrowFeePerBook", 5000L);
        }

        try {
            model.addAttribute("depositAmount", Long.parseLong(settings.getOrDefault("Deposit_Amount", "50000")));
        } catch (NumberFormatException e) {
            model.addAttribute("depositAmount", 50000L);
        }

        try {
            model.addAttribute("damageCompensationAmount", Long.parseLong(settings.getOrDefault("Damage_Compensation_Amount", "120000")));
        } catch (NumberFormatException e) {
            model.addAttribute("damageCompensationAmount", 120000L);
        }

        return "guest/about";
    }

    @GetMapping("/membership-tiers")
    public String membershipTiersPage(Model model) {
        model.addAttribute("tiers", membershipService.getAllTiers());
        return "guest/membership-tiers";
    }

    // UC-3: View Book Detail - Xem chi tiết một quyển sách
    @GetMapping("/books/{id}")
    public String viewBookDetail(@PathVariable Integer id, Model model, Principal principal) {
        Book book = bookService.findBookById(id);
        List<Feedback> bookReviews = memberReviewService.getApprovedReviewsByBookId(id);
        double averageRating = bookReviews.stream()
                .map(Feedback::getRating)
                .filter(rating -> rating != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        model.addAttribute("book", book);
        model.addAttribute("bookReviews", bookReviews);
        model.addAttribute("reviewCount", bookReviews.size());
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("reviewBorrowEligible", false);
        model.addAttribute("reviewAlreadySubmitted", false);
        if (principal != null) {
            try {
                model.addAttribute("reviewBorrowEligible",
                        memberReviewService.isEligibleToReview(principal.getName(), id));
                model.addAttribute("reviewAlreadySubmitted",
                        memberReviewService.hasActiveReview(principal.getName(), id));
            } catch (ApplicationException ignored) {
                // Non-member authenticated accounts cannot submit member reviews.
            }
        }
        if (!model.containsAttribute("reviewRequest")) {
            MemberReviewSubmitRequest reviewRequest = new MemberReviewSubmitRequest();
            reviewRequest.setBookId(id);
            model.addAttribute("reviewRequest", reviewRequest);
        }
        addFavoriteBookIds(model, principal);
        return "guest/book-detail";
    }

    private void addFavoriteBookIds(Model model, Principal principal) {
        if (principal == null) {
            model.addAttribute("favoriteBookIds", Collections.emptySet());
            return;
        }

        try {
            model.addAttribute("favoriteBookIds", memberFavoriteService.getMyFavoriteBookIds(principal.getName()));
        } catch (ApplicationException e) {
            model.addAttribute("favoriteBookIds", Collections.emptySet());
        }
    }

    // API for Live Search Suggestion
    @GetMapping("/api/books/search/suggest")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> suggestBooks(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return org.springframework.http.ResponseEntity.ok(Collections.emptyList());
        }
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("title").ascending());
        org.springframework.data.domain.Page<Book> bookPage = bookService.searchBooks(keyword, null, null, pageable);
        
        List<Map<String, Object>> suggestions = bookPage.getContent().stream().map(book -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("bookId", book.getBookId());
            map.put("title", book.getTitle());
            String authorNames = "";
            if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
                authorNames = book.getAuthors().stream()
                        .map(com.lms.entity.Author::getAuthorName)
                        .collect(Collectors.joining(", "));
            } else {
                authorNames = message("book.multipleAuthors");
            }
            map.put("authorName", authorNames);
            map.put("thumbnailUrl", book.getCoverImageUrl() != null && !book.getCoverImageUrl().trim().isEmpty() ? book.getCoverImageUrl() : "https://picsum.photos/seed/" + book.getBookId() + "/600/800");
            return map;
        }).toList();
        
        return org.springframework.http.ResponseEntity.ok(suggestions);
    }
}
