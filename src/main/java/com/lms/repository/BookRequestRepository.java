package com.lms.repository;

import com.lms.entity.BookRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.lms.enums.BookRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface BookRequestRepository extends JpaRepository<BookRequest, Integer> {

    boolean existsByMember_MemberIdAndDedupKeyAndStatusIn(
            Integer memberId, String dedupKey, Collection<BookRequestStatus> statuses);

    boolean existsByMember_MemberIdAndDedupKeyAndStatusInAndRequestIdNot(
            Integer memberId, String dedupKey, Collection<BookRequestStatus> statuses, Integer requestId);

    long countByStatus(BookRequestStatus status);

    @EntityGraph(attributePaths = {"member", "member.user", "processedBy", "processedBy.user"})
    Page<BookRequest> findByMember_MemberIdOrderByCreatedDateDesc(Integer memberId, Pageable pageable);

    Optional<BookRequest> findByRequestIdAndMember_MemberId(Integer requestId, Integer memberId);

    @EntityGraph(attributePaths = {"member", "member.user", "processedBy", "processedBy.user"})
    @Query("""
            select request from BookRequest request
            where (:status is null or request.status = :status)
              and (:keyword is null or :keyword = ''
                   or lower(request.title) like lower(concat('%', :keyword, '%'))
                   or lower(request.author) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(request.isbn, '')) like lower(concat('%', :keyword, '%'))
                   or lower(request.member.user.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(request.requestReason) like lower(concat('%', :keyword, '%')))
            """)
    Page<BookRequest> searchForModeration(
            @Param("status") BookRequestStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
