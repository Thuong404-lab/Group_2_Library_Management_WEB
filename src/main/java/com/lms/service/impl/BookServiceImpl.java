package com.lms.service.impl;

import com.lms.entity.Book;
import com.lms.repository.AuthorRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.GenreRepository;
import com.lms.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * BookService - Xử lý Logic Sách
 * Liên quan: Member 1 (Thương) + Member 2 (Khanh)
 */
@Service
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;


    public BookServiceImpl(BookRepository bookRepository, CategoryRepository categoryRepository, GenreRepository genreRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;

    }


    // UC-1: Tìm kiếm sách
    @Override
    public Page<Book> searchBooks(String keyword, Integer categoryId, Integer genreId, int page) {
        Pageable pageable = PageRequest.of(page, 8);  // phân trang

        return bookRepository.searchBooksAdvanced(keyword, categoryId, genreId, pageable);
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
