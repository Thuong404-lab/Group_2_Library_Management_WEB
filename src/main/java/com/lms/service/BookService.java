package com.lms.service;

import com.lms.entity.Book;
import org.springframework.data.domain.Page;

/**
 * BookService - Xử lý Logic Sách
 * Liên quan: Member 1 (Thương) + Member 2 (Khanh)
 */
public interface BookService {

    // UC-1: Tìm kiếm sách
    Page<Book> searchBooks(String keyword, Integer categoryId, Integer genreId, int page);

    // UC-3: Xem danh sách sách
    void findAllBooks(int page);

    // UC-3: Xem chi tiết sách
    void findBookById(Integer id);

}
