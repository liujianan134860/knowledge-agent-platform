package com.liujianan.agentdemo.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRunRequest(
        @NotBlank
        @Size(max = 300)
        String taskDescription,

        String sessionId,

        String skillId
) {
}
