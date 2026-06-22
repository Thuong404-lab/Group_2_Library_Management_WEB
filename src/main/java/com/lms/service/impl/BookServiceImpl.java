package com.lms.service.impl;

import com.lms.service.BookService;

import com.lms.repository.BookRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.GenreRepository;
import com.lms.repository.AuthorRepository;
import org.springframework.stereotype.Service;

/**
 * BookService - Xử lý Logic Sách
 * Liên quan: Member 1 (Thương) + Member 2 (Khanh)
 */
@Service
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;

    public BookServiceImpl(BookRepository bookRepository, CategoryRepository categoryRepository, GenreRepository genreRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
    }


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
