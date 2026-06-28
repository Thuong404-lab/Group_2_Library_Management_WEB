package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.entity.Staff;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import com.lms.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.lms.dto.request.LibrarianNotificationSendRequest;
import com.lms.service.LibrarianInteractionService;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Người phụ trách: Trần Ngọc Linh Đang (CE191088)

@Controller
@RequestMapping("/librarian")
public class LibrarianDashboardController {

    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final ReservationRepository reservationRepository;
    private final BookItemRepository bookItemRepository;
    private final MemberRepository memberRepository;
    private final StaffRepository staffRepository;
    private final LibrarianInteractionService librarianInteractionService;

    public LibrarianDashboardController(BorrowRepository borrowRepository,
                                        BorrowDetailRepository borrowDetailRepository,
                                        ReservationRepository reservationRepository,
                                        BookItemRepository bookItemRepository,
                                        MemberRepository memberRepository,
                                        StaffRepository staffRepository,
                                        LibrarianInteractionService librarianInteractionService) {

        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.reservationRepository = reservationRepository;
        this.bookItemRepository = bookItemRepository;
        this.memberRepository = memberRepository;
        this.staffRepository = staffRepository;
        this.librarianInteractionService = librarianInteractionService;
    }

    @GetMapping("/dashboard")
    public String viewDashboard(Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        LocalDateTime now = LocalDateTime.now();

        model.addAttribute("activeBorrows", borrowRepository.countByStatusIgnoreCase("Active"));
        model.addAttribute("pendingReservations", reservationRepository.countByStatusIgnoreCase("Pending"));
        model.addAttribute("overdueDetails", borrowDetailRepository.countByStatusIgnoreCase("Overdue"));
        model.addAttribute("availableItems", bookItemRepository.countByStatusIgnoreCase("Available"));
        model.addAttribute("totalMembers", memberRepository.count());
        model.addAttribute("totalLibrarians", staffRepository.countByStaffTypeIgnoreCase("Librarian"));
        model.addAttribute("currentDate", LocalDate.now());

        model.addAttribute("recentBorrows", borrowRepository.findTop5ByOrderByBorrowDateDesc());

        model.addAttribute("dueSoonDetails",
                borrowDetailRepository.findTop5ByStatusIgnoreCaseAndDueDateBetweenOrderByDueDateAsc(
                        "Borrowed",
                        now,
                        now.plusDays(7)));
        model.addAttribute("reviews", librarianInteractionService.getReviewsForModeration(null, PageRequest.of(0, 20,
                                Sort.by("createdDate").descending())));
        model.addAttribute("notificationRequest", new LibrarianNotificationSendRequest());
        model.addAttribute("members", librarianInteractionService.getAllMembers());
        model.addAttribute("requests", librarianInteractionService.getBookAcquisitionRequests(PageRequest.of(0, 20,
                                Sort.by("createdDate").descending())));
        addCurrentUser(model, userDetails);

        return "librarian/dashboard";
    }

    // View Librarian List
    @GetMapping("/librarians")
    public String viewLibrarianList(@RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PageRequest pageRequest = PageRequest.of(page, 10, Sort.by("staffId").ascending());
        Page<Staff> staffPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            staffPage = staffRepository.searchByStaffTypeAndKeyword("Librarian", keyword.trim(), pageRequest);
        } else {
            staffPage = staffRepository.findByStaffTypeIgnoreCase("Librarian", pageRequest);
        }

        model.addAttribute("staffPage", staffPage);
        model.addAttribute("keyword", keyword);
        addCurrentUser(model, userDetails);
        return "librarian/librarian-list";
    }

    private void addCurrentUser(Model model, CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getAccount() != null) {
            model.addAttribute("currentUser", userDetails.getAccount().getUser());
        }
    }
}