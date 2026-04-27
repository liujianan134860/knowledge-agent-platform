package com.liujianan.agentdemo.chat;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import com.liujianan.agentdemo.harness.ChatSession;
import com.liujianan.agentdemo.harness.SessionService;
import com.liujianan.agentdemo.harness.TraceRecorder;
import com.liujianan.agentdemo.llm.LlmClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final KnowledgeService knowledgeService;
    private final SessionService sessionService;
    private final TraceRecorder traceRecorder;
    private final LlmClient llmClient;

    public ChatService(KnowledgeService knowledgeService, SessionService sessionService, TraceRecorder traceRecorder, LlmClient llmClient) {
        this.knowledgeService = knowledgeService;
        this.sessionService = sessionService;
        this.traceRecorder = traceRecorder;
        this.llmClient = llmClient;
    }

    public ChatResponse answer(ChatRequest request) {
        long start = System.currentTimeMillis();
        ChatSession session = sessionService.append(request.sessionId(), "user", request.question());
        traceRecorder.record(session.id(), "USER_INPUT", "received user question", Map.of("questionLength", request.question().length()));

        List<DocumentChunk> sources = knowledgeService.search(request.question(), 3);
        traceRecorder.record(session.id(), "RETRIEVAL", "retrieved knowledge chunks", Map.of("topK", 3, "hitCount", sources.size()));

        traceRecorder.record(session.id(), "CONTEXT_BUILD", "built prompt context", Map.of("sourceCount", sources.size()));
        String answer = llmClient.answer(request.question(), sources);
        sessionService.append(session.id(), "assistant", answer);
        long latency = System.currentTimeMillis() - start;
        int promptTokens = estimateTokens(request.question(), sources);
        traceRecorder.record(session.id(), "ANSWER", "composed answer", Map.of("latencyMs", latency, "promptTokens", promptTokens, "llmConfigured", llmClient.isConfigured()));
        return new ChatResponse(session.id(), answer, sources, promptTokens, latency);
    }

    private int estimateTokens(String question, List<DocumentChunk> sources) {
        int chars = question.length() + sources.stream().mapToInt(source -> source.content().length()).sum();
        return Math.max(1, chars / 4);
    }
}
