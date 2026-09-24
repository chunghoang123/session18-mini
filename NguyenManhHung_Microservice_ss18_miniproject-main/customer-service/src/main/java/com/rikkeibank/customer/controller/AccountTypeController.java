package com.rikkeibank.customer.controller;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.customer.dto.AccountTypeRequest;
import com.rikkeibank.customer.dto.AccountTypeResponse;
import com.rikkeibank.customer.service.AccountTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/account-types")
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountTypeResponse>> createAccountType(
            @Valid @RequestBody AccountTypeRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdmin(roles);
        AccountTypeResponse response = accountTypeService.createAccountType(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Account type created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountTypeResponse>>> getAllAccountTypes() {
        List<AccountTypeResponse> list = accountTypeService.getAllAccountTypes();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{typeCode}")
    public ResponseEntity<ApiResponse<AccountTypeResponse>> getAccountTypeByCode(@PathVariable String typeCode) {
        AccountTypeResponse response = accountTypeService.getAccountTypeByCode(typeCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountTypeResponse>> updateAccountType(
            @PathVariable Long id,
            @Valid @RequestBody AccountTypeRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdmin(roles);
        AccountTypeResponse response = accountTypeService.updateAccountType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Account type updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccountType(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdmin(roles);
        accountTypeService.deleteAccountType(id);
        return ResponseEntity.ok(ApiResponse.success("Account type deleted successfully", null));
    }

    private void enforceAdmin(String roles) {
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied: ADMIN role required");
        }
    }
}
