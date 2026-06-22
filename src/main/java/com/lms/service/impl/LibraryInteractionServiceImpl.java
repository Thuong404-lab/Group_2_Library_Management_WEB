package com.lms.service.impl;

import com.lms.service.LibraryInteractionService;

import org.springframework.stereotype.Service;

/**
 * LibraryInteractionService - Xử lý Logic Dịch vụ & Tương tác
 * Người phụ trách: Trần Nguyễn Quốc Anh (CE191655)
 */
@Service
public class LibraryInteractionServiceImpl implements LibraryInteractionService {

    // UC-7.1: Thêm yêu thích
    @Override
    public void addToFavorites(Integer memberId, Integer bookId) {
        // TODO: Implement
    }

    // UC-7.2: Đề xuất sách mới
    @Override
    public void suggestNewBook(Integer memberId, String title, String author, String reason) {
        // TODO: Implement - Lưu vào BookRequests
    }

    // UC-7.3: Đánh giá sách
    @Override
    public void submitReview(Integer memberId, Integer bookId, Integer rating, String comment) {
        // TODO: Implement - Lưu Feedback
    }

    // UC-7.4: Lấy thông báo
    @Override
    public void getNotifications(Integer memberId) {
        // TODO: Implement
    }

    // UC-15.1: Gửi thông báo cho Member
    @Override
    public void sendNotification(Integer staffId, String title, String content) {
        // TODO: Implement
    }

    // UC-15.2: Phản hồi yêu cầu mua sách
    @Override
    public void respondToBookRequest(Integer requestId, String response) {
        // TODO: Implement
    }

    // UC-15.3: Duyệt review
    @Override
    public void moderateReview(Integer feedbackId, String action) {
        // TODO: Implement
    }
}
