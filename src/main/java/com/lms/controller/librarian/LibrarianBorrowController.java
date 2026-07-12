package com.lms.controller.librarian;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.Borrow;
import com.lms.repository.BorrowRepository;
import com.lms.service.BorrowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.Arrays;

@Controller
public class LibrarianBorrowController {

    private final BorrowService borrowService;
    private final BorrowRepository borrowRepository;

    public LibrarianBorrowController(BorrowService borrowService, BorrowRepository borrowRepository) {
        this.borrowService = borrowService;
        this.borrowRepository = borrowRepository;
    }

    // 1. Xem danh sách toàn cục các phiếu mượn trả
    @GetMapping("/librarian/borrow/list")
    public String listAllBorrows(@RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("borrowDate").descending());
        Page<Borrow> borrowPage;

        String activeStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String activeKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        if (activeStatus != null) {
            if (activeKeyword != null) {
                borrowPage = borrowRepository.findByStatusAndKeyword(activeStatus, activeKeyword, pageable);
            } else {
                borrowPage = borrowRepository.findByStatus(activeStatus, pageable);
            }
        } else if (activeKeyword != null) {
            borrowPage = borrowRepository.findByKeyword(activeKeyword, pageable);
        } else {
            borrowPage = borrowRepository.findAll(pageable);
        }

        model.addAttribute("borrows", borrowPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", borrowPage.getTotalPages());
        model.addAttribute("totalItems", borrowPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "librarian/borrow-list";
    }

    // =========================================================================
    // CHỨC NĂNG TRẢ SÁCH TẠI QUẦY (showReturnDesk & confirmBookReturn)
    // ĐÃ ĐƯỢC XÓA BỎ TẠI ĐÂY ĐỂ CHUYỂN TOÀN BỘ SANG LOANCONTROLLER (Fix lỗi sập Spring)
    // =========================================================================

    // 2. Quầy mượn sách tích hợp đồng bộ danh sách online (Master-Detail) đúng mô hình chia đôi màn hình
    @GetMapping("/librarian/borrow/create")
    public String showCreateBorrowForm(@RequestParam(value = "requestId", required = false) Integer requestId, Model model) {
        model.addAttribute("borrowRequest", new BorrowRequest());
        model.addAttribute("pendingRequests", borrowService.getAllPendingRequests());
        model.addAttribute("pendingReturnRequests", borrowService.getPendingReturnRequestDTOs());
        model.addAttribute("activeReservations", borrowService.getPendingReservationDTOs());

        if (requestId != null) {
            try {
                Borrow selectedBorrow = borrowService.getBorrowById(requestId);
                model.addAttribute("selectedRequest", selectedBorrow);
                model.addAttribute("requestDetails", borrowService.getBorrowDetailsByBorrowId(requestId));
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không thể lấy thông tin chi tiết: " + e.getMessage());
            }
        }

        return "librarian/create-borrow";
    }

    // 3. Phê duyệt đơn mượn trực tuyến: Chuyển đổi trạng thái từ Pending -> Active
    @PostMapping("/librarian/borrow/approve/{borrowId}")
    public String approveMemberRequest(@PathVariable("borrowId") Integer borrowId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.approvePendingRequest(borrowId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt và cấp sách vật lý thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phê duyệt thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // 4. Từ chối duyệt đơn mượn trực tuyến
    @PostMapping("/librarian/borrow/reject/{borrowId}")
    public String rejectMemberRequest(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            borrowService.updateStatus(borrowId, "Rejected");
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối cấp sách cho yêu cầu trực tuyến này.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Từ chối thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // 5. Nhận sách trả từ độc giả gửi yêu cầu online trả sách
    @PostMapping("/librarian/borrow/approve-return/{borrowId}")
    public String approveReturnRequest(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            borrowService.approveReturnRequest(borrowId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận nhận lại sách vật lý nhập kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xác nhận trả sách thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // 6. Xử lý tạo phiếu mượn trực tiếp bằng mã vạch (Barcode) quét tại quầy
    @PostMapping("/librarian/borrow/create")
    public String processCreateBorrow(@ModelAttribute("borrowRequest") BorrowRequest request,
                                      @RequestParam("rawBarcodes") String rawBarcodes,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            if (rawBarcodes != null && !rawBarcodes.trim().isEmpty()) {
                request.setBarcodes(Arrays.asList(rawBarcodes.split("\\s*,\\s*")));
            }
            String librarianUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.processBorrowing(request, librarianUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo phiếu mượn trực tiếp tại quầy thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // 7. Phê duyệt đặt giữ chỗ trước từ Độc giả
    @PostMapping("/librarian/reservations/approve/{reservationId}")
    public String approveReservationRequest(@PathVariable("reservationId") Integer reservationId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";
            borrowService.approveReservationRequest(reservationId, staffUsername);
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt đơn đặt trước sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Duyệt thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }
}