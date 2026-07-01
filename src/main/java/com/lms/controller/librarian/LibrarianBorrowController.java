package com.lms.controller.librarian;

import com.lms.dto.request.BorrowRequest;
import com.lms.entity.Borrow;
import com.lms.service.BorrowService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.Arrays;

@Controller
@RequestMapping("/librarian/borrow")
public class LibrarianBorrowController {

    private final BorrowService borrowService;

    public LibrarianBorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    // Xem danh sách toàn cục các phiếu mượn trả
    @GetMapping("/list")
    public String listAllBorrows(Model model) {
        model.addAttribute("pendingRequests", borrowService.getAllPendingRequests());
        model.addAttribute("returnRequests", borrowService.getAllReturnRequests());
        model.addAttribute("activeLoans", borrowService.getAllActiveLoans());
        return "librarian/borrow-list";
    }

    // Quầy mượn sách tích hợp đồng bộ danh sách online (Master-Detail) đúng mô hình chia đôi màn hình
    @GetMapping("/create")
    public String showCreateBorrowForm(@RequestParam(value = "requestId", required = false) Integer requestId, Model model) {
        model.addAttribute("borrowRequest", new BorrowRequest());

        // Bước 1: Lấy danh sách thành viên gửi đơn mượn trực tuyến đang chờ phê duyệt (Hiện ở cột trái)
        model.addAttribute("pendingRequests", borrowService.getAllPendingRequests());

        // Bước 2 & 3: Khi click chọn 1 member, nạp thông tin chi tiết và danh sách sách muốn mượn qua cột phải
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

    // CHỨC NĂNG PHÊ DUYỆT ĐƠN MƯỢN ONLINE: Chuyển đổi trạng thái từ Pending -> Active
    @PostMapping("/approve/{borrowId}")
    public String approveMemberRequest(@PathVariable("borrowId") Integer borrowId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            String staffUsername = (principal != null) ? principal.getName() : "admin";

            // ĐÃ FIX: Đảm bảo truyền đủ cả 2 tham số sang cho Service xử lý
            borrowService.approvePendingRequest(borrowId, staffUsername);

            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt và cấp sách vật lý thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phê duyệt thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // CHỨC NĂNG TỪ CHỐI DUYỆT ĐƠN MƯỢN ONLINE
    @PostMapping("/reject/{borrowId}")
    public String rejectMemberRequest(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            borrowService.updateStatus(borrowId, "Rejected");
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối cấp sách cho yêu cầu trực tuyến này.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Từ chối thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/create";
    }

    // CHỨC NĂNG DUYỆT ĐƠN TRẢ ONLINE: Chuyển đổi trạng thái từ Return_Pending -> Returned
    @PostMapping("/approve-return/{borrowId}")
    public String approveReturnRequest(@PathVariable("borrowId") Integer borrowId, RedirectAttributes redirectAttributes) {
        try {
            borrowService.approveReturnRequest(borrowId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận nhận lại sách vật lý nhập kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xác nhận trả sách thất bại: " + e.getMessage());
        }
        return "redirect:/librarian/borrow/list";
    }

    // Xử lý tạo phiếu mượn trực tiếp bằng mã vạch (Barcode) quét tại quầy
    @PostMapping("/create")
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
            return "redirect:/librarian/borrow/create";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thất bại: " + e.getMessage());
            return "redirect:/librarian/borrow/create";
        }
    }
}