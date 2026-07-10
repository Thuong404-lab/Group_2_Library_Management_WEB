package com.lms.service;

import com.lms.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * BookService - Xử lý Logic Sách
 * Liên quan: Member 1 (Thương) + Member 2 (Khanh)
 */
public interface BookService {

    // UC-1: Tìm kiếm sách
    Page<Book> searchBooks(String keyword, Integer genreId, String status, Pageable pageable);

    // UC-3: Xem danh sách sách
    Page<Book> findAllBooks(Pageable pageable);

    // Lấy sách mới nhất cho trang chủ
    List<Book> getRecentBooks(int limit);

    // UC-3: Xem chi tiết sách
    Book findBookById(Integer id);

}
