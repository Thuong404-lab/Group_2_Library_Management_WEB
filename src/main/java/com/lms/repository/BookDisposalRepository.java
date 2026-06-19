package com.lms.repository;
import com.lms.entity.BookDisposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookDisposalRepository extends JpaRepository<BookDisposal, Integer> {
}
