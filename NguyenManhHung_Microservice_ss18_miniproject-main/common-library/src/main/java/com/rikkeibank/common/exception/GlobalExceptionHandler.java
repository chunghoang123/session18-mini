package com.rikkeibank.common.exception;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAppException(AppException ex) {
        log.warn("AppException occurred: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        ErrorCode ec = ex.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ec.getStatus())
                .errorCode(ec.getCode())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(ec.getHttpStatus())
                .body(ApiResponse.<ErrorResponse>builder()
                        .code(ec.getStatus())
                        .message(ex.getMessage())
                        .data(errorResponse)
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        String message = details.isEmpty() ? "Validation failed" : String.join(", ", details);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(400)
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(message)
                .details(details)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest()
                .body(ApiResponse.<ErrorResponse>builder()
                        .code(400)
                        .message(message)
                        .data(errorResponse)
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred: ", ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(500)
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : "Internal server error")
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.internalServerError()
                .body(ApiResponse.<ErrorResponse>builder()
                        .code(500)
                        .message("Internal server error")
                        .data(errorResponse)
                        .timestamp(Instant.now())
                        .build());
    }
}
