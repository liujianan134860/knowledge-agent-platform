package com.liujianan.agentdemo.audit;

import com.liujianan.agentdemo.common.RequestIdFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(String userId, String action, String resourceType, String resourceId, Map<String, Object> attributes) {
        auditLogRepository.save(new AuditLog(null, userId, action, resourceType, resourceId,
                RequestIdFilter.currentRequestId(), attributes == null ? Map.of() : attributes, LocalDateTime.now()));
    }
}
