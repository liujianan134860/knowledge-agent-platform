package com.liujianan.agentdemo.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_step")
public class AgentStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String runId;

    @Column(nullable = false)
    private Integer stepIndex;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AgentStepType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String toolInput;

    @Column(columnDefinition = "TEXT")
    private String toolResult;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AgentStep() {
    }

    public AgentStep(Long id, String runId, Integer stepIndex, AgentStepType type, String content,
                     String toolName, String toolInput, String toolResult, LocalDateTime createdAt) {
        this.id = id;
        this.runId = runId;
        this.stepIndex = stepIndex;
        this.type = type;
        this.content = content;
        this.toolName = toolName;
        this.toolInput = toolInput;
        this.toolResult = toolResult;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getRunId() { return runId; }
    public Integer getStepIndex() { return stepIndex; }
    public AgentStepType getType() { return type; }
    public String getContent() { return content; }
    public String getToolName() { return toolName; }
    public String getToolInput() { return toolInput; }
    public String getToolResult() { return toolResult; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setRunId(String runId) { this.runId = runId; }
    public void setStepIndex(Integer stepIndex) { this.stepIndex = stepIndex; }
    public void setType(AgentStepType type) { this.type = type; }
    public void setContent(String content) { this.content = content; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public void setToolInput(String toolInput) { this.toolInput = toolInput; }
    public void setToolResult(String toolResult) { this.toolResult = toolResult; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Method-style accessors
    public Long id() { return id; }
    public String runId() { return runId; }
    public Integer stepIndex() { return stepIndex; }
    public AgentStepType type() { return type; }
    public String content() { return content; }
    public String toolName() { return toolName; }
    public String toolInput() { return toolInput; }
    public String toolResult() { return toolResult; }
    public LocalDateTime createdAt() { return createdAt; }
}
