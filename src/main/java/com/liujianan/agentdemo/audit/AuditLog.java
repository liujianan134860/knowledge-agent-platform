package com.liujianan.agentdemo.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.liujianan.agentdemo.harness.JsonMapConverter;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String userId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 80)
    private String resourceType;

    @Column(length = 120)
    private String resourceId;

    @Column(length = 64)
    private String requestId;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> attributes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(Long id, String userId, String action, String resourceType, String resourceId,
                    String requestId, Map<String, Object> attributes, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.requestId = requestId;
        this.attributes = attributes;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public String getRequestId() { return requestId; }
    public Map<String, Object> getAttributes() { return attributes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
