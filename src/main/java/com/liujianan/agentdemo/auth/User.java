package com.liujianan.agentdemo.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class User {
    @Id
    @Column(length = 64)
    private String id;

    @Column(unique = true, nullable = false, length = 30)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    public User() {
    }

    public User(String id, String username, String passwordHash, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }

    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setRole(UserRole role) { this.role = role; }
    public void setStatus(UserStatus status) { this.status = status; }
}
