package com.lms.service;

import com.lms.dto.request.MemberBookAcquisitionRequest;
import com.lms.entity.BookAcquisitionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberBookAcquisitionService {

    void submitRequest(String username, MemberBookAcquisitionRequest request);
    void updatePendingRequest(String username, Integer requestId, MemberBookAcquisitionRequest request);
    void cancelPendingRequest(String username, Integer requestId);
    Page<BookAcquisitionRequest> getMyRequests(String username, Pageable pageable);
}
