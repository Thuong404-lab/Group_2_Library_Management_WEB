package com.lms.service;

/**
 * LibraryInteractionService - Xử lý Logic Dịch vụ & Tương tác
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
public interface LibraryInteractionService {

    // UC-7.1: Thêm yêu thích
    void addToFavorites(Integer memberId, Integer bookId);

    // UC-7.2: Đề xuất sách mới
    void suggestNewBook(Integer memberId, String title, String author, String reason);

    // UC-7.3: Đánh giá sách
    void submitReview(Integer memberId, Integer bookId, Integer rating, String comment);

    // UC-7.4: Lấy thông báo
    void getNotifications(Integer memberId);

    // UC-15.1: Gửi thông báo cho Member
    void sendNotification(Integer staffId, String title, String content);

    // UC-15.2: Phản hồi yêu cầu mua sách
    void respondToBookRequest(Integer requestId, String response);

    // UC-15.3: Duyệt review
    void moderateReview(Integer feedbackId, String action);

}
