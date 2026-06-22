package com.lms.service.impl;

import com.lms.service.LoanService;

import com.lms.repository.BorrowRepository;
import com.lms.repository.ReservationRepository;
import org.springframework.stereotype.Service;

/**
 * LoanService - Xử lý Logic Quản lý Phiếu mượn (Thủ thư)
 * Người phụ trách: Huỳnh Gia Hưng (CE190488)
 */
@Service
public class LoanServiceImpl implements LoanService {
    private final BorrowRepository borrowRepository;
    private final ReservationRepository reservationRepository;

    public LoanServiceImpl(BorrowRepository borrowRepository, ReservationRepository reservationRepository) {
        this.borrowRepository = borrowRepository;
        this.reservationRepository = reservationRepository;
    }


    // UC-13.1: Xem chi tiết phiếu mượn
    @Override
    public void getLoanDetails(Integer borrowId) {
        // TODO: Implement
    }

    // UC-13.2: Xác nhận trả sách
    @Override
    public void confirmReturn(String barcode, Integer memberId) {
        // TODO: Implement
    }

    // UC-13.3: Duyệt yêu cầu mượn
    @Override
    public void processBorrowRequest(Integer borrowId) {
        // TODO: Implement
    }

    // UC-13.4: Gia hạn mượn
    @Override
    public void processRenewal(Integer borrowDetailId) {
        // TODO: Implement - Kiểm tra số lần gia hạn (max 2)
        // TODO: Gia hạn thêm 7 ngày
    }
}
