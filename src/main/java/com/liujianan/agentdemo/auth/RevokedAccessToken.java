package com.liujianan.agentdemo.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "revoked_access_token")
public class RevokedAccessToken {
    @Id
    private String tokenId;
    private String userId;
    private LocalDateTime revokedAt;

    protected RevokedAccessToken() {
    }

    public RevokedAccessToken(String tokenId, String userId, LocalDateTime revokedAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.revokedAt = revokedAt;
    }

    public String getTokenId() { return tokenId; }
    public String getUserId() { return userId; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
}
