package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.chat.ChatRequest;
import com.liujianan.agentdemo.chat.ChatResponse;
import com.liujianan.agentdemo.common.HarnessMetrics;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import com.liujianan.agentdemo.llm.ModelClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HarnessOrchestratorTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private RetrievalAgent retrievalAgent;

    @Mock
    private AnswerComposer answerComposer;

    @Mock
    private TraceAgent traceAgent;

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private ModelClient modelClient;

    private HarnessMetrics harnessMetrics;
    private HarnessOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        harnessMetrics = new HarnessMetrics(new SimpleMeterRegistry());
        // Create agents with real metrics for full integration test
        RetrievalAgent realRetrievalAgent = new RetrievalAgent(knowledgeService, traceAgent, harnessMetrics);
        AnswerComposer realAnswerComposer = new AnswerComposer(modelClient, traceAgent, harnessMetrics);

        orchestrator = new HarnessOrchestrator(sessionService, realRetrievalAgent,
                realAnswerComposer, traceAgent);
    }

    @Test
    void answer_shouldExecuteFullPipeline() {
        // Arrange
        ChatSession session = new ChatSession();
        session.setId("session1");
        session.setMessages(List.of());

        DocumentChunk chunk = new DocumentChunk(1L, "Java Basics",
                "Java is a programming language.", List.of(), LocalDateTime.now(), "user1");

        when(sessionService.append(any(), eq("user"), anyString(), eq("user1")))
                .thenReturn(session);
        when(knowledgeService.search("What is Java?", 3, "user1"))
                .thenReturn(List.of(chunk));
        when(modelClient.answer(anyString(), anyList(), anyList()))
                .thenReturn("Java is a programming language. [1]");
        when(modelClient.isConfigured()).thenReturn(true);
        when(sessionService.append(any(), eq("assistant"), anyString(), eq("user1")))
                .thenReturn(session);

        // Act
        ChatRequest request = new ChatRequest("What is Java?", null);
        ChatResponse response = orchestrator.answer(request, "user1");

        // Assert
        assertNotNull(response);
        assertEquals("session1", response.sessionId());
        assertNotNull(response.answer());
        assertFalse(response.sources().isEmpty());
        assertTrue(response.latencyMs() >= 0);

        // Verify metrics were recorded
        assertTrue(harnessMetrics.getRetrievalHitCount() > 0);
        assertTrue(harnessMetrics.getLlmAnswerTimer().count() > 0);

        // Verify traces were recorded
        verify(traceAgent, atLeast(3)).record(anyString(), anyString(), anyString(), anyMap(), eq("user1"));
    }

    @Test
    void answer_withNoSources_shouldStillProduceAnswer() {
        // Arrange
        ChatSession session = new ChatSession();
        session.setId("session2");
        session.setMessages(List.of());

        when(sessionService.append(any(), eq("user"), anyString(), eq("user1")))
                .thenReturn(session);
        when(knowledgeService.search("unknown topic", 3, "user1"))
                .thenReturn(List.of());
        when(modelClient.answer(anyString(), anyList(), anyList()))
                .thenReturn("I don't have specific information about this.");
        when(modelClient.isConfigured()).thenReturn(true);
        when(sessionService.append(any(), eq("assistant"), anyString(), eq("user1")))
                .thenReturn(session);

        // Act
        ChatRequest request = new ChatRequest("unknown topic", null);
        ChatResponse response = orchestrator.answer(request, "user1");

        // Assert
        assertNotNull(response);
        assertTrue(response.sources().isEmpty());

        // Verify miss metric
        assertTrue(harnessMetrics.getRetrievalMissCount() > 0);
    }

    @Test
    void answerStream_shouldDeliverEvents() {
        // Arrange
        ChatSession session = new ChatSession();
        session.setId("session3");
        session.setMessages(List.of());

        when(sessionService.append(any(), eq("user"), anyString(), eq("user1")))
                .thenReturn(session);
        when(knowledgeService.search("Hello", 3, "user1"))
                .thenReturn(List.of());
        when(modelClient.isConfigured()).thenReturn(true);

        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(3, Consumer.class);
            Consumer<String> onDone = invocation.getArgument(4, Consumer.class);
            onDelta.accept("Hello there!");
            onDone.accept("Hello there!");
            return null;
        }).when(modelClient).answerStream(anyString(), anyList(), anyList(),
                any(), any(), any());

        // Act
        AtomicReference<String> sessionRef = new AtomicReference<>();
        StringBuilder deltaBuilder = new StringBuilder();
        AtomicReference<Long> latencyRef = new AtomicReference<>();

        orchestrator.answerStream(new ChatRequest("Hello", null), "user1",
                sessionId -> sessionRef.set(sessionId),
                sources -> {},
                delta -> deltaBuilder.append(delta),
                latency -> latencyRef.set(latency)
        );

        // Assert
        assertEquals("session3", sessionRef.get());
        assertEquals("Hello there!", deltaBuilder.toString());
        assertTrue(latencyRef.get() >= 0);
    }
}
