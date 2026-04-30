package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.common.HarnessMetrics;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RetrievalAgent {
    private final KnowledgeService knowledgeService;
    private final TraceAgent traceAgent;
    private final HarnessMetrics harnessMetrics;

    public RetrievalAgent(KnowledgeService knowledgeService, TraceAgent traceAgent, HarnessMetrics harnessMetrics) {
        this.knowledgeService = knowledgeService;
        this.traceAgent = traceAgent;
        this.harnessMetrics = harnessMetrics;
    }

    public List<DocumentChunk> search(String sessionId, String question, int topK, String userId) {
        List<DocumentChunk> sources = knowledgeService.search(question, topK, userId);
        harnessMetrics.recordRetrieval(!sources.isEmpty());
        traceAgent.record(sessionId, "RETRIEVAL", "retrieved knowledge chunks",
                Map.of("topK", topK, "hitCount", sources.size()), userId);
        return sources;
    }
}
