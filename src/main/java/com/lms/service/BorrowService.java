package com.lms.service;

import com.lms.dto.request.BorrowRequest;
import com.lms.dto.response.MemberBorrowDTO;
import com.lms.entity.Borrow;
import com.lms.entity.BorrowDetail;
import java.util.List;

public interface BorrowService {
    Borrow processBorrowing(BorrowRequest request, String librarianUsername) throws Exception;
    Borrow memberSubmitBorrowRequest(String username, Integer bookId, Integer numberOfDays) throws Exception;
    void processReturnBook(String barcode) throws Exception;

    List<Borrow> getBorrowsByMemberAndStatus(String username, String status);

    List<Borrow> getAllPendingRequests();
    List<Borrow> getAllReturnRequests();
    List<Borrow> getAllActiveLoans();

    void approvePendingRequest(Integer borrowId, String staffUsername) throws Exception;
    void approveReturnRequest(Integer borrowId) throws Exception;
    void updateStatus(Integer loanId, String status) throws Exception;

    List<Borrow> getAllBorrowHistoryByMember(String username);

    Borrow getBorrowById(Integer borrowId) throws Exception;
    List<BorrowDetail> getBorrowDetailsByBorrowId(Integer borrowId);

    // BỔ SUNG ĐẦY ĐỦ CHO MEMBER VIEW MANAGEMENT:
    List<MemberBorrowDTO> getMemberCurrentBorrows(String username);
    List<MemberBorrowDTO> getMemberReservations(String username);
    List<MemberBorrowDTO> getMemberOneMonthHistory(String username);
}