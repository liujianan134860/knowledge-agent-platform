package com.liujianan.agentdemo.harness;

import java.time.LocalDateTime;
import java.util.Map;

public record RunStage(
        String stage,
        String message,
        String summary,
        Map<String, Object> attributes,
        LocalDateTime createdAt,
        long durationMs
) {
}
