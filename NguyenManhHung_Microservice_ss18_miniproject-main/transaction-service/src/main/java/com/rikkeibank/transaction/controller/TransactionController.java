package com.rikkeibank.transaction.controller;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.dto.PageResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.transaction.dto.TransactionResponse;
import com.rikkeibank.transaction.dto.TransferRequest;
import com.rikkeibank.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        log.info("Received transfer request from user: {} ({})", username, userIdHeader);
        Long userId = userIdHeader != null ? Long.parseLong(userIdHeader) : null;
        TransactionResponse response = transactionService.transfer(request, userId, username, roles);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Transfer executed successfully", response));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable String transactionId) {
        TransactionResponse response = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAccountTransactions(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<TransactionResponse> response = transactionService.getTransactionsByAccount(accountNumber, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/teller/daily")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTellerDailyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        // Enforce TELLER role
        if (roles == null || !roles.contains("ROLE_TELLER")) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied: TELLER role required to view teller daily transactions");
        }
        if (userIdHeader == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User identity is required");
        }

        Long tellerId = Long.parseLong(userIdHeader);
        PageResponse<TransactionResponse> response = transactionService.getDailyTransactionsForTeller(tellerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin/daily")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getAdminDailyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied: ADMIN role required");
        }

        PageResponse<TransactionResponse> response = transactionService.getDailyTransactionsAdmin(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
