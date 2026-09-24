package com.rikkeibank.identity.controller;

import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.common.security.SecurityConstants;
import com.rikkeibank.identity.dto.*;
import com.rikkeibank.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for username: {}", request.getUsername());
        UserDto createdUser = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User registered successfully", createdUser));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for username: {}", request.getUsername());
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", tokenResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokenResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = SecurityConstants.HEADER_STRING, required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/admin/revoke-user/{userId}")
    public ResponseEntity<ApiResponse<Void>> revokeUser(
            @PathVariable Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        // Enforce ADMIN role check
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new AppException(ErrorCode.FORBIDDEN, "Only administrators can force logout users");
        }
        authService.revokeUserByAdmin(userId);
        return ResponseEntity.ok(ApiResponse.success("User access revoked and forced logout successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @RequestHeader(value = "X-User-Name", required = false) String username) {
        if (username == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User context is missing");
        }
        UserDto userDto = authService.getCurrentUser(username);
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }
}
