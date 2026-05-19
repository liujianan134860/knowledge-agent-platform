package com.liujianan.agentdemo.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liujianan.agentdemo.common.ApiResponse;
import com.liujianan.agentdemo.tool.ToolRegistry;
import com.liujianan.agentdemo.tool.entity.ToolApproval;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final ReActExecutor reActExecutor;
    private final AgentRunRepository runRepository;
    private final AgentStepRepository stepRepository;
    private final ToolRegistry toolRegistry;
    private final TaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;

    public AgentController(ReActExecutor reActExecutor,
                           AgentRunRepository runRepository,
                           AgentStepRepository stepRepository,
                           ToolRegistry toolRegistry,
                           TaskExecutor taskExecutor,
                           ObjectMapper objectMapper) {
        this.reActExecutor = reActExecutor;
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.toolRegistry = toolRegistry;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/run")
    public ApiResponse<AgentRunResponse> run(@Valid @RequestBody AgentRunRequest request,
                                              HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        AgentRun run = reActExecutor.execute(request.sessionId(), request.taskDescription(), userId);
        List<AgentStep> steps = stepRepository.findByRunIdOrderByStepIndexAsc(run.getId());
        return ApiResponse.ok(AgentRunResponse.from(run, steps));
    }

    @GetMapping("/runs")
    public ApiResponse<List<AgentRunResponse>> listRuns(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        List<AgentRun> runs = runRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<AgentRunResponse> responses = runs.stream()
                .map(run -> AgentRunResponse.from(run,
                        stepRepository.findByRunIdOrderByStepIndexAsc(run.getId())))
                .toList();
        return ApiResponse.ok(responses);
    }

    @GetMapping("/runs/{runId}")
    public ApiResponse<AgentRunResponse> getRun(@PathVariable String runId,
                                                  HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        AgentRun run = runRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new IllegalArgumentException("run not found: " + runId));
        List<AgentStep> steps = stepRepository.findByRunIdOrderByStepIndexAsc(runId);
        return ApiResponse.ok(AgentRunResponse.from(run, steps));
    }

    @GetMapping("/runs/{runId}/steps")
    public ApiResponse<List<AgentStepResponse>> getSteps(@PathVariable String runId,
                                                           HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        runRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new IllegalArgumentException("run not found: " + runId));
        List<AgentStepResponse> stepResponses = stepRepository.findByRunIdOrderByStepIndexAsc(runId)
                .stream()
                .map(AgentStepResponse::from)
                .toList();
        return ApiResponse.ok(stepResponses);
    }

    @PostMapping(value = "/run/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runStream(@Valid @RequestBody AgentRunRequest request,
                                 HttpServletRequest httpRequest) {
        SseEmitter emitter = new SseEmitter(60_000L);
        emitter.onCompletion(() -> {});
        emitter.onError(t -> log.warn("SSE error: {}", t.getMessage()));
        emitter.onTimeout(() -> log.info("SSE timeout"));
        String userId = (String) httpRequest.getAttribute("userId");

        taskExecutor.execute(() -> {
            try {
                AgentRun run = reActExecutor.executeStream(
                        request.sessionId(), request.taskDescription(), userId,
                        step -> sendEvent(emitter, "step", toJson(step)),
                        approval -> sendEvent(emitter, "approval_required", toJson(approval))
                );

                List<AgentStep> steps = stepRepository.findByRunIdOrderByStepIndexAsc(run.getId());
                AgentRunResponse response = AgentRunResponse.from(run, steps);
                sendEvent(emitter, "done", toJson(response));
                emitter.complete();
            } catch (Exception e) {
                log.error("Agent stream failed", e);
                sendEvent(emitter, "error", "{\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                emitter.complete();
            }
        });
        return emitter;
    }

    @GetMapping("/approvals/pending")
    public ApiResponse<List<ToolApproval>> getPendingApprovals(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return ApiResponse.ok(toolRegistry.getPendingApprovals(userId));
    }

    @PostMapping("/approvals/{id}/approve")
    public ApiResponse<String> approve(@PathVariable Long id, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        String result = toolRegistry.executeApproved(id, userId);
        return ApiResponse.ok(result);
    }

    @PostMapping("/approvals/{id}/reject")
    public ApiResponse<String> reject(@PathVariable Long id, HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        String result = toolRegistry.rejectApproval(id, userId);
        return ApiResponse.ok(result);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            log.warn("SSE send failed: {}", e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("JSON serialization failed: {}", e.getMessage());
            return "{}";
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
