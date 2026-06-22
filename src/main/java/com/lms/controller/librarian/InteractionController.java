package com.lms.controller.librarian;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * InteractionController - Tương tác Thủ thư với Hội viên
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
@Controller
@RequestMapping("/librarian/interaction")
public class InteractionController {

    // UC-15.1: Send Notifications to Members
    @GetMapping("/notifications")
    public String showNotificationForm(Model model) {
        // TODO: Implement - Hiển thị form gửi thông báo
        // TODO: Lấy danh sách Member để chọn người nhận
        return "librarian/notifications";
    }

    @PostMapping("/notifications/send")
    public String sendNotification(@RequestParam String title,
                                    @RequestParam String content,
                                    Model model) {
        // TODO: Implement - Tạo Notification mới
        // TODO: Gửi cho tất cả Member hoặc chọn lọc
        return "redirect:/librarian/interaction/notifications?sent";
    }

    // UC-15.2: Respond to Book Acquisition Requests
    @GetMapping("/book-requests")
    public String viewBookRequests(Model model) {
        // TODO: Implement - Lấy danh sách BookRequests từ Member
        return "librarian/book-requests";
    }

    @PostMapping("/book-requests/respond/{id}")
    public String respondToBookRequest(@PathVariable Integer id,
                                        @RequestParam String response,
                                        Model model) {
        // TODO: Implement - Cập nhật trạng thái BookRequest (Approved/Rejected)
        // TODO: Gửi thông báo phản hồi cho Member
        return "redirect:/librarian/interaction/book-requests?responded";
    }

    // UC-15.3: Moderate Reviews & Comments
    @GetMapping("/reviews")
    public String viewAllReviews(Model model) {
        // TODO: Implement - Lấy tất cả Feedback/Review chờ duyệt
        return "librarian/moderate-reviews";
    }

    @PostMapping("/reviews/moderate/{id}")
    public String moderateReview(@PathVariable Integer id,
                                  @RequestParam String action, Model model) {
        // TODO: Implement - Duyệt (approve) hoặc xóa (reject) review
        return "redirect:/librarian/interaction/reviews?moderated";
    }
}
