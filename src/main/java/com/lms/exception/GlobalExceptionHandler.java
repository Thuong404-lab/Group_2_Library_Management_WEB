package com.lms.exception;

import com.lms.dto.response.ApiErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
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
import java.util.Locale;
import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_VIEW = "error";
    private static final MessageSource FALLBACK_MESSAGES = createFallbackMessages();

    @Autowired(required = false)
    private MessageSource messageSource;

    @ExceptionHandler(ApplicationException.class)
    public Object handleApplicationException(ApplicationException ex, HttpServletRequest request) {
        logExpected(ex, request);
        return render(ex.getStatus(), applicationTitle(ex), ex.getMessage(), request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class,
            EntityNotFoundException.class, NoSuchElementException.class})
    public Object handleNotFound(Exception ex, HttpServletRequest request) {
        return render(HttpStatus.NOT_FOUND, message("error.notFound.title"),
                message("error.notFound.message"), request);
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
                .orElse(message("error.validation.submittedInvalid"));
        return render(HttpStatus.BAD_REQUEST, message("error.validation.title"), message, request);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            ServletRequestBindingException.class, HttpMessageNotReadableException.class,
            HandlerMethodValidationException.class, IllegalArgumentException.class})
    public Object handleBadRequest(Exception ex, HttpServletRequest request) {
        String message = message("error.validation.parametersInvalid");
        if (ex instanceof ConstraintViolationException constraintException) {
            message = constraintException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(message);
        } else if (ex instanceof IllegalArgumentException) {
            message = readableMessage(ex, message);
        }
        return render(HttpStatus.BAD_REQUEST, message("error.validation.title"), message, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return render(HttpStatus.CONFLICT, message("error.conflict.title"),
                readableMessage(ex, message("error.conflict.state")), request);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public Object handleDataConflict(Exception ex, HttpServletRequest request) {
        LOGGER.warn("Data conflict at {}", request.getRequestURI(), ex);
        return render(HttpStatus.CONFLICT, message("error.conflict.title"),
                message("error.conflict.dataChanged"), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public Object handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return render(HttpStatus.UNAUTHORIZED, message("error.unauthorized.title"),
                message("error.unauthorized.message"), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return render(HttpStatus.FORBIDDEN, message("error.forbidden.title"),
                message("error.forbidden.message"), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return render(HttpStatus.METHOD_NOT_ALLOWED, message("error.methodNotAllowed.title"),
                message("error.methodNotAllowed.message"), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Object handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return render(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message("error.unsupportedMedia.title"),
                message("error.unsupportedMedia.message"), request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public Object handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return render(HttpStatus.NOT_ACCEPTABLE, message("error.notAcceptable.title"),
                message("error.notAcceptable.message"), request);
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
        return render(HttpStatus.PAYLOAD_TOO_LARGE, message("error.upload.title"),
                message("error.upload.message"), request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(Exception ex, HttpServletRequest request) {
        LOGGER.error("Unexpected error while handling {} {}",
                request.getMethod(), request.getRequestURI(), ex);
        return render(HttpStatus.INTERNAL_SERVER_ERROR, message("error.system.title"),
                message("error.system.message"), request);
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

    private String applicationTitle(ApplicationException exception) {
        if (exception instanceof ResourceNotFoundException) return message("error.notFound.title");
        if (exception instanceof ValidationException) return message("error.validation.title");
        if (exception instanceof ConflictException) return message("error.conflict.title");
        if (exception instanceof ForbiddenException) return message("error.forbidden.title");
        if (exception instanceof UnauthorizedException) return message("error.unauthorized.title");
        if (exception instanceof ExternalServiceException) return message("error.external.title");
        if (exception instanceof FileStorageException) return message("error.fileStorage.title");
        if (exception instanceof DataProcessingException) return message("error.dataProcessing.title");
        String title = exception.getErrorTitle();
        return title != null && title.startsWith("error.") ? message(title) : title;
    }

    private String message(String key, Object... arguments) {
        MessageSource source = messageSource == null ? FALLBACK_MESSAGES : messageSource;
        Locale locale = messageSource == null ? Locale.forLanguageTag("vi") : LocaleContextHolder.getLocale();
        return source.getMessage(key, arguments, locale);
    }

    private static MessageSource createFallbackMessages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    private void logExpected(ApplicationException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            LOGGER.error("Application error at {}", request.getRequestURI(), ex);
        } else {
            LOGGER.warn("Request rejected at {}: {}", request.getRequestURI(), ex.getMessage());
        }
    }
}
