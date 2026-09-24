package com.rikkeibank.customer.controller;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.dto.PageResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.customer.dto.StaffRequest;
import com.rikkeibank.customer.dto.StaffResponse;
import com.rikkeibank.customer.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(
            @Valid @RequestBody StaffRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdmin(roles);
        StaffResponse response = staffService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Staff created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaffById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdminOrTeller(roles);
        StaffResponse response = staffService.getStaffById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/branch/{branch}")
    public ResponseEntity<ApiResponse<List<StaffResponse>>> getStaffByBranch(
            @PathVariable String branch,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdminOrTeller(roles);
        List<StaffResponse> list = staffService.getStaffByBranch(branch);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StaffResponse>>> getAllStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdmin(roles);
        PageResponse<StaffResponse> response = staffService.getAllStaff(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffRequest request,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdmin(roles);
        StaffResponse response = staffService.updateStaff(id, request);
        return ResponseEntity.ok(ApiResponse.success("Staff updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        enforceAdmin(roles);
        staffService.deleteStaff(id);
        return ResponseEntity.ok(ApiResponse.success("Staff deleted successfully", null));
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
