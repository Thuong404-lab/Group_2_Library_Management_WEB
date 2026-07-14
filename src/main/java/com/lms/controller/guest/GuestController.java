package com.lms.controller.guest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.lms.service.BookService;
import com.lms.repository.GenreRepository;
import com.lms.repository.MemberRepository;       // Thêm import cho MemberRepository
import com.lms.repository.WalletRepository;       // Thêm import cho WalletRepository
import com.lms.service.MemberFavoriteService;
import com.lms.service.MemberReviewService;

import java.math.BigDecimal;
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
public class GuestController {

    private final BookService bookService;
    private final GenreRepository genreRepository;
    private final MemberFavoriteService memberFavoriteService;
    private final MemberReviewService memberReviewService;
    private final MemberRepository memberRepository; // Thêm khai báo thuộc tính sửa lỗi dòng 117
    private final WalletRepository walletRepository; // Thêm khai báo thuộc tính sửa lỗi dòng 119

    // Constructor Injection đầy đủ tất cả các Repository và Service cần dùng
    public GuestController(BookService bookService,
                           GenreRepository genreRepository,
                           MemberFavoriteService memberFavoriteService,
                           MemberReviewService memberReviewService,
                           MemberRepository memberRepository,
                           WalletRepository walletRepository) {
        this.bookService = bookService;
        this.genreRepository = genreRepository;
        this.memberFavoriteService = memberFavoriteService;
        this.memberReviewService = memberReviewService;
        this.memberRepository = memberRepository;
        this.walletRepository = walletRepository;
    }

    // Trang chủ
    @GetMapping("/")
    public String homePage(Model model, Principal principal) {
        List<Book> books = bookService.getRecentBooks(6);
        List<Book> trendingBooks = bookService.getTrendingBooks(6);
        model.addAttribute("books", books);
        model.addAttribute("trendingBooks", trendingBooks);
        addFavoriteBookIds(model, principal);
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
    public String aboutPage() {
        return "guest/about";
    }

    // UC-3: View Book Detail - Xem chi tiết một quyển sách
    @GetMapping("/books/{id}")
    public String viewBookDetail(@PathVariable Integer id, Model model, Principal principal) {
        Book book = bookService.findBookById(id);
        List<Feedback> bookReviews = memberReviewService.getApprovedReviewsByBookId(id);
        double averageRating = bookReviews.stream().map(Feedback::getRating).filter(r -> r != null).mapToInt(Integer::intValue).average().orElse(0);

        model.addAttribute("book", book);
        model.addAttribute("bookReviews", bookReviews);
        model.addAttribute("reviewCount", bookReviews.size());
        model.addAttribute("averageRating", averageRating);

        // --- DỮ LIỆU TÀI CHÍNH CHO MODAL ĐĂNG KÝ MƯỢN ĐƠN LẺ ---
        BigDecimal walletBalance = BigDecimal.ZERO;
        double discountPercent = 0.0;
        if (principal != null) {
            try {
                com.lms.entity.Member member = memberRepository.findByAccountUsername(principal.getName()).orElse(null);
                if (member != null) {
                    walletBalance = walletRepository.findByMemberMemberId(member.getMemberId())
                            .map(w -> w.getBalance() == null ? BigDecimal.ZERO : w.getBalance()).orElse(BigDecimal.ZERO);
                    discountPercent = (member.getTier() != null && member.getTier().getDiscountPercent() != null)
                            ? member.getTier().getDiscountPercent().doubleValue() : 0.0;
                }
            } catch(Exception ignored){}
        }
        model.addAttribute("walletBalance", walletBalance);
        model.addAttribute("discountPercent", discountPercent);
        model.addAttribute("borrowFeePerDay", 5000);
        // -----------------------------------------------------------

        if (!model.containsAttribute("reviewRequest")) {
            MemberReviewSubmitRequest reviewRequest = new MemberReviewSubmitRequest();
            reviewRequest.setBookId(id);
            model.addAttribute("reviewRequest", reviewRequest);
        }
        addFavoriteBookIds(model, principal);
        return "guest/book-detail";
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
                authorNames = "Nhiều tác giả";
            }
            map.put("authorName", authorNames);
            map.put("thumbnailUrl", book.getCoverImageUrl() != null && !book.getCoverImageUrl().trim().isEmpty() ? book.getCoverImageUrl() : "https://picsum.photos/seed/" + book.getBookId() + "/600/800");
            return map;
        }).toList();

        return org.springframework.http.ResponseEntity.ok(suggestions);
    }

    // Hàm bổ trợ xử lý danh sách yêu thích để render lên UI (Sửa lỗi "cannot find symbol method addFavoriteBookIds")
    private void addFavoriteBookIds(Model model, Principal principal) {
        if (principal != null) {
            try {
                // Gọi đúng tên method getMyFavoriteBookIds từ Service của bạn
                java.util.Set<Integer> favoriteBookIds = memberFavoriteService.getMyFavoriteBookIds(principal.getName());
                model.addAttribute("favoriteBookIds", favoriteBookIds != null ? favoriteBookIds : Collections.emptySet());
            } catch (Exception e) {
                model.addAttribute("favoriteBookIds", Collections.emptySet());
            }
        } else {
            model.addAttribute("favoriteBookIds", Collections.emptySet());
        }
    }


}