```java
package com.lms.controller.librarian;

import com.lms.config.CustomUserDetails;
import com.lms.repository.BookItemRepository;
import com.lms.repository.BorrowDetailRepository;
import com.lms.repository.BorrowRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.ReservationRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;

// * Người phụ trách: Trần Ngọc Linh Đang (CE191088)

@Controller
@RequestMapping("/librarian")
public class LibrarianDashboardController {

    private final BorrowRepository borrowRepository;
    private final BorrowDetailRepository borrowDetailRepository;
    private final ReservationRepository reservationRepository;
    private final BookItemRepository bookItemRepository;
    private final MemberRepository memberRepository;

    public LibrarianDashboardController(BorrowRepository borrowRepository,
            BorrowDetailRepository borrowDetailRepository,
            ReservationRepository reservationRepository,
            BookItemRepository bookItemRepository,
            MemberRepository memberRepository) {
        this.borrowRepository = borrowRepository;
        this.borrowDetailRepository = borrowDetailRepository;
        this.reservationRepository = reservationRepository;
        this.bookItemRepository = bookItemRepository;
        this.memberRepository = memberRepository;
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
        model.addAttribute("currentDate", LocalDate.now());

        model.addAttribute("recentBorrows", borrowRepository.findTop5ByOrderByBorrowDateDesc());

        model.addAttribute("dueSoonDetails",
                borrowDetailRepository.findTop5ByStatusIgnoreCaseAndDueDateBetweenOrderByDueDateAsc(
                        "Borrowed",
                        now,
                        now.plusDays(7)
                )
        );

        if (userDetails != null && userDetails.getAccount() != null) {
            model.addAttribute("currentUser", userDetails.getAccount().getUser());
        }

        return "librarian/dashboard";
    }
}```
