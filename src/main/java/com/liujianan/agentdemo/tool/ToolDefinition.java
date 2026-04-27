package com.liujianan.agentdemo.tool;

public record ToolDefinition(
        String name,
        String description,
        String inputExample,
        String parameterSchema,
        String permission,
        long timeoutMs
) {
}
