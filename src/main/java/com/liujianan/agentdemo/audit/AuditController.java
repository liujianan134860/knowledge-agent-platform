package com.liujianan.agentdemo.audit;

import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {
    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AuditLogResponse>> page(@RequestParam(required = false) String userId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = userId == null || userId.isBlank()
                ? auditLogRepository.findAll(pageable)
                : auditLogRepository.findByUserId(userId, pageable);
        return ApiResponse.ok(PageResponse.from(result.map(AuditLogResponse::from)));
    }
}
