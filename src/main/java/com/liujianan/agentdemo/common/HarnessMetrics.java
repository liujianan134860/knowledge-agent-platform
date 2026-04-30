package com.liujianan.agentdemo.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom Micrometer metrics for RAG, LLM, Tool, and Evaluation tracking.
 */
@Component
public class HarnessMetrics {
    private final Counter retrievalHitCounter;
    private final Counter retrievalMissCounter;
    private final Timer llmAnswerTimer;
    private final Counter toolInvokeCounter;
    private final Counter toolSuccessCounter;
    private final Counter toolFailureCounter;
    private final Counter evaluationRunCounter;
    private final Counter evaluationPassCounter;
    private final Counter evaluationFailCounter;

    public HarnessMetrics(MeterRegistry registry) {
        this.retrievalHitCounter = Counter.builder("rag.retrieval.hit")
                .description("Number of retrieval attempts that returned results")
                .register(registry);
        this.retrievalMissCounter = Counter.builder("rag.retrieval.miss")
                .description("Number of retrieval attempts that returned no results")
                .register(registry);
        this.llmAnswerTimer = Timer.builder("llm.answer.latency")
                .description("Latency of LLM answer generation")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
        this.toolInvokeCounter = Counter.builder("tool.call.total")
                .description("Total number of tool invocations")
                .register(registry);
        this.toolSuccessCounter = Counter.builder("tool.call.success")
                .description("Number of successful tool invocations")
                .register(registry);
        this.toolFailureCounter = Counter.builder("tool.call.failure")
                .description("Number of failed tool invocations")
                .register(registry);
        this.evaluationRunCounter = Counter.builder("evaluation.run.total")
                .description("Total number of evaluation runs")
                .register(registry);
        this.evaluationPassCounter = Counter.builder("evaluation.run.pass")
                .description("Number of evaluation runs that passed (score >= 0.6)")
                .register(registry);
        this.evaluationFailCounter = Counter.builder("evaluation.run.fail")
                .description("Number of evaluation runs that failed (score < 0.6)")
                .register(registry);
    }

    // --- Retrieval Metrics ---

    public void recordRetrieval(boolean hit) {
        if (hit) {
            retrievalHitCounter.increment();
        } else {
            retrievalMissCounter.increment();
        }
    }

    // --- LLM Metrics ---

    public Timer.Sample startLlmTimer() {
        return Timer.start();
    }

    public void stopLlmTimer(Timer.Sample sample) {
        sample.stop(llmAnswerTimer);
    }

    // --- Tool Metrics ---

    public void recordToolInvocation() {
        toolInvokeCounter.increment();
    }

    public void recordToolSuccess() {
        toolSuccessCounter.increment();
    }

    public void recordToolFailure() {
        toolFailureCounter.increment();
    }

    // --- Evaluation Metrics ---

    public void recordEvaluationRun(double score) {
        evaluationRunCounter.increment();
        if (score >= 0.6) {
            evaluationPassCounter.increment();
        } else {
            evaluationFailCounter.increment();
        }
    }

    // --- Getters for testing/verification ---

    public Timer getLlmAnswerTimer() {
        return llmAnswerTimer;
    }

    public double getRetrievalHitCount() {
        return retrievalHitCounter.count();
    }

    public double getRetrievalMissCount() {
        return retrievalMissCounter.count();
    }

    public double getToolInvokeCount() {
        return toolInvokeCounter.count();
    }

    public double getToolSuccessCount() {
        return toolSuccessCounter.count();
    }

    public double getToolFailureCount() {
        return toolFailureCounter.count();
    }

    public double getEvaluationRunCount() {
        return evaluationRunCounter.count();
    }
}
