package com.punestartup.common.web;

import com.punestartup.common.exception.ApiException;
import com.punestartup.common.exception.InvalidParameterException;
import com.punestartup.common.exception.RateLimitException;
import com.punestartup.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(InvalidParameterException ex,
                                                                 HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex, HttpServletRequest request) {
        RequestContext.setErrorCode(request, ex.getErrorCode());
        ErrorResponse body = ErrorResponse.of(ex.getStatus().value(), ex.getErrorCode(), ex.getMessage(),
                request.getRequestURI(), RequestContext.resolve(request));
        return ResponseEntity.status(ex.getStatus())
                .header("Retry-After", Long.toString(ex.getRetryAfterSeconds()))
                .body(body);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return respond(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        log.warn("validation_failed method={} path={} fields={}", request.getMethod(), request.getRequestURI(),
                fieldErrors.keySet());
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "The request body is missing or malformed", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                             HttpServletRequest request) {
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "The uploaded file is too large", request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex,
                                                         HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "The multipart request is missing or malformed", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Missing required parameter '%s'".formatted(ex.getParameterName()), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        String message = "Parameter '%s' has an invalid value".formatted(ex.getName());
        return respond(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", message, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                            HttpServletRequest request) {
        log.warn("data_integrity_violation method={} path={} cause={}", request.getMethod(),
                request.getRequestURI(), ex.getMostSpecificCause().getClass().getSimpleName());
        return respond(HttpStatus.CONFLICT, "CONSTRAINT_VIOLATION",
                "The request conflicts with existing data", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String errorCode, String message,
                                                 HttpServletRequest request) {
        return respond(status, errorCode, message, request, null);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String errorCode, String message,
                                                 HttpServletRequest request, java.util.Map<String, String> fieldErrors) {
        RequestContext.setErrorCode(request, errorCode);
        String requestId = RequestContext.resolve(request);
        ErrorResponse body = fieldErrors == null
                ? ErrorResponse.of(status.value(), errorCode, message, request.getRequestURI(), requestId)
                : ErrorResponse.withFieldErrors(status.value(), errorCode, message, request.getRequestURI(),
                        requestId, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
