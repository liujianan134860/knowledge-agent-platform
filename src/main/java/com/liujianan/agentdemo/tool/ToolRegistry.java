package com.liujianan.agentdemo.tool;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.liujianan.agentdemo.audit.AuditService;
import com.liujianan.agentdemo.harness.TraceAgent;
import com.liujianan.agentdemo.tool.entity.ToolApproval;
import com.liujianan.agentdemo.tool.entity.ToolInvocation;
import com.liujianan.agentdemo.tool.repository.ToolApprovalRepository;
import com.liujianan.agentdemo.tool.repository.ToolInvocationRepository;

@Service
public class ToolRegistry {
    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, ToolCallback> callbacks = new LinkedHashMap<>();
    private final Map<String, ToolDefinition> definitions = new LinkedHashMap<>();
    private final ToolInvocationRepository invocationRepository;
    private final ToolApprovalRepository approvalRepository;
    private final AuditService auditService;
    private final TraceAgent traceAgent;
    private final ThreadLocal<Consumer<ToolApproval>> approvalCallbackHolder = new ThreadLocal<>();

    public ToolRegistry(ObjectProvider<ToolCallbackProvider> toolCallbackProvider,
                        ToolInvocationRepository invocationRepository,
                        ToolApprovalRepository approvalRepository,
                        AuditService auditService,
                        TraceAgent traceAgent) {
        this.invocationRepository = invocationRepository;
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
        this.traceAgent = traceAgent;

        List<ToolCallbackProvider> providers = toolCallbackProvider.orderedStream().toList();
        for (ToolCallbackProvider provider : providers) {
            for (ToolCallback callback : provider.getToolCallbacks()) {
                callbacks.put(callback.getToolDefinition().name(), callback);
            }
        }
        log.info("ToolRegistry initialized with {} tools: {}", callbacks.size(), callbacks.keySet());
    }

    public String execute(String toolName, String input, String runId, String userId) {
        ToolCallback callback = callbacks.get(toolName);
        if (callback == null) {
            return "Error: unknown tool '" + toolName + "'. Available: " + callbacks.keySet();
        }

        ToolDefinition def = definitions.getOrDefault(toolName,
                ToolDefinition.of(toolName, "", ToolRiskLevel.LOW, "unknown"));
        String riskLevel = def.getRiskLevel().name();

        // Approval check for HIGH risk tools
        if (def.getRiskLevel() == ToolRiskLevel.HIGH) {
            Consumer<ToolApproval> approvalCallback = approvalCallbackHolder.get();
            if (approvalCallback != null) {
                ToolApproval approval = new ToolApproval(null, runId, -1, toolName,
                        input, "PENDING", userId, LocalDateTime.now());
                approval = approvalRepository.save(approval);
                approvalCallback.accept(approval);
                return "[WAITING_APPROVAL:" + approval.getId() + "] " + toolName + " — requires your confirmation. Use approve/reject to proceed.";
            }
            // No approval callback set — degrade to normal execution (backward compat)
            log.info("HIGH risk tool '{}' executing without approval (no callback configured)", toolName);
        }

        return doExecute(toolName, input, runId, userId, riskLevel);
    }

    private String doExecute(String toolName, String input, String runId, String userId, String riskLevel) {
        ToolCallback callback = callbacks.get(toolName);
        long start = System.currentTimeMillis();
        String result;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            result = callback.call(input);
        } catch (Exception e) {
            log.error("Tool {} execution failed", toolName, e);
            result = "Error: " + e.getMessage();
            status = "FAILED";
            errorMessage = e.getMessage();
        }

        long durationMs = System.currentTimeMillis() - start;

        // Persist tool invocation record
        try {
            ToolInvocation inv = new ToolInvocation(null, runId, userId, toolName,
                    input, result, riskLevel, status, durationMs, errorMessage,
                    LocalDateTime.now());
            invocationRepository.save(inv);
        } catch (Exception e) {
            log.warn("Failed to persist tool_invocation for {}: {}", toolName, e.getMessage());
        }

        // Audit log
        try {
            auditService.record(userId, "TOOL_CALL", "tool", toolName,
                    Map.of("runId", runId, "riskLevel", riskLevel, "status", status, "durationMs", durationMs));
        } catch (Exception e) {
            log.warn("Failed to audit tool call for {}: {}", toolName, e.getMessage());
        }

        // Trace event
        try {
            traceAgent.record(runId != null ? runId : "tool", "TOOL_CALL",
                    "Tool: " + toolName + " -> " + status + " (" + durationMs + "ms)",
                    Map.of("toolName", toolName, "status", status, "durationMs", durationMs),
                    userId);
        } catch (Exception e) {
            log.warn("Failed to trace tool call for {}: {}", toolName, e.getMessage());
        }

        return result;
    }

    public String executeApproved(Long approvalId, String userId) {
        ToolApproval approval = approvalRepository.findByIdAndUserId(approvalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));
        if (!"PENDING".equals(approval.getStatus())) {
            return "Error: approval already " + approval.getStatus();
        }
        approval.setStatus("APPROVED");
        approvalRepository.save(approval);

        ToolDefinition def = definitions.getOrDefault(approval.getToolName(),
                ToolDefinition.of(approval.getToolName(), "", ToolRiskLevel.LOW, "unknown"));
        return doExecute(approval.getToolName(), approval.getToolInput(),
                approval.getRunId(), userId, def.getRiskLevel().name());
    }

    public String rejectApproval(Long approvalId, String userId) {
        ToolApproval approval = approvalRepository.findByIdAndUserId(approvalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));
        if (!"PENDING".equals(approval.getStatus())) {
            return "Error: approval already " + approval.getStatus();
        }
        approval.setStatus("REJECTED");
        approvalRepository.save(approval);
        return "[REJECTED] Tool execution was rejected by user.";
    }

    public void setApprovalCallback(Consumer<ToolApproval> callback) {
        approvalCallbackHolder.set(callback);
    }

    public void clearApprovalCallback() {
        approvalCallbackHolder.remove();
    }

    public List<ToolApproval> getPendingApprovals(String userId) {
        return approvalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING");
    }

    public void registerDefinition(ToolDefinition def) {
        definitions.put(def.getName(), def);
    }

    public ToolDefinition getDefinition(String name) {
        return definitions.get(name);
    }

    public Map<String, ToolDefinition> getAllDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public Set<String> getToolNames() {
        return callbacks.keySet();
    }

    public boolean hasTool(String name) {
        return callbacks.containsKey(name);
    }

    public Map<String, ToolCallback> getCallbacks() {
        return callbacks;
    }
}
