package com.lms.service;

import com.lms.dto.request.MemberBookRequest;
import com.lms.entity.BookRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberBookRequestService {

    void submitRequest(String username, MemberBookRequest request);
    void updatePendingRequest(String username, Integer requestId, MemberBookRequest request);
    void cancelPendingRequest(String username, Integer requestId);
    Page<BookRequest> getMyRequests(String username, Pageable pageable);
}
