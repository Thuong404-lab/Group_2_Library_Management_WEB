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
    // Luá»“ng mÆ°á»£n trá»±c tiáº¿p táº¡i quáº§y
    Borrow processBorrowing(BorrowRequest request, String librarianUsername);
    Borrow activatePendingBankBorrow(Integer borrowId);
    void cancelPendingBankBorrow(Integer borrowId, String paymentStatus);

    // Luá»“ng Ä‘Äƒng kÃ½ mÆ°á»£n trá»±c tuyáº¿n (Chá» duyá»‡t)
    Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays);
    void approvePendingRequest(Integer borrowId, String staffUsername);
    void rejectPendingRequest(Integer borrowId);

    void confirmPhysicalPickup(Integer borrowId, String staffUsername);

    // Luá»“ng YÃŠU Cáº¦U TRáº¢ SÃCH VÃ€ GIA Háº N
    void memberSubmitRenewRequest(String username, Integer borrowDetailId, Integer renewalDays);
    void processReturnBook(String barcode); // Tráº£ trá»±c tiáº¿p qua quÃ©t mÃ£ váº¡ch

    // Luá»“ng Äáº¶T TRÆ¯á»šC SÃCH - RESERVATION (Má»›i nÃ¢ng cáº¥p)
    Reservation memberSubmitReservationRequest(String username, Integer bookId);
    void approveReservationRequest(Integer reservationId, String staffUsername);
    void rejectReservationRequest(Integer reservationId, String staffUsername);
    void memberCancelReservation(String username, Integer reservationId);
    Reservation getReservationById(Integer reservationId);

    // FIX: ThÃªm phÆ°Æ¡ng thá»©c nÃ y Ä‘á»ƒ Ä‘á»“ng bá»™ vá»›i BorrowServiceImpl
    List<Reservation> getAllPendingReservations();

    // CÃ¡c phÆ°Æ¡ng thá»©c truy váº¥n danh sÃ¡ch dá»¯ liá»‡u
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

    // Dá»¯ liá»‡u Ä‘á»“ng bá»™ ra cÃ¡c Tab hiá»ƒn thá»‹ trÃªn Front-end
    List<MemberBorrowDTO> getMemberCurrentBorrows(String username);
    List<MemberBorrowDTO> getMemberReservations(String username);
    List<MemberBorrowDTO> getMemberOneMonthHistory(String username);

    // ThÃªm vÃ o interface BorrowService
    List<ReturnRequestDTO> getPendingReturnRequestDTOs();
    List<ReservationRequestDTO> getPendingReservationDTOs();

    // Dá»¯ liá»‡u cáº¥u hÃ¬nh há»‡ thá»‘ng
    int getMaxBorrowDays();

    void memberSubmitReturnRequest(String username, Integer borrowDetailId);
    void approveReturnRequest(Integer borrowId);

    // ÄÄƒng kÃ½ mÆ°á»£n nhiá»u cuá»‘n trá»±c tuyáº¿n
    Borrow memberSubmitMultiBookBorrowRequest(String username, List<Integer> bookIds, Integer numberOfDays);

    // TÃ­nh toÃ¡n xem trÆ°á»›c phÃ­ mÆ°á»£n
    java.math.BigDecimal calculateBorrowFeePreview(String username, List<Integer> bookIds, Integer numberOfDays);
}

