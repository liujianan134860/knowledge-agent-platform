package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.HarnessMetrics;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.llm.ModelClient;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class AnswerComposer {
    private final ModelClient modelClient;
    private final TraceAgent traceAgent;
    private final HarnessMetrics harnessMetrics;

    public AnswerComposer(ModelClient modelClient, TraceAgent traceAgent, HarnessMetrics harnessMetrics) {
        this.modelClient = modelClient;
        this.traceAgent = traceAgent;
        this.harnessMetrics = harnessMetrics;
    }

    public String compose(String sessionId, String question, List<DocumentChunk> sources,
                          List<SessionMessage> history, long startMs, String userId) {
        Timer.Sample sample = harnessMetrics.startLlmTimer();
        String answer = modelClient.answer(question, sources, history);
        sample.stop(harnessMetrics.getLlmAnswerTimer());
        long latency = System.currentTimeMillis() - startMs;
        int promptTokens = estimateTokens(question, sources);
        traceAgent.record(sessionId, "ANSWER", "composed answer", Map.of(
                "latencyMs", latency,
                "promptTokens", promptTokens,
                "llmConfigured", modelClient.isConfigured()
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
                            "llmConfigured", modelClient.isConfigured()
                    ), userId);
                    onDone.accept(full);
                },
                error -> {
                    harnessMetrics.recordToolFailure();
                    traceAgent.record(sessionId, "ANSWER", "stream failed",
                            Map.of("error", error), userId);
                    onError.accept(error);
                }
        );
    }

    private int estimateTokens(String question, List<DocumentChunk> sources) {
        int chars = question.length() + sources.stream().mapToInt(source -> source.content().length()).sum();
        return Math.max(1, chars / 4);
    }
}
