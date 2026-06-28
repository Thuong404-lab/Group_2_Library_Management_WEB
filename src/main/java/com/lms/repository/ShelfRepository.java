package com.lms.repository;
import com.lms.entity.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelfRepository extends JpaRepository<Shelf, Integer> {
    boolean existsByShelfNameIgnoreCase(String shelfName);
}

