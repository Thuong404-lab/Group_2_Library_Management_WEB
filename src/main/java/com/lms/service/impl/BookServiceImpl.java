package com.lms.service.impl;

import com.lms.service.BookService;

import org.springframework.stereotype.Service;

/**
 * BookService - Xử lý Logic Sách
 * Liên quan: Member 1 (Thương) + Member 2 (Khanh)
 */
@Service
public class BookServiceImpl implements BookService {

    // UC-1: Tìm kiếm sách
    @Override
    public void searchBooks(String keyword, Integer categoryId, Integer genreId, int page) {
        // TODO: Implement - Tìm kiếm theo keyword, category, genre
        // TODO: Hỗ trợ phân trang (Pageable)
    }

    // UC-3: Xem danh sách sách
    @Override
    public void findAllBooks(int page) {
        // TODO: Implement - Lấy tất cả sách (phân trang)
    }

    // UC-3: Xem chi tiết sách
    @Override
    public void findBookById(Integer id) {
        // TODO: Implement - Lấy sách theo ID kèm Author, Genre, Feedback
    }
}
