package com.rikkeibank.identity.service;

import com.rikkeibank.common.enums.RoleType;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.common.security.JwtUtils;
import com.rikkeibank.common.security.SecurityConstants;
import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.RefreshTokenRequest;
import com.rikkeibank.identity.dto.RegisterRequest;
import com.rikkeibank.identity.dto.TokenResponse;
import com.rikkeibank.identity.dto.UserDto;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTokenBlacklistService blacklistService;

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_EXISTS, "Username " + request.getUsername() + " is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTS, "Email " + request.getEmail() + " is already registered");
        }

        RoleType role = request.getRole() != null ? request.getRole() : RoleType.ROLE_CUSTOMER;

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user: id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());

        return mapToDto(saved);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.FORBIDDEN, "User account is disabled");
        }

        String accessToken = JwtUtils.generateAccessToken(user.getId(), user.getUsername(), List.of(user.getRole().name()));
        String refreshToken = JwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(SecurityConstants.ACCESS_TOKEN_EXPIRATION_MS / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!JwtUtils.validateToken(refreshToken)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        if (blacklistService.isTokenBlacklisted(refreshToken)) {
            throw new AppException(ErrorCode.TOKEN_REVOKED, "Refresh token has been revoked");
        }

        String username = JwtUtils.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User associated with refresh token not found"));

        if (blacklistService.isUserRevoked(user.getId())) {
            throw new AppException(ErrorCode.TOKEN_REVOKED, "User session has been revoked by admin");
        }

        String newAccessToken = JwtUtils.generateAccessToken(user.getId(), user.getUsername(), List.of(user.getRole().name()));

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(SecurityConstants.ACCESS_TOKEN_EXPIRATION_MS / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return;
        }
        String token = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
        try {
            Date expiration = JwtUtils.getExpirationDateFromToken(token);
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            if (remainingMs > 0) {
                blacklistService.blacklistToken(token, remainingMs);
            }
        } catch (Exception e) {
            log.warn("Error calculating token remaining time: {}", e.getMessage());
        }
    }

    public void revokeUserByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User with ID " + userId + " not found"));

        // Revoke user session for the duration of a refresh token lifetime (7 days)
        blacklistService.revokeUser(user.getId(), SecurityConstants.REFRESH_TOKEN_EXPIRATION_MS);
        log.warn("Admin revoked session for user: id={}, username={}", user.getId(), user.getUsername());
    }

    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));
        return mapToDto(user);
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
