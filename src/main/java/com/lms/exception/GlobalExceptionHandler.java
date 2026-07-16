package com.lms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleMissingStaticResource(NoResourceFoundException ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Không tìm thấy dữ liệu");
        model.addAttribute("errorMessage", "Đường dẫn bạn yêu cầu không tồn tại hoặc đã bị xóa.");
        return "error";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public String handleValidationException(ValidationException ex, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("error", "Dữ liệu không hợp lệ");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorizedException(UnauthorizedException ex, Model model) {
        model.addAttribute("status", 401);
        model.addAttribute("error", "Chưa đăng nhập");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Vui lòng đăng nhập để thực hiện chức năng này.");
        return "error";
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public String handleForbiddenException(ForbiddenException ex, Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("error", "Không có quyền truy cập");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Bạn không có quyền truy cập vào tài nguyên này.");
        return "error";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Không tìm thấy dữ liệu");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictException.class)
    public String handleConflictException(ConflictException ex, Model model) {
        model.addAttribute("status", 409);
        model.addAttribute("error", "Dữ liệu đã tồn tại hoặc xung đột");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        ex.printStackTrace();
        model.addAttribute("status", 500);
        model.addAttribute("error", "Lỗi hệ thống (Backend)");
        model.addAttribute("errorMessage", "Đã xảy ra lỗi không xác định từ phía máy chủ. Vui lòng thử lại sau hoặc liên hệ quản trị viên.");
        return "error";
    }
}
