package com.rikkeibank.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class JwtUtils {

    private static SecretKey getSigningKey() {
        byte[] keyBytes = SecurityConstants.SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static String generateAccessToken(Long userId, String username, List<String> roles) {
        return generateToken(userId, username, roles, SecurityConstants.ACCESS_TOKEN_EXPIRATION_MS);
    }

    public static String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, List.of(), SecurityConstants.REFRESH_TOKEN_EXPIRATION_MS);
    }

    public static String generateToken(Long userId, String username, List<String> roles, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (List<String>) claims.get("roles");
    }

    public static Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public static <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public static Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}
