package com.lms.service;

import com.lms.dto.request.BorrowRequest;
import com.lms.dto.response.MemberBorrowDTO;
import com.lms.dto.response.ReservationRequestDTO;
import com.lms.dto.response.ReturnRequestDTO;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import com.lms.entity.Reservation;
import java.util.List;

public interface BorrowService {
    // Luồng mượn trực tiếp tại quầy
    Borrow processBorrowing(BorrowRequest request, String librarianUsername);

    // Luồng đăng ký mượn trực tuyến (Chờ duyệt)
    Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays);
    void approvePendingRequest(Integer borrowId, String staffUsername);

    // Luồng YÊU CẦU TRẢ SÁCH (Mới nâng cấp)
    void memberSubmitReturnRequest(String username, Integer borrowDetailId);
    void approveReturnRequest(Integer borrowId);
    void processReturnBook(String barcode); // Trả trực tiếp qua quét mã vạch

    // Luồng ĐẶT TRƯỚC SÁCH - RESERVATION (Mới nâng cấp)
    Reservation memberSubmitReservationRequest(String username, Integer bookId);
    void approveReservationRequest(Integer reservationId, String staffUsername);
    void memberCancelReservation(String username, Integer reservationId);

    // FIX: Thêm phương thức này để đồng bộ với BorrowServiceImpl
    List<Reservation> getAllPendingReservations();

    // Các phương thức truy vấn danh sách dữ liệu
    List<Borrow> getBorrowsByMemberAndStatus(String username, String status);
    List<Borrow> getAllPendingRequests();
    List<Borrow> getAllReturnRequests();
    List<Borrow> getAllActiveLoans();
    void updateStatus(Integer loanId, String status);
    List<Borrow> getAllBorrowHistoryByMember(String username);
    Borrow getBorrowById(Integer borrowId);
    List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId);

    // Dữ liệu đồng bộ ra các Tab hiển thị trên Front-end
    List<MemberBorrowDTO> getMemberCurrentBorrows(String username);
    List<MemberBorrowDTO> getMemberReservations(String username);
    List<MemberBorrowDTO> getMemberOneMonthHistory(String username);

    // Thêm vào interface BorrowService
    List<ReturnRequestDTO> getPendingReturnRequestDTOs();
    List<ReservationRequestDTO> getPendingReservationDTOs();
}