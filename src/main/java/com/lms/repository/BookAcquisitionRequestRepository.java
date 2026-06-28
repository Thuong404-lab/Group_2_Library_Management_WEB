package com.lms.repository;

import com.lms.entity.BookAcquisitionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookAcquisitionRequestRepository extends JpaRepository<BookAcquisitionRequest, Integer> {

    boolean existsByMember_MemberIdAndTitleIgnoreCase(Integer memberId, String title);
}