package com.rikkeibank.customer.controller;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.dto.PageResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.customer.dto.CustomerRequest;
import com.rikkeibank.customer.dto.CustomerResponse;
import com.rikkeibank.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdminOrTeller(roles);
        CustomerResponse created = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Customer created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        CustomerResponse response = customerService.getCustomerById(id);
        // If caller is CUSTOMER, they can only view their own profile
        if (roles != null && roles.contains("ROLE_CUSTOMER") && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER")) {
            if (response.getUserId() != null && currentUserId != null && !response.getUserId().toString().equals(currentUserId)) {
                throw new AppException(ErrorCode.FORBIDDEN, "You are only allowed to view your own customer profile");
            }
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{customerCode}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerByCode(@PathVariable String customerCode) {
        CustomerResponse response = customerService.getCustomerByCode(customerCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdminOrTeller(roles);
        PageResponse<CustomerResponse> response = customerService.getAllCustomers(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdminOrTeller(roles);
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdmin(roles);
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    private void enforceAdmin(String roles) {
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied: ADMIN role required");
        }
    }

    private void enforceAdminOrTeller(String roles) {
        if (roles == null || (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_TELLER"))) {
            throw new AppException(ErrorCode.FORBIDDEN, "Access denied: ADMIN or TELLER role required");
        }
    }
}
