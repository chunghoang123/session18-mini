package com.rikkeibank.identity.service;

import com.rikkeibank.common.security.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public void blacklistToken(String token, long expirationMs) {
        if (token == null || expirationMs <= 0) return;
        try {
            String key = SecurityConstants.REDIS_BLACKLIST_KEY_PREFIX + token;
            redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(expirationMs));
            log.info("Token successfully added to Redis blacklist: {}", key);
        } catch (Exception e) {
            log.warn("Failed to blacklist token in Redis: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token) {
        if (token == null) return false;
        try {
            String key = SecurityConstants.REDIS_BLACKLIST_KEY_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Failed to check Redis blacklist for token: {}", e.getMessage());
            return false;
        }
    }

    public void revokeUser(Long userId, long durationMs) {
        if (userId == null) return;
        try {
            String key = SecurityConstants.REDIS_USER_REVOKED_PREFIX + userId;
            redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(durationMs));
            log.info("User {} successfully revoked in Redis: {}", userId, key);
        } catch (Exception e) {
            log.warn("Failed to revoke user in Redis: {}", e.getMessage());
        }
    }

    public boolean isUserRevoked(Long userId) {
        if (userId == null) return false;
        try {
            String key = SecurityConstants.REDIS_USER_REVOKED_PREFIX + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Failed to check Redis revocation for user: {}", e.getMessage());
            return false;
        }
    }
}
