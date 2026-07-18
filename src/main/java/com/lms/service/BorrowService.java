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
    Borrow activatePendingBankBorrow(Integer borrowId);
    void cancelPendingBankBorrow(Integer borrowId, String paymentStatus);

    // Luồng đăng ký mượn trực tuyến (Chờ duyệt)
    Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays);
    void approvePendingRequest(Integer borrowId, String staffUsername);
    void rejectPendingRequest(Integer borrowId, String reason);

    void confirmPhysicalPickup(Integer borrowId, String staffUsername);

    // Luồng YÊU CẦU TRẢ SÁCH VÀ GIA HẠN
    void memberSubmitRenewRequest(Integer borrowDetailId);
    void processReturnBook(String barcode); // Trả trực tiếp qua quét mã vạch

    // Luồng ĐẶT TRƯỚC SÁCH - RESERVATION (Mới nâng cấp)
    Reservation memberSubmitReservationRequest(String username, Integer bookId);
    void approveReservationRequest(Integer reservationId, String staffUsername);
    void rejectReservationRequest(Integer reservationId, String staffUsername, String reason);
    void memberCancelReservation(String username, Integer reservationId);
    Reservation getReservationById(Integer reservationId);

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
    List<BorrowDetail> getPendingRenewalRequests();
    BorrowDetail getBorrowDetailById(Integer borrowDetailId);

    // Dữ liệu đồng bộ ra các Tab hiển thị trên Front-end
    List<MemberBorrowDTO> getMemberCurrentBorrows(String username);
    List<MemberBorrowDTO> getMemberReservations(String username);
    List<MemberBorrowDTO> getMemberOneMonthHistory(String username);

    // Thêm vào interface BorrowService
    List<ReturnRequestDTO> getPendingReturnRequestDTOs();
    List<ReservationRequestDTO> getPendingReservationDTOs();

    // Dữ liệu cấu hình hệ thống
    int getMaxBorrowDays();

    void memberSubmitReturnRequest(String username, Integer borrowDetailId);
    void approveReturnRequest(Integer borrowId);

    // Đăng ký mượn nhiều cuốn trực tuyến
    Borrow memberSubmitMultiBookBorrowRequest(String username, List<Integer> bookIds, Integer numberOfDays);

    // Tính toán xem trước phí mượn
    java.math.BigDecimal calculateBorrowFeePreview(String username, List<Integer> bookIds, Integer numberOfDays);
}
