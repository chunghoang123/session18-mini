package com.rikkeibank.identity.service;

import com.rikkeibank.common.enums.RoleType;
import com.rikkeibank.common.exception.AppException;
import com.rikkeibank.identity.dto.LoginRequest;
import com.rikkeibank.identity.dto.TokenResponse;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTokenBlacklistService blacklistService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("customer1")
                .password("encoded_pass")
                .email("customer1@example.com")
                .fullName("Test Customer")
                .role(RoleType.ROLE_CUSTOMER)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Login Success: Valid username and password returns access & refresh tokens")
    void testLogin_Success() {
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

        LoginRequest request = LoginRequest.builder()
                .username("customer1")
                .password("password123")
                .build();

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("customer1", response.getUsername());
        assertEquals("ROLE_CUSTOMER", response.getRole());
    }

    @Test
    @DisplayName("Login Failed: Incorrect password throws AppException (UNAUTHORIZED)")
    void testLogin_WrongPassword_ThrowsException() {
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpass", "encoded_pass")).thenReturn(false);

        LoginRequest request = LoginRequest.builder()
                .username("customer1")
                .password("wrongpass")
                .build();

        assertThrows(AppException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Admin Revoke User: Adds revocation key into Redis blacklist")
    void testRevokeUserByAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        authService.revokeUserByAdmin(1L);

        verify(blacklistService, times(1)).revokeUser(eq(1L), anyLong());
    }
}
