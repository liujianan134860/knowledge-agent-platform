package com.liujianan.agentdemo.tool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tool_invocation")
public class ToolInvocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36)
    private String runId;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 100)
    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String toolInput;

    @Column(columnDefinition = "TEXT")
    private String toolOutput;

    @Column(nullable = false, length = 10)
    private String riskLevel = "LOW";

    @Column(nullable = false, length = 20)
    private String status = "SUCCESS";

    @Column
    private Long durationMs;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ToolInvocation() {
    }

    public ToolInvocation(Long id, String runId, String userId, String toolName,
                          String toolInput, String toolOutput, String riskLevel,
                          String status, Long durationMs, String errorMessage,
                          LocalDateTime createdAt) {
        this.id = id;
        this.runId = runId;
        this.userId = userId;
        this.toolName = toolName;
        this.toolInput = toolInput;
        this.toolOutput = toolOutput;
        this.riskLevel = riskLevel;
        this.status = status;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getRunId() { return runId; }
    public String getUserId() { return userId; }
    public String getToolName() { return toolName; }
    public String getToolInput() { return toolInput; }
    public String getToolOutput() { return toolOutput; }
    public String getRiskLevel() { return riskLevel; }
    public String getStatus() { return status; }
    public Long getDurationMs() { return durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setRunId(String runId) { this.runId = runId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public void setToolInput(String toolInput) { this.toolInput = toolInput; }
    public void setToolOutput(String toolOutput) { this.toolOutput = toolOutput; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setStatus(String status) { this.status = status; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long id() { return id; }
    public String runId() { return runId; }
    public String userId() { return userId; }
    public String toolName() { return toolName; }
    public String toolInput() { return toolInput; }
    public String toolOutput() { return toolOutput; }
    public String riskLevel() { return riskLevel; }
    public String status() { return status; }
    public Long durationMs() { return durationMs; }
    public String errorMessage() { return errorMessage; }
    public LocalDateTime createdAt() { return createdAt; }
}
