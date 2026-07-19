package com.lms.repository;

import com.lms.entity.BookAcquisitionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.lms.enums.AcquisitionRequestStatus;
import java.util.Collection;

@Repository
public interface BookAcquisitionRequestRepository extends JpaRepository<BookAcquisitionRequest, Integer> {

    boolean existsByMember_MemberIdAndTitleIgnoreCase(Integer memberId, String title);

    boolean existsByMember_MemberIdAndTitleIgnoreCaseAndStatusIn(
            Integer memberId, String title, Collection<AcquisitionRequestStatus> statuses);

    long countByStatus(AcquisitionRequestStatus status);

    Page<BookAcquisitionRequest> findByMember_MemberIdOrderByCreatedDateDesc(Integer memberId, Pageable pageable);
}
