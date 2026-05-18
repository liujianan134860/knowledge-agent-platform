package com.liujianan.agentdemo.auth;

import com.liujianan.agentdemo.audit.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;
    private final long refreshTokenExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       AuditService auditService, RefreshTokenRepository refreshTokenRepository,
                       RevokedAccessTokenRepository revokedAccessTokenRepository,
                       @Value("${ai-platform.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("username already exists");
        }
        String userId = "u-" + UUID.randomUUID();
        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(userId, username, passwordHash, LocalDateTime.now());
        userRepository.save(user);
        auditService.record(user.getId(), "USER_REGISTER", "USER", user.getId(), java.util.Map.of("username", username));
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();
        User user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("user is disabled");
        }
        auditService.record(user.getId(), "USER_LOGIN", "USER", user.getId(), java.util.Map.of("username", username));
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = hash(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findById(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("invalid refresh token"));
        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("invalid refresh token");
        }
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("invalid refresh token"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("user is disabled");
        }
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        auditService.record(user.getId(), "TOKEN_REFRESH", "USER", user.getId(), java.util.Map.of("username", user.getUsername()));
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String accessToken, LogoutRequest request, String userId) {
        if (accessToken != null && !accessToken.isBlank()) {
            JwtUtil.TokenClaims claims = jwtUtil.parseToken(accessToken);
            if (claims.tokenId() != null && !claims.tokenId().isBlank()) {
                revokedAccessTokenRepository.save(new RevokedAccessToken(claims.tokenId(), claims.userId(), LocalDateTime.now()));
            }
        }
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshTokenRepository.findById(hash(request.refreshToken()))
                    .ifPresent(token -> {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    });
        }
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> {
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
        auditService.record(userId, "USER_LOGOUT", "USER", userId, java.util.Map.of());
    }

    public boolean isAccessTokenRevoked(String tokenId) {
        return tokenId != null && revokedAccessTokenRepository.existsById(tokenId);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = issueRefreshToken(user);
        return new AuthResponse(token, refreshToken, user.getId(), user.getUsername(), user.getRole().name());
    }

    private String issueRefreshToken(User user) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.save(new RefreshToken(hash(token), user.getId(), now,
                now.plusNanos(refreshTokenExpirationMs * 1_000_000)));
        return token;
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash token", e);
        }
    }
}
