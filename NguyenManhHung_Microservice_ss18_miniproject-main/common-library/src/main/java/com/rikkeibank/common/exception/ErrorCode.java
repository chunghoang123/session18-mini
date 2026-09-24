package com.rikkeibank.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SUCCESS(200, "SUCCESS", "Request successfully processed"),
    CREATED(201, "CREATED", "Resource successfully created"),
    BAD_REQUEST(400, "BAD_REQUEST", "Invalid request parameter or payload"),
    VALIDATION_FAILED(400, "VALIDATION_FAILED", "Validation constraint violated"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "Authentication required or token expired/invalid"),
    TOKEN_REVOKED(401, "TOKEN_REVOKED", "Token has been revoked or forced logout by administrator"),
    FORBIDDEN(403, "FORBIDDEN", "You do not have permission to access this resource"),
    NOT_FOUND(404, "NOT_FOUND", "Resource not found"),
    ACCOUNT_NOT_FOUND(404, "ACCOUNT_NOT_FOUND", "Bank account not found"),
    CUSTOMER_NOT_FOUND(404, "CUSTOMER_NOT_FOUND", "Customer profile not found"),
    STAFF_NOT_FOUND(404, "STAFF_NOT_FOUND", "Staff profile not found"),
    CONFLICT(409, "CONFLICT", "Resource already exists or state conflict"),
    USERNAME_EXISTS(409, "USERNAME_EXISTS", "Username already exists"),
    EMAIL_EXISTS(409, "EMAIL_EXISTS", "Email address already registered"),
    INSUFFICIENT_BALANCE(400, "INSUFFICIENT_BALANCE", "Account balance is insufficient for this transaction"),
    ACCOUNT_LOCKED(400, "ACCOUNT_LOCKED", "Account is locked or inactive"),
    SAME_ACCOUNT_TRANSFER(400, "SAME_ACCOUNT_TRANSFER", "Cannot transfer to the same account"),
    CIRCUIT_BREAKER_OPEN(503, "CIRCUIT_BREAKER_OPEN", "Service is temporarily unavailable due to high failure rate"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "Internal system error occurred");

    private final int status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(int status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(this.status);
    }
}
