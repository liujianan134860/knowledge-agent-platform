package com.liujianan.agentdemo.tool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tool_approval")
public class ToolApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String runId;

    @Column(nullable = false)
    private Integer stepIndex;

    @Column(nullable = false, length = 100)
    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String toolInput;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ToolApproval() {}

    public ToolApproval(Long id, String runId, Integer stepIndex, String toolName,
                        String toolInput, String status, String userId, LocalDateTime createdAt) {
        this.id = id;
        this.runId = runId;
        this.stepIndex = stepIndex;
        this.toolName = toolName;
        this.toolInput = toolInput;
        this.status = status;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getRunId() { return runId; }
    public Integer getStepIndex() { return stepIndex; }
    public String getToolName() { return toolName; }
    public String getToolInput() { return toolInput; }
    public String getStatus() { return status; }
    public String getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setRunId(String runId) { this.runId = runId; }
    public void setStepIndex(Integer stepIndex) { this.stepIndex = stepIndex; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public void setToolInput(String toolInput) { this.toolInput = toolInput; }
    public void setStatus(String status) { this.status = status; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
