package com.rikkeibank.common.exception;

public class InsufficientBalanceException extends AppException {
    public InsufficientBalanceException(String message) {
        super(ErrorCode.INSUFFICIENT_BALANCE, message);
    }
}
