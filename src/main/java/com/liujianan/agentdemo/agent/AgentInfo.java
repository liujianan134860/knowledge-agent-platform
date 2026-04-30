package com.liujianan.agentdemo.agent;

public record AgentInfo(
        String name,
        String displayName,
        String role,
        String responsibilities,
        String inputSummary,
        String outputSummary
) {
}
