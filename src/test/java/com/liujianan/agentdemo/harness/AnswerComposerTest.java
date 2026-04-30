package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.HarnessMetrics;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.llm.ModelClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerComposerTest {

    @Mock
    private ModelClient modelClient;

    @Mock
    private TraceAgent traceAgent;

    private HarnessMetrics harnessMetrics;
    private AnswerComposer answerComposer;

    @BeforeEach
    void setUp() {
        harnessMetrics = new HarnessMetrics(new SimpleMeterRegistry());
        answerComposer = new AnswerComposer(modelClient, traceAgent, harnessMetrics);
    }

    @Test
    void compose_shouldReturnModelClientAnswer() {
        when(modelClient.answer(anyString(), anyList(), anyList())).thenReturn("Test answer");
        when(modelClient.isConfigured()).thenReturn(true);

        String result = answerComposer.compose("session1", "question",
                new ArrayList<>(), new ArrayList<>(), System.currentTimeMillis(), "user1");

        assertEquals("Test answer", result);
        verify(modelClient).answer("question", new ArrayList<>(), new ArrayList<>());
        verify(traceAgent).record(eq("session1"), eq("ANSWER"), anyString(), anyMap(), eq("user1"));
    }

    @Test
    void compose_shouldRecordLlmLatencyMetric() {
        when(modelClient.answer(anyString(), anyList(), anyList())).thenReturn("Answer");
        when(modelClient.isConfigured()).thenReturn(true);

        answerComposer.compose("session1", "question",
                new ArrayList<>(), new ArrayList<>(), System.currentTimeMillis(), "user1");

        assertEquals(1, harnessMetrics.getLlmAnswerTimer().count());
    }

    @Test
    void compose_shouldTraceLatencyAndTokens() {
        when(modelClient.answer(anyString(), anyList(), anyList())).thenReturn("Answer");
        when(modelClient.isConfigured()).thenReturn(true);

        List<DocumentChunk> sources = List.of(
                new DocumentChunk(1L, "Src", "Source content for testing.",
                        List.of(), LocalDateTime.now(), "user1")
        );
        List<SessionMessage> history = List.of(
                new SessionMessage("user", "Hello", java.time.LocalDateTime.now())
        );

        long startMs = System.currentTimeMillis();
        answerComposer.compose("session1", "What is Java?", sources, history, startMs, "user1");

        verify(traceAgent).record(eq("session1"), eq("ANSWER"), eq("composed answer"),
                argThat(map ->
                        map.containsKey("latencyMs") &&
                        map.containsKey("promptTokens") &&
                        map.containsKey("llmConfigured")
                ), eq("user1"));
    }

    @Test
    void composeStream_shouldDeliverTokensAndRecordMetrics() {
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(3);
            Consumer<String> onDone = invocation.getArgument(4);
            onDelta.accept("Hello ");
            onDelta.accept("world");
            onDone.accept("Hello world");
            return null;
        }).when(modelClient).answerStream(anyString(), anyList(), anyList(),
                any(), any(), any());
        when(modelClient.isConfigured()).thenReturn(true);

        StringBuilder collected = new StringBuilder();
        AtomicReference<String> doneText = new AtomicReference<>();

        answerComposer.composeStream("session1", "Hi", new ArrayList<>(), new ArrayList<>(),
                System.currentTimeMillis(), "user1",
                delta -> collected.append(delta),
                full -> doneText.set(full),
                error -> {}
        );

        assertEquals("Hello world", collected.toString());
        assertEquals("Hello world", doneText.get());
        assertEquals(1, harnessMetrics.getLlmAnswerTimer().count());
    }

    @Test
    void composeStream_whenError_shouldRecordToolFailureAndTrace() {
        doAnswer(invocation -> {
            Consumer<String> onError = invocation.getArgument(5);
            onError.accept("Stream error occurred");
            return null;
        }).when(modelClient).answerStream(anyString(), anyList(), anyList(),
                any(), any(), any());

        List<String> errors = new ArrayList<>();
        answerComposer.composeStream("session1", "Hi", new ArrayList<>(), new ArrayList<>(),
                System.currentTimeMillis(), "user1",
                delta -> {},
                full -> {},
                error -> errors.add(error)
        );

        assertFalse(errors.isEmpty());
        assertEquals(1.0, harnessMetrics.getToolFailureCount(), 0.001);
        verify(traceAgent).record(eq("session1"), eq("ANSWER"), eq("stream failed"),
                argThat(map -> map.containsKey("error")), eq("user1"));
    }
}
