package com.liujianan.agentdemo.harness;

import java.time.LocalDateTime;
import java.util.List;

public record ChatSession(
        String id,
        List<SessionMessage> messages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
