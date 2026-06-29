package com.lms.repository;

import com.lms.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    Page<Feedback> findAll(Pageable pageable);

    boolean existsByMember_MemberIdAndBook_BookId(Integer memberId, Integer bookId);

    @EntityGraph(attributePaths = {"book", "book.authors"})
    List<Feedback> findByMember_MemberIdOrderByCreatedDateDesc(Integer memberId);
}
