package com.lms.repository;

import com.lms.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer>, JpaSpecificationExecutor<Book> {
    long countByStatusIgnoreCase(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.bookId = :bookId")
    Optional<Book> findByIdForUpdate(@Param("bookId") Integer bookId);
    
    boolean existsByGenre_GenreId(Integer genreId);

    @Query("SELECT b.genre.genreId, COUNT(b) FROM Book b " +
           "WHERE b.genre IS NOT NULL GROUP BY b.genre.genreId")
    List<Object[]> countTitlesByGenre();

    @Query("select (count(b) > 0) from Book b " +
            "where upper(replace(replace(b.isbn, '-', ''), ' ', '')) = :isbn")
    boolean existsByNormalizedIsbn(@Param("isbn") String isbn);

    @Query("select (count(distinct b) > 0) from Book b join b.authors author " +
            "where lower(trim(b.title)) = lower(:title) " +
            "and lower(trim(author.authorName)) = lower(:author)")
    boolean existsByNormalizedTitleAndAuthor(@Param("title") String title,
                                             @Param("author") String author);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.authors a " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:genreId IS NULL OR b.genre.genreId = :genreId) " +
           "AND (:status IS NULL OR :status = '' OR b.status = :status)")
    Page<Book> searchBooks(@Param("keyword") String keyword,
                           @Param("genreId") Integer genreId,
                           @Param("status") String status,
                           Pageable pageable);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN b.authors a " +
           "WHERE (:keyword = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.authorName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR EXISTS (SELECT barcodeItem.bookItemId FROM BookItem barcodeItem " +
           "WHERE barcodeItem.book = b AND LOWER(barcodeItem.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "AND (:bookCondition = '' OR EXISTS (SELECT bi.bookItemId FROM BookItem bi " +
           "WHERE bi.book = b AND bi.bookCondition = :bookCondition)) " +
           "AND (:bookItemStatus = '' OR EXISTS (SELECT statusItem.bookItemId FROM BookItem statusItem " +
           "WHERE statusItem.book = b AND statusItem.status = :bookItemStatus))")
    Page<Book> searchBookItems(@Param("keyword") String keyword,
                               @Param("bookCondition") String bookCondition,
                               @Param("bookItemStatus") String bookItemStatus,
                               Pageable pageable);

    @Query("SELECT d.book FROM BorrowDetail d GROUP BY d.book ORDER BY COUNT(d) DESC")
    List<Book> findTrendingBooks(Pageable pageable);
}
