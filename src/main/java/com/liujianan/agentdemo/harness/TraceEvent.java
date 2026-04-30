package com.liujianan.agentdemo.harness;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "trace_event")
public class TraceEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String sessionId;

    @Column(nullable = false, length = 30)
    private String stage;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> attributes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 20)
    private String userId;

    public TraceEvent() {
    }

    public TraceEvent(Long id, String sessionId, String stage, String message, Map<String, Object> attributes,
                      LocalDateTime createdAt, String userId) {
        this.id = id;
        this.sessionId = sessionId;
        this.stage = stage;
        this.message = message;
        this.attributes = attributes;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getStage() { return stage; }
    public String getMessage() { return message; }
    public Map<String, Object> getAttributes() { return attributes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getUserId() { return userId; }

    public void setId(Long id) { this.id = id; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setStage(String stage) { this.stage = stage; }
    public void setMessage(String message) { this.message = message; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUserId(String userId) { this.userId = userId; }

    // Method-style accessors
    public Long id() { return id; }
    public String sessionId() { return sessionId; }
    public String stage() { return stage; }
    public String message() { return message; }
    public Map<String, Object> attributes() { return attributes; }
    public LocalDateTime createdAt() { return createdAt; }
    public String userId() { return userId; }
}
