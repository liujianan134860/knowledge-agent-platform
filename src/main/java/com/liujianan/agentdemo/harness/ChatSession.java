package com.liujianan.agentdemo.harness;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_session")
public class ChatSession {
    @Id
    @Column(length = 36)
    private String id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_message", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "message_order")
    private List<SessionMessage> messages = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 20)
    private String userId;

    public ChatSession() {
    }

    public ChatSession(String id, List<SessionMessage> messages, LocalDateTime createdAt, LocalDateTime updatedAt, String userId) {
        this.id = id;
        this.messages = messages != null ? messages : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
    }

    public String getId() { return id; }
    public List<SessionMessage> getMessages() { return messages; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getUserId() { return userId; }

    public void setId(String id) { this.id = id; }
    public void setMessages(List<SessionMessage> messages) { this.messages = messages; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setUserId(String userId) { this.userId = userId; }

    // Bean-style accessors for record compatibility
    public String id() { return id; }
    public List<SessionMessage> messages() { return messages; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public String userId() { return userId; }
}
