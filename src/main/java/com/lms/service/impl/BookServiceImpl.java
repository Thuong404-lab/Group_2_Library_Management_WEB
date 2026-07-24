package com.lms.service.impl;

import com.lms.entity.Book;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.AuthorRepository;
import com.lms.repository.BookRepository;
import com.lms.repository.CategoryRepository;
import com.lms.repository.GenreRepository;
import com.lms.service.BookService;
import com.lms.service.LocalizedMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import com.lms.entity.Author;
import java.util.List;

/**
 * BookService - Xử lý Logic Sách
 * Liên quan: Member 1 (Thương) + Member 2 (Khanh)
 */
@Service
public class BookServiceImpl implements BookService {
    @Autowired
    private LocalizedMessageService messages = LocalizedMessageService.fallback();
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final GenreRepository genreRepository;
    private final AuthorRepository authorRepository;

    public BookServiceImpl(BookRepository bookRepository, CategoryRepository categoryRepository,
            GenreRepository genreRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.genreRepository = genreRepository;
        this.authorRepository = authorRepository;
    }

    // UC-1: Tìm kiếm sách
    @Override
    public Page<Book> searchBooks(String keyword, Integer genreId, String status, Pageable pageable) {
        Specification<Book> spec = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }
            Predicate predicate = cb.conjunction();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String[] words = keyword.trim().split("\\s+");

                // Ensure we only join once, and reuse it for all words
                Join<Book, Author> authorJoin = root.join("authors", JoinType.LEFT);

                for (String word : words) {
                    String likePattern = "%" + word.toLowerCase() + "%";

                    Predicate titlePredicate = cb.like(cb.lower(root.get("title")), likePattern);
                    Predicate authorPredicate = cb.like(cb.lower(authorJoin.get("authorName")), likePattern);
                    Predicate isbnPredicate = cb.like(cb.lower(root.get("isbn")), likePattern);

                    Predicate wordPredicate = cb.or(titlePredicate, authorPredicate, isbnPredicate);
                    predicate = cb.and(predicate, wordPredicate);
                }
            }

            if (genreId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("genre").get("genreId"), genreId));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            return predicate;
        };

        return bookRepository.findAll(spec, pageable);
    }

    // UC-3: Xem danh sách sách
    @Override
    public Page<Book> findAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    // Lấy sách mới nhất cho trang chủ
    @Override
    public List<Book> getRecentBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Direction.DESC, "bookId"));
        return bookRepository.findAll(pageable).getContent();
    }

    @Override
    public List<Book> getTrendingBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Book> trending = bookRepository.findTrendingBooks(pageable);
        if (trending.isEmpty()) {
            // Fallback to recent books if no books have been borrowed yet
            return getRecentBooks(limit);
        }
        return trending;
    }

    // UC-3: Xem chi tiết sách
    @Override
    public Book findBookById(Integer id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("backend.inventory.bookNotFound", id)));
    }
}
