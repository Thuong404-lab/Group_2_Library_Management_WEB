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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public Page<Book> searchBooks(String keyword, Integer genreId, String status, Pageable pageable) {
        return bookRepository.searchBooks(keyword, genreId, status, pageable);
    }

    // UC-3: Xem danh sách sách
    @Override
    public Page<Book> findAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    // Lấy sách mới nhất cho trang chủ
    @Override
    public List<Book> getRecentBooks(int limit) {
        Pageable pageable =
                PageRequest.of(0, limit,
                        Sort.by(Sort.Direction.DESC, "bookId"));
        return bookRepository.findAll(pageable).getContent();
    }

    // UC-3: Xem chi tiết sách
    @Override
    public Book findBookById(Integer id) {
        return bookRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + id));
    }
}
