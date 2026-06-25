package com.lms.repository;
import com.lms.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    @Query("SELECT b FROM Book b WHERE " +
            "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR b.isbn = :keyword) AND " +
            "(:genreId IS NULL OR b.genre.genreId = :genreId) AND " +
            "(:categoryId IS NULL OR b.genre.category.categoryId = :categoryId)")
    Page<Book> searchBooksAdvanced(@Param("keyword") String keyword,
                                   @Param("categoryId") Integer categoryId,
                                   @Param("genreId") Integer genreId,
                                   Pageable pageable);
}
