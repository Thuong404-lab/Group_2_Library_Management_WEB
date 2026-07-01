package com.lms.repository;

import com.lms.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    long countByStatusIgnoreCase(String status);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.authors a " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:genreId IS NULL OR b.genre.genreId = :genreId) " +
           "AND (:status IS NULL OR :status = '' OR b.status = :status)")
    Page<Book> searchBooks(@Param("keyword") String keyword,
                           @Param("genreId") Integer genreId,
                           @Param("status") String status,
                           Pageable pageable);
}
