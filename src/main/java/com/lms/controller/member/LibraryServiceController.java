package com.lms.controller.member;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

/**
 * LibraryServiceController - Dịch vụ Thư viện (Yêu thích, Đánh giá, Đề xuất)
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
@Controller
@RequestMapping("/member/services")
public class LibraryServiceController {

    // UC-7.1: Add to Favorites
    @PostMapping("/favorite/{bookId}")
    public String addToFavorites(@PathVariable Integer bookId,
                                  Principal principal, Model model) {
        // TODO: Implement - Thêm sách vào danh sách yêu thích
        // TODO: Kiểm tra trùng lặp (đã thêm chưa)
        return "redirect:/books/" + bookId + "?favorited";
    }

    // UC-7.1: Remove from Favorites
    @PostMapping("/favorite/remove/{bookId}")
    public String removeFromFavorites(@PathVariable Integer bookId,
                                       Principal principal) {
        // TODO: Implement - Xóa sách khỏi danh sách yêu thích
        return "redirect:/member/favorites?removed";
    }

    // UC-7.2: Suggest New Books
    @GetMapping("/suggestions")
    public String showSuggestionForm(Model model) {
        // TODO: Implement - Hiển thị form đề xuất mua sách mới
        return "member/suggestions";
    }

    @PostMapping("/suggestions")
    public String submitSuggestion(Principal principal, Model model) {
        // TODO: Implement - Lưu đề xuất sách mới vào BookRequests
        return "redirect:/member/services/suggestions?submitted";
    }

    // UC-7.3: Rate & Review Books
    @PostMapping("/review/{bookId}")
    public String submitReview(@PathVariable Integer bookId,
                                @RequestParam Integer rating,
                                @RequestParam String comment,
                                Principal principal, Model model) {
        // TODO: Implement - Lưu Feedback (rating + comment) cho sách
        // TODO: Kiểm tra Member đã review sách này chưa
        // TODO: Cập nhật averageRating của Book
        return "redirect:/books/" + bookId + "?reviewed";
    }

    // UC-7.4: View Notifications
    @GetMapping("/notifications")
    public String viewNotifications(Principal principal, Model model) {
        // TODO: Implement - Lấy danh sách Notification của Member
        // TODO: Đánh dấu đã đọc (isRead = true)
        return "member/notifications";
    }
}
