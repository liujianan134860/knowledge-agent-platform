package com.liujianan.agentdemo.harness;

import java.time.LocalDateTime;
import java.util.List;

public record SessionResponse(
        String id,
        int messageCount,
        List<SessionMessage> messages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SessionResponse from(ChatSession session) {
        return new SessionResponse(
                session.getId(),
                session.getMessages() == null ? 0 : session.getMessages().size(),
                session.getMessages(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
