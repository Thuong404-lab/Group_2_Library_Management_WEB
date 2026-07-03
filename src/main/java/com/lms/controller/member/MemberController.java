package com.lms.controller.member;

import com.lms.entity.Book;
import com.lms.entity.Member;
import com.lms.repository.BorrowDetailRepository;
import com.lms.service.MembershipService;
import com.lms.service.BorrowService;
import com.lms.service.MemberFavoriteService;
import com.lms.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final MembershipService membershipService;
    private final BorrowDetailRepository borrowDetailRepository;
    private final BorrowService borrowService;
    private final MemberFavoriteService memberFavoriteService;
    private final BookService bookService;

    public MemberController(MembershipService membershipService,
                            BorrowDetailRepository borrowDetailRepository,
                            BorrowService borrowService,
                            MemberFavoriteService memberFavoriteService,
                            BookService bookService) {
        this.membershipService = membershipService;
        this.borrowDetailRepository = borrowDetailRepository;
        this.borrowService = borrowService;
        this.memberFavoriteService = memberFavoriteService;
        this.bookService = bookService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        String username = principal.getName();

        Member member = membershipService.getMemberByUsername(username);
        model.addAttribute("member", member);

        // Đếm số lượng thực tế sách đang mượn
        long activeCount = borrowDetailRepository.countActiveBorrowedBooks(member.getMemberId());
        model.addAttribute("activeBorrowsCount", activeCount);

        // Đồng bộ chính xác chuỗi lọc trạng thái để tương thích với dữ liệu thật trong DB
        model.addAttribute("activeBorrows", borrowService.getBorrowsByMemberAndStatus(username, "Active"));
        model.addAttribute("pendingBorrows", borrowService.getBorrowsByMemberAndStatus(username, "Pending"));
        model.addAttribute("recommendedBooks", memberFavoriteService.getFavoriteBooksByMember(username));

        // Lấy danh sách 10 cuốn sách đầu tiên hiển thị lên grid màn hình chính
        Page<Book> bookPage = bookService.findAllBooks(PageRequest.of(0, 10));
        model.addAttribute("books", bookPage.getContent());

        // Nạp bảng xếp hạng thành viên
        List<Member> leaderBoard = membershipService.getTopMembersBySpending();
        model.addAttribute("leaderBoard", leaderBoard);

        return "member/dashboard";
    }

    @GetMapping("/borrow")
    public String borrow() {
        return "member/borrow";
    }

    @GetMapping("/wallet")
    public String wallet() {
        return "member/wallet";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "redirect:/member/interaction/notifications";
    }
}