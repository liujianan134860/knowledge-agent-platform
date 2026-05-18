package com.liujianan.agentdemo.harness;

import java.time.LocalDateTime;
import java.util.Map;

public record TraceEventResponse(
        Long id,
        String sessionId,
        String stage,
        String message,
        Map<String, Object> attributes,
        LocalDateTime createdAt
) {
    public static TraceEventResponse from(TraceEvent event) {
        return new TraceEventResponse(
                event.getId(),
                event.getSessionId(),
                event.getStage(),
                event.getMessage(),
                event.getAttributes(),
                event.getCreatedAt()
        );
    }
}
