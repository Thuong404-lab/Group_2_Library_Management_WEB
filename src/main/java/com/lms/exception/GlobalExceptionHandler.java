package com.lms.exception;

import com.lms.dto.response.ApiErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_VIEW = "error";

    @ExceptionHandler(ApplicationException.class)
    public Object handleApplicationException(ApplicationException ex, HttpServletRequest request) {
        logExpected(ex, request);
        return render(ex.getStatus(), ex.getErrorTitle(), ex.getMessage(), request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class,
            EntityNotFoundException.class, NoSuchElementException.class})
    public Object handleNotFound(Exception ex, HttpServletRequest request) {
        return render(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu",
                "Đường dẫn hoặc dữ liệu bạn yêu cầu không tồn tại.", request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleBindingException(Exception ex, HttpServletRequest request) {
        BindingResult bindingResult = ex instanceof MethodArgumentNotValidException methodException
                ? methodException.getBindingResult()
                : ((BindException) ex).getBindingResult();
        String message = bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("Dữ liệu gửi lên không hợp lệ.");
        return render(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", message, request);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class, HttpMessageNotReadableException.class,
            HandlerMethodValidationException.class, IllegalArgumentException.class})
    public Object handleBadRequest(Exception ex, HttpServletRequest request) {
        String message = "Tham số yêu cầu không hợp lệ.";
        if (ex instanceof ConstraintViolationException constraintException) {
            message = constraintException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(message);
        } else if (ex instanceof IllegalArgumentException) {
            message = readableMessage(ex, message);
        }
        return render(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", message, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return render(HttpStatus.CONFLICT, "Dữ liệu xung đột",
                readableMessage(ex, "Thao tác không thể thực hiện ở trạng thái hiện tại."), request);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public Object handleDataConflict(Exception ex, HttpServletRequest request) {
        LOGGER.warn("Data conflict at {}", request.getRequestURI(), ex);
        return render(HttpStatus.CONFLICT, "Dữ liệu xung đột",
                "Dữ liệu đã thay đổi hoặc đang được sử dụng. Vui lòng tải lại và thử lại.", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public Object handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return render(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập",
                "Vui lòng đăng nhập để thực hiện chức năng này.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return render(HttpStatus.FORBIDDEN, "Không có quyền truy cập",
                "Bạn không có quyền thực hiện chức năng này.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return render(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ",
                "Phương thức HTTP này không được hỗ trợ cho đường dẫn hiện tại.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return render(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng không được hỗ trợ",
                "Định dạng dữ liệu gửi lên không được hệ thống hỗ trợ.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public Object handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return render(HttpStatus.NOT_ACCEPTABLE, "Định dạng phản hồi không được hỗ trợ",
                "Hệ thống không thể trả dữ liệu theo định dạng được yêu cầu.", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus resolvedStatus = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        String message = ex.getReason() == null || ex.getReason().isBlank()
                ? resolvedStatus.getReasonPhrase()
                : ex.getReason();
        return render(resolvedStatus, resolvedStatus.getReasonPhrase(),
                message, request);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public Object handleMultipart(Exception ex, HttpServletRequest request) {
        return render(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp tải lên không hợp lệ",
                "Tệp tải lên quá lớn hoặc không đúng định dạng.", request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, HttpServletRequest request) {
        LOGGER.error("Unexpected error while handling {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return render(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống",
                "Đã xảy ra lỗi phía máy chủ. Vui lòng thử lại sau hoặc liên hệ quản trị viên.", request);
    }

    private Object render(HttpStatus status, String title, String message, HttpServletRequest request) {
        if (expectsJson(request)) {
            return ResponseEntity.status(status).body(new ApiErrorResponse(
                    Instant.now(), status.value(), title, message, request.getRequestURI()));
        }
        ModelAndView modelAndView = new ModelAndView(ERROR_VIEW);
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("error", title);
        modelAndView.addObject("errorMessage", message);
        return modelAndView;
    }

    private boolean expectsJson(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod method
                && (AnnotatedElementUtils.hasAnnotation(method.getMethod(), ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), RestController.class))) {
            return true;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return request.getRequestURI().contains("/api/")
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"));
    }

    private String readableMessage(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
    }

    private void logExpected(ApplicationException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            LOGGER.error("Application error at {}", request.getRequestURI(), ex);
        } else {
            LOGGER.warn("Request rejected at {}: {}", request.getRequestURI(), ex.getMessage());
        }
    }
}
