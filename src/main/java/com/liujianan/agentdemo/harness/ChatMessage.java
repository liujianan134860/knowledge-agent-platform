package com.liujianan.agentdemo.harness;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String sessionId;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(Long id, String sessionId, String userId, String role, String content, LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public SessionMessage toSessionMessage() {
        return new SessionMessage(role, content, createdAt);
    }
}
