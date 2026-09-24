package com.rikkeibank.common.security;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String SECRET_KEY = "RikkeiBankSecretKeyForJwtTokenGenerationAndValidationShouldBeVeryLongAndSecure2024!";
    public static final long ACCESS_TOKEN_EXPIRATION_MS = 15 * 60 * 1000; // 15 mins
    public static final long REFRESH_TOKEN_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000; // 7 days (seamless customer login)
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String REDIS_BLACKLIST_KEY_PREFIX = "token:blacklist:";
    public static final String REDIS_USER_REVOKED_PREFIX = "user:revoked:";
}
