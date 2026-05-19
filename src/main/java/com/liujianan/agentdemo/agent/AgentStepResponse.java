package com.liujianan.agentdemo.agent;

import java.time.LocalDateTime;

public record AgentStepResponse(
        Long id,
        Integer stepIndex,
        String type,
        String content,
        String toolName,
        String toolInput,
        String toolResult,
        LocalDateTime createdAt
) {
    public static AgentStepResponse from(AgentStep step) {
        return new AgentStepResponse(
                step.getId(),
                step.getStepIndex(),
                step.getType().name(),
                step.getContent(),
                step.getToolName(),
                step.getToolInput(),
                step.getToolResult(),
                step.getCreatedAt()
        );
    }
}
