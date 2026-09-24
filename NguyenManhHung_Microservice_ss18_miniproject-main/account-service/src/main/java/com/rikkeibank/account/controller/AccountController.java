package com.rikkeibank.account.controller;

import com.rikkeibank.account.dto.*;
import com.rikkeibank.account.service.BankAccountService;
import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final BankAccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> openAccount(
            @Valid @RequestBody OpenAccountRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        // ADMIN, TELLER or CUSTOMER can open an account
        AccountResponse response = accountService.openAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Bank account opened successfully", response));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @PathVariable String accountNumber) {
        AccountResponse response = accountService.getAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByCustomer(
            @PathVariable Long customerId) {
        List<AccountResponse> list = accountService.getAccountsByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PatchMapping("/{accountNumber}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> updateStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateStatusRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied: ADMIN role required to lock/unlock accounts");
        }
        AccountResponse response = accountService.updateStatus(accountNumber, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Account status updated", response));
    }

    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<ApiResponse<AccountResponse>> debit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DebitRequest request) {
        log.info("Processing debit request on account: {}", accountNumber);
        AccountResponse response = accountService.debit(accountNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Account debited successfully", response));
    }

    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<ApiResponse<AccountResponse>> credit(
            @PathVariable String accountNumber,
            @Valid @RequestBody CreditRequest request) {
        log.info("Processing credit request on account: {}", accountNumber);
        AccountResponse response = accountService.credit(accountNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Account credited successfully", response));
    }

    @PostMapping("/{accountNumber}/compensate-debit")
    public ResponseEntity<ApiResponse<AccountResponse>> compensateDebit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DebitRequest request) {
        log.info("Processing compensate-debit on account: {}", accountNumber);
        AccountResponse response = accountService.compensateDebit(accountNumber, request);
        return ResponseEntity.ok(ApiResponse.success("Compensating debit refund completed", response));
    }
}
