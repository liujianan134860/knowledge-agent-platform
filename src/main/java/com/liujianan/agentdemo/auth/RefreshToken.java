package com.liujianan.agentdemo.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id
    private String tokenHash;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    protected RefreshToken() {
    }

    public RefreshToken(String tokenHash, String userId, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getTokenHash() { return tokenHash; }
    public String getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
