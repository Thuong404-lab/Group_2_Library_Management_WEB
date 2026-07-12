package com.lms.repository;
import com.lms.entity.BookItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookItemRepository extends JpaRepository<BookItem, Integer> {
    Optional<BookItem> findByBarcode(String barcode);

    long countByStatusIgnoreCase(String status);

    long countByShelf_ShelfId(Integer shelfId);

    long countByBook_BookId(Integer bookId);

    long countByBook_BookIdAndStatusIgnoreCase(Integer bookId, String status);

    List<BookItem> findByBook_BookId(Integer bookId);

    Optional<BookItem> findFirstByBook_BookIdAndStatus(Integer bookId, String status);
    
}
