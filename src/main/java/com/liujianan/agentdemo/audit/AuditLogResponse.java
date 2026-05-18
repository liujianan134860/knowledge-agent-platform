package com.liujianan.agentdemo.audit;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        String userId,
        String action,
        String resourceType,
        String resourceId,
        String requestId,
        Map<String, Object> attributes,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getUserId(), log.getAction(), log.getResourceType(),
                log.getResourceId(), log.getRequestId(), log.getAttributes(), log.getCreatedAt());
    }
}
