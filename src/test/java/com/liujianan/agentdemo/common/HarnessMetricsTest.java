package com.liujianan.agentdemo.common;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class HarnessMetricsTest {

    private MeterRegistry meterRegistry;
    private HarnessMetrics harnessMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        harnessMetrics = new HarnessMetrics(meterRegistry);
    }

    @Test
    void recordRetrieval_hit_shouldIncrementHitCounter() {
        harnessMetrics.recordRetrieval(true);
        assertEquals(1.0, harnessMetrics.getRetrievalHitCount(), 0.001);
        assertEquals(0.0, harnessMetrics.getRetrievalMissCount(), 0.001);
    }

    @Test
    void recordRetrieval_miss_shouldIncrementMissCounter() {
        harnessMetrics.recordRetrieval(false);
        assertEquals(0.0, harnessMetrics.getRetrievalHitCount(), 0.001);
        assertEquals(1.0, harnessMetrics.getRetrievalMissCount(), 0.001);
    }

    @Test
    void recordRetrieval_multipleCalls_shouldAccumulate() {
        harnessMetrics.recordRetrieval(true);
        harnessMetrics.recordRetrieval(true);
        harnessMetrics.recordRetrieval(false);

        assertEquals(2.0, harnessMetrics.getRetrievalHitCount(), 0.001);
        assertEquals(1.0, harnessMetrics.getRetrievalMissCount(), 0.001);
    }

    @Test
    void llmTimer_shouldRecordLatency() {
        Timer.Sample sample = harnessMetrics.startLlmTimer();
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        harnessMetrics.stopLlmTimer(sample);

        Timer timer = harnessMetrics.getLlmAnswerTimer();
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 5);
    }

    @Test
    void recordToolInvocation_shouldIncrementCounter() {
        harnessMetrics.recordToolInvocation();
        harnessMetrics.recordToolInvocation();
        harnessMetrics.recordToolInvocation();

        assertEquals(3.0, harnessMetrics.getToolInvokeCount(), 0.001);
    }

    @Test
    void recordToolSuccess_shouldIncrementCounter() {
        harnessMetrics.recordToolSuccess();
        harnessMetrics.recordToolSuccess();

        assertEquals(2.0, harnessMetrics.getToolSuccessCount(), 0.001);
    }

    @Test
    void recordToolFailure_shouldIncrementCounter() {
        harnessMetrics.recordToolFailure();

        assertEquals(1.0, harnessMetrics.getToolFailureCount(), 0.001);
    }

    @Test
    void recordEvaluationRun_highScore_shouldCountAsPass() {
        harnessMetrics.recordEvaluationRun(0.8);
        harnessMetrics.recordEvaluationRun(0.9);

        assertEquals(2.0, harnessMetrics.getEvaluationRunCount(), 0.001);
        double passCount = meterRegistry.get("evaluation.run.pass").counter().count();
        double failCount = meterRegistry.get("evaluation.run.fail").counter().count();
        assertEquals(2.0, passCount, 0.001);
        assertEquals(0.0, failCount, 0.001);
    }

    @Test
    void recordEvaluationRun_lowScore_shouldCountAsFail() {
        harnessMetrics.recordEvaluationRun(0.3);
        harnessMetrics.recordEvaluationRun(0.5);

        assertEquals(2.0, harnessMetrics.getEvaluationRunCount(), 0.001);
        double passCount = meterRegistry.get("evaluation.run.pass").counter().count();
        double failCount = meterRegistry.get("evaluation.run.fail").counter().count();
        assertEquals(0.0, passCount, 0.001);
        assertEquals(2.0, failCount, 0.001);
    }

    @Test
    void allMetrics_shouldBeRegisteredWithDescriptions() {
        assertNotNull(meterRegistry.get("rag.retrieval.hit").counter());
        assertNotNull(meterRegistry.get("rag.retrieval.miss").counter());
        assertNotNull(meterRegistry.get("llm.answer.latency").timer());
        assertNotNull(meterRegistry.get("tool.call.total").counter());
        assertNotNull(meterRegistry.get("tool.call.success").counter());
        assertNotNull(meterRegistry.get("tool.call.failure").counter());
        assertNotNull(meterRegistry.get("evaluation.run.total").counter());
        assertNotNull(meterRegistry.get("evaluation.run.pass").counter());
        assertNotNull(meterRegistry.get("evaluation.run.fail").counter());
    }
}
