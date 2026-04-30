package com.liujianan.agentdemo.harness;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class SessionMessage {
    private String role;
    private String content;
    private LocalDateTime createdAt;

    public SessionMessage() {
    }

    public SessionMessage(String role, String content, LocalDateTime createdAt) {
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String role() { return role; }
    public String content() { return content; }
    public LocalDateTime createdAt() { return createdAt; }
}
