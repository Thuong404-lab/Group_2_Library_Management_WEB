package com.lms.repository;
import com.lms.entity.BookItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface BookItemRepository extends JpaRepository<BookItem, Integer> {
    Optional<BookItem> findByBarcode(String barcode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from BookItem item where item.barcode = :barcode")
    Optional<BookItem> findByBarcodeForUpdate(@Param("barcode") String barcode);

    long countByStatusIgnoreCase(String status);

    long countByShelf_ShelfId(Integer shelfId);

    @Query("select item.shelf.shelfId, count(item) from BookItem item where item.shelf is not null group by item.shelf.shelfId")
    List<Object[]> countBookItemsByShelf();

    long countByBook_BookId(Integer bookId);

    long countByBook_BookIdAndStatusIgnoreCase(Integer bookId, String status);

    List<BookItem> findByBook_BookId(Integer bookId);

    Optional<BookItem> findFirstByBook_BookIdAndStatusIgnoreCaseOrderByBookItemIdAsc(Integer bookId, String status);

    @Query("""
            select distinct item.book.bookId
            from BookItem item, Favorites favorite
            where favorite.member.memberId = :memberId
              and favorite.book.bookId = item.book.bookId
              and lower(item.status) = 'available'
            """)
    Set<Integer> findAvailableFavoriteBookIds(@Param("memberId") Integer memberId);
}
