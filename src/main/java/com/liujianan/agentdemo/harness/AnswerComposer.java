package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.AiPlatformProperties;
import com.liujianan.agentdemo.common.HarnessMetrics;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.llm.ModelClient;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AnswerComposer {
    private final ModelClient modelClient;
    private final TraceAgent traceAgent;
    private final HarnessMetrics harnessMetrics;
    private final AiPlatformProperties aiPlatformProperties;
    private final AtomicInteger consecutiveModelFailures = new AtomicInteger();
    private volatile long modelCircuitOpenUntilMs = 0;

    public AnswerComposer(ModelClient modelClient, TraceAgent traceAgent, HarnessMetrics harnessMetrics,
                          AiPlatformProperties aiPlatformProperties) {
        this.modelClient = modelClient;
        this.traceAgent = traceAgent;
        this.harnessMetrics = harnessMetrics;
        this.aiPlatformProperties = aiPlatformProperties;
    }

    public String compose(String sessionId, String question, List<DocumentChunk> sources,
                          List<SessionMessage> history, long startMs, String userId) {
        Timer.Sample sample = harnessMetrics.startLlmTimer();
        String answer = answerWithRetry(question, sources, history);
        sample.stop(harnessMetrics.getLlmAnswerTimer());
        long latency = System.currentTimeMillis() - startMs;
        int promptTokens = estimateTokens(question, sources);
        traceAgent.record(sessionId, "ANSWER", "composed answer", Map.of(
                "latencyMs", latency,
                "promptTokens", promptTokens,
                "llmConfigured", modelClient.isConfigured(),
                "promptVersion", aiPlatformProperties.getPromptVersion(),
                "retrievalVersion", aiPlatformProperties.getRetrievalVersion()
        ), userId);
        return answer;
    }

    public void composeStream(String sessionId, String question, List<DocumentChunk> sources,
                              List<SessionMessage> history, long startMs, String userId,
                              Consumer<String> onDelta, Consumer<String> onDone, Consumer<String> onError) {
        Timer.Sample sample = harnessMetrics.startLlmTimer();
        modelClient.answerStream(question, sources, history,
                onDelta,
                full -> {
                    sample.stop(harnessMetrics.getLlmAnswerTimer());
                    long latency = System.currentTimeMillis() - startMs;
                    int promptTokens = estimateTokens(question, sources);
                    traceAgent.record(sessionId, "ANSWER", "composed answer", Map.of(
                            "latencyMs", latency,
                            "promptTokens", promptTokens,
                            "llmConfigured", modelClient.isConfigured(),
                            "promptVersion", aiPlatformProperties.getPromptVersion(),
                            "retrievalVersion", aiPlatformProperties.getRetrievalVersion()
                    ), userId);
                    onDone.accept(full);
                },
                error -> {
                    String safeError = error == null || error.isBlank() ? "stream failed without error message" : error;
                    harnessMetrics.recordToolFailure();
                    traceAgent.record(sessionId, "ANSWER", "stream failed",
                            Map.of("error", safeError), userId);
                    onError.accept(safeError);
                }
        );
    }

    private int estimateTokens(String question, List<DocumentChunk> sources) {
        int chars = question.length() + sources.stream().mapToInt(source -> source.content().length()).sum();
        return Math.max(1, chars / 4);
    }

    private String answerWithRetry(String question, List<DocumentChunk> sources, List<SessionMessage> history) {
        long now = System.currentTimeMillis();
        if (now < modelCircuitOpenUntilMs) {
            harnessMetrics.recordModelCircuitOpen(true);
            throw new IllegalStateException("model circuit breaker is open");
        }
        harnessMetrics.recordModelCircuitOpen(false);
        int maxAttempts = Math.max(1, aiPlatformProperties.getModelMaxAttempts());
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String answer = modelClient.answer(question, sources, history);
                consecutiveModelFailures.set(0);
                harnessMetrics.recordModelCircuitOpen(false);
                return answer;
            } catch (RuntimeException e) {
                last = e;
            }
        }
        int failures = consecutiveModelFailures.incrementAndGet();
        if (failures >= Math.max(1, aiPlatformProperties.getModelCircuitBreakerThreshold())) {
            modelCircuitOpenUntilMs = System.currentTimeMillis() + Math.max(1000, aiPlatformProperties.getModelCircuitBreakerOpenMs());
            harnessMetrics.recordModelCircuitOpen(true);
        }
        throw last == null ? new IllegalStateException("model call failed") : last;
    }
}
