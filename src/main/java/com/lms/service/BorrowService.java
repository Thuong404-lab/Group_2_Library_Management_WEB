package com.lms.service;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.Borrow;

public interface BorrowService {
    Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception;
}