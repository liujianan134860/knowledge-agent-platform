package com.liujianan.agentdemo.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(@Value("${security.jwt.secret:}") String configuredSecret,
                   @Value("${security.jwt.expiration-ms:86400000}") long expirationMs) {
        String envSecret = System.getenv("JWT_SECRET");
        String secret = configuredSecret == null || configuredSecret.isBlank() ? envSecret : configuredSecret;
        if (secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32) {
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } else {
            this.secretKey = Keys.hmacShaKeyFor("knowledge-agent-platform-dev-secret-32b".getBytes(StandardCharsets.UTF_8));
        }
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String username) {
        return generateToken(userId, username, UserRole.USER);
    }

    public String generateToken(String userId, String username, UserRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .claim("username", username)
                .claim("role", role == null ? UserRole.USER.name() : role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String extractUserId(String token) throws JwtException {
        return parseToken(token).userId();
    }

    public TokenClaims parseToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String role = claims.get("role", String.class);
        return new TokenClaims(claims.getSubject(), claims.get("username", String.class),
                claims.getId(),
                role == null ? UserRole.USER : UserRole.valueOf(role));
    }

    public record TokenClaims(String userId, String username, String tokenId, UserRole role) {
    }
}
