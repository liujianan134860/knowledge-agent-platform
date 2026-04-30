package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final TraceEventRepository traceEventRepository;

    public RunController(TraceEventRepository traceEventRepository) {
        this.traceEventRepository = traceEventRepository;
    }

    @GetMapping
    public ApiResponse<List<RunStage>> getRun(@RequestParam String sessionId,
                                              HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        List<TraceEvent> events = traceEventRepository
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);

        List<RunStage> stages = new ArrayList<>();
        LocalDateTime prevTime = null;
        for (TraceEvent event : events) {
            long durationMs = 0;
            if (prevTime != null) {
                durationMs = Duration.between(prevTime, event.createdAt()).toMillis();
            }
            prevTime = event.createdAt();
            String summary = buildSummary(event);
            stages.add(new RunStage(event.stage(), event.message(), summary,
                    event.attributes(), event.createdAt(), durationMs));
        }
        return ApiResponse.ok(stages);
    }

    private String buildSummary(TraceEvent event) {
        Map<String, Object> attrs = event.attributes();
        if (attrs == null) return "";
        return switch (event.stage()) {
            case "USER_INPUT" ->
                    "问题长度: " + attrs.getOrDefault("questionLength", "?");
            case "RETRIEVAL" ->
                    "命中 " + attrs.getOrDefault("hitCount", "?") + " 个片段 (topK=" + attrs.getOrDefault("topK", "?") + ")";
            case "CONTEXT_BUILD" ->
                    "组装 " + attrs.getOrDefault("sourceCount", "?") + " 个来源";
            case "ANSWER" ->
                    "延迟: " + attrs.getOrDefault("latencyMs", "?") + "ms, tokens: " + attrs.getOrDefault("promptTokens", "?");
            case "QA_REVIEW" ->
                    "分数: " + attrs.getOrDefault("score", "?");
            default -> event.message();
        };
    }
}
