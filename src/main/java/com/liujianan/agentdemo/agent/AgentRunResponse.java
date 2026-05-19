package com.liujianan.agentdemo.agent;

import java.time.LocalDateTime;
import java.util.List;

public record AgentRunResponse(
        String id,
        String sessionId,
        String status,
        String skillId,
        String taskDescription,
        String finalAnswer,
        String errorMessage,
        int maxSteps,
        int stepCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AgentStepResponse> steps
) {
    public static AgentRunResponse from(AgentRun run, List<AgentStep> steps) {
        return new AgentRunResponse(
                run.getId(),
                run.getSessionId(),
                run.getStatus().name(),
                run.getSkillId(),
                run.getTaskDescription(),
                run.getFinalAnswer(),
                run.getErrorMessage(),
                run.getMaxSteps(),
                steps.size(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                steps.stream().map(AgentStepResponse::from).toList()
        );
    }
}
