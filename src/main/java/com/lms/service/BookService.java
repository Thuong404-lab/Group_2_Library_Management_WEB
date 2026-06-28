package com.lms.service;

/**
 * BookService - Xử lý Logic Sách
 * Liên quan: Member 1 (Thương) + Member 2 (Khanh)
 */
public interface BookService {

    // UC-1: Tìm kiếm sách
    void searchBooks(String keyword, Integer categoryId, Integer genreId, int page);

    // UC-3: Xem danh sách sách
    void findAllBooks(int page);

    // Lấy sách mới nhất cho trang chủ
    java.util.List<com.lms.entity.Book> getRecentBooks(int limit);

    // UC-3: Xem chi tiết sách
    com.lms.entity.Book findBookById(Integer id);

}
