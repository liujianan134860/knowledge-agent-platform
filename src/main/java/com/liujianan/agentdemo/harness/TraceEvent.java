package com.liujianan.agentdemo.harness;

import java.time.LocalDateTime;
import java.util.Map;

public record TraceEvent(
        Long id,
        String sessionId,
        String stage,
        String message,
        Map<String, Object> attributes,
        LocalDateTime createdAt
) {
}
