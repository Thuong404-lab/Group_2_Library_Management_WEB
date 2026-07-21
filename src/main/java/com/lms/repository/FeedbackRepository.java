package com.lms.repository;

import com.lms.entity.Feedback;
import com.lms.enums.FeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    @EntityGraph(attributePaths = {"book", "member", "member.user"})
    Page<Feedback> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"book", "member", "member.user"})
    Page<Feedback> findByStatus(FeedbackStatus status, Pageable pageable);

    boolean existsByMember_MemberIdAndBook_BookIdAndStatusNot(
            Integer memberId, Integer bookId, FeedbackStatus status);

    @Query("""
            select count(feedback)
            from Feedback feedback
            where feedback.status = com.lms.enums.FeedbackStatus.APPROVED
              and (feedback.librarianResponse is null or trim(feedback.librarianResponse) = '')
            """)
    long countAwaitingLibrarianResponse();

    @EntityGraph(attributePaths = {"book", "book.authors"})
    List<Feedback> findByMember_MemberIdAndStatusNotOrderByCreatedDateDesc(
            Integer memberId, FeedbackStatus status);

    @EntityGraph(attributePaths = {"book"})
    Page<Feedback> findByMember_MemberIdAndStatusNotOrderByCreatedDateDesc(
            Integer memberId, FeedbackStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "member.user"})
    List<Feedback> findByBook_BookIdAndStatusOrderByCreatedDateDesc(
            Integer bookId, FeedbackStatus status);
}
