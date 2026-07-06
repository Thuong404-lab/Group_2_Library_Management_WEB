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
    Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception;

    // Luồng đăng ký mượn trực tuyến (Chờ duyệt)
    Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) throws Exception;
    void approvePendingRequest(Integer borrowId, String staffUsername) throws Exception;

    // Luồng YÊU CẦU TRẢ SÁCH (Mới nâng cấp)
    void memberSubmitReturnRequest(String username, Integer borrowDetailId) throws Exception;
    void approveReturnRequest(Integer borrowId) throws Exception;
    void processReturnBook(String barcode) throws Exception; // Trả trực tiếp qua quét mã vạch

    // Luồng ĐẶT TRƯỚC SÁCH - RESERVATION (Mới nâng cấp)
    Reservation memberSubmitReservationRequest(String username, Integer bookId) throws Exception;
    void approveReservationRequest(Integer reservationId, String staffUsername) throws Exception;
    void memberCancelReservation(String username, Integer reservationId) throws Exception;

    // FIX: Thêm phương thức này để đồng bộ với BorrowServiceImpl
    List<Reservation> getAllPendingReservations();

    // Các phương thức truy vấn danh sách dữ liệu
    List<Borrow> getBorrowsByMemberAndStatus(String username, String status);
    List<Borrow> getAllPendingRequests();
    List<Borrow> getAllReturnRequests();
    List<Borrow> getAllActiveLoans();
    void updateStatus(Integer loanId, String status) throws Exception;
    List<Borrow> getAllBorrowHistoryByMember(String username);
    Borrow getBorrowById(Integer borrowId) throws Exception;
    List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId);

    // Dữ liệu đồng bộ ra các Tab hiển thị trên Front-end
    List<MemberBorrowDTO> getMemberCurrentBorrows(String username);
    List<MemberBorrowDTO> getMemberReservations(String username);
    List<MemberBorrowDTO> getMemberOneMonthHistory(String username);

    // Thêm vào interface BorrowService
    List<ReturnRequestDTO> getPendingReturnRequestDTOs();
    List<ReservationRequestDTO> getPendingReservationDTOs();
}