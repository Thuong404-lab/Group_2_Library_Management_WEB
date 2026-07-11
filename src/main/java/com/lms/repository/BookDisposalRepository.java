package com.lms.repository;
import com.lms.entity.BookDisposal;
import com.lms.entity.BookItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookDisposalRepository extends JpaRepository<BookDisposal, Integer> {
    List<BookDisposal> findByBookItem(BookItem bookItem);
}
