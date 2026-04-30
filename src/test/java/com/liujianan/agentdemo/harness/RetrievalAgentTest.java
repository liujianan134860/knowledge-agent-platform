package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.HarnessMetrics;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetrievalAgentTest {

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private TraceAgent traceAgent;

    private HarnessMetrics harnessMetrics;
    private RetrievalAgent retrievalAgent;

    @BeforeEach
    void setUp() {
        harnessMetrics = new HarnessMetrics(new SimpleMeterRegistry());
        retrievalAgent = new RetrievalAgent(knowledgeService, traceAgent, harnessMetrics);
    }

    @Test
    void search_withHits_shouldReturnSourcesAndRecordHitMetric() {
        List<DocumentChunk> expectedSources = List.of(
                new DocumentChunk(1L, "Test", "Content", List.of(), LocalDateTime.now(), "user1")
        );
        when(knowledgeService.search("question", 3, "user1"))
                .thenReturn(expectedSources);

        List<DocumentChunk> results = retrievalAgent.search("session1", "question", 3, "user1");

        assertEquals(1, results.size());
        assertEquals(1.0, harnessMetrics.getRetrievalHitCount(), 0.001);
        assertEquals(0.0, harnessMetrics.getRetrievalMissCount(), 0.001);
        verify(traceAgent).record(eq("session1"), eq("RETRIEVAL"), anyString(), anyMap(), eq("user1"));
    }

    @Test
    void search_withNoHits_shouldRecordMissMetric() {
        when(knowledgeService.search("question", 3, "user1"))
                .thenReturn(List.of());

        List<DocumentChunk> results = retrievalAgent.search("session1", "question", 3, "user1");

        assertTrue(results.isEmpty());
        assertEquals(0.0, harnessMetrics.getRetrievalHitCount(), 0.001);
        assertEquals(1.0, harnessMetrics.getRetrievalMissCount(), 0.001);
    }

    @Test
    void search_shouldDelegateToKnowledgeService() {
        when(knowledgeService.search("test query", 5, "user2"))
                .thenReturn(List.of());

        retrievalAgent.search("session2", "test query", 5, "user2");

        verify(knowledgeService).search("test query", 5, "user2");
    }
}
