package com.rikkeibank.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.common.dto.ApiResponse;
import com.rikkeibank.common.dto.ErrorResponse;
import com.rikkeibank.common.exception.ErrorCode;
import com.rikkeibank.common.security.JwtUtils;
import com.rikkeibank.common.security.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator",
            "/h2-console"
    );

    public JwtAuthenticationFilter(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Allow public paths
            boolean isOpenApi = OPEN_API_ENDPOINTS.stream().anyMatch(path::startsWith);
            if (isOpenApi) {
                return chain.filter(exchange);
            }

            // Check Authorization Header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, ErrorCode.UNAUTHORIZED, "Missing Authorization Header");
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
                return onError(exchange, ErrorCode.UNAUTHORIZED, "Invalid Authorization Header Format");
            }

            String token = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();

            if (!JwtUtils.validateToken(token)) {
                return onError(exchange, ErrorCode.UNAUTHORIZED, "Invalid or Expired JWT Token");
            }

            Long userId = JwtUtils.getUserIdFromToken(token);
            String username = JwtUtils.getUsernameFromToken(token);
            List<String> roles = JwtUtils.getRolesFromToken(token);
            String rolesString = roles != null ? String.join(",", roles) : "";

            // Check Redis Blacklist for revoked token or force-logged-out user
            String blacklistTokenKey = SecurityConstants.REDIS_BLACKLIST_KEY_PREFIX + token;
            String revokedUserKey = SecurityConstants.REDIS_USER_REVOKED_PREFIX + userId;

            return redisTemplate.hasKey(blacklistTokenKey)
                    .flatMap(isTokenBlacklisted -> {
                        if (Boolean.TRUE.equals(isTokenBlacklisted)) {
                            return onError(exchange, ErrorCode.TOKEN_REVOKED, "Token has been revoked");
                        }
                        return redisTemplate.hasKey(revokedUserKey)
                                .flatMap(isUserRevoked -> {
                                    if (Boolean.TRUE.equals(isUserRevoked)) {
                                        return onError(exchange, ErrorCode.TOKEN_REVOKED, "User session has been revoked by Administrator");
                                    }

                                    // Mutate request with user context headers
                                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                            .header("X-User-Id", String.valueOf(userId))
                                            .header("X-User-Name", username)
                                            .header("X-User-Roles", rolesString)
                                            .build();

                                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                                });
                    })
                    .onErrorResume(e -> {
                        log.warn("Redis check bypassed or failed: {}", e.getMessage());
                        // If Redis is temporarily unreachable, still forward valid signed JWT
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", String.valueOf(userId))
                                .header("X-User-Name", username)
                                .header("X-User-Roles", rolesString)
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, ErrorCode errorCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(errorCode.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorDetails = ErrorResponse.builder()
                .status(errorCode.getStatus())
                .errorCode(errorCode.getCode())
                .message(message)
                .path(exchange.getRequest().getURI().getPath())
                .timestamp(Instant.now())
                .build();

        ApiResponse<ErrorResponse> apiResponse = ApiResponse.<ErrorResponse>builder()
                .code(errorCode.getStatus())
                .message(message)
                .data(errorDetails)
                .timestamp(Instant.now())
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(apiResponse);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":401,\"message\":\"" + message + "\"}").getBytes();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
    }
}
