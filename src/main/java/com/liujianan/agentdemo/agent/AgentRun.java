package com.liujianan.agentdemo.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_run")
public class AgentRun {
    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 36)
    private String sessionId;

    @Column(nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AgentRunStatus status;

    @Column(length = 50)
    private String skillId;

    @Column(nullable = false, length = 1000)
    private String taskDescription;

    @Column(columnDefinition = "TEXT")
    private String finalAnswer;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private int maxSteps = 20;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public AgentRun() {
    }

    public AgentRun(String id, String sessionId, String userId, AgentRunStatus status,
                    String taskDescription, String finalAnswer, String errorMessage,
                    int maxSteps, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = status;
        this.taskDescription = taskDescription;
        this.finalAnswer = finalAnswer;
        this.errorMessage = errorMessage;
        this.maxSteps = maxSteps;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public AgentRun(String id, String sessionId, String userId, AgentRunStatus status,
                    String taskDescription, String finalAnswer, String errorMessage,
                    int maxSteps, String skillId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = status;
        this.taskDescription = taskDescription;
        this.finalAnswer = finalAnswer;
        this.errorMessage = errorMessage;
        this.maxSteps = maxSteps;
        this.skillId = skillId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public AgentRunStatus getStatus() { return status; }
    public String getSkillId() { return skillId; }
    public String getTaskDescription() { return taskDescription; }
    public String getFinalAnswer() { return finalAnswer; }
    public String getErrorMessage() { return errorMessage; }
    public int getMaxSteps() { return maxSteps; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(String id) { this.id = id; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setStatus(AgentRunStatus status) { this.status = status; }
    public void setSkillId(String skillId) { this.skillId = skillId; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
    public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setMaxSteps(int maxSteps) { this.maxSteps = maxSteps; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Method-style accessors for record compatibility
    public String id() { return id; }
    public String skillId() { return skillId; }
    public String sessionId() { return sessionId; }
    public String userId() { return userId; }
    public AgentRunStatus status() { return status; }
    public String taskDescription() { return taskDescription; }
    public String finalAnswer() { return finalAnswer; }
    public String errorMessage() { return errorMessage; }
    public int maxSteps() { return maxSteps; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
