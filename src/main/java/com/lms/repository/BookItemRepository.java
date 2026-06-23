package com.lms.repository;
import com.lms.entity.BookItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BookItemRepository extends JpaRepository<BookItem, Integer> {
    Optional<BookItem> findByBarcode(String barcode);

    long countByStatusIgnoreCase(String status);
}
