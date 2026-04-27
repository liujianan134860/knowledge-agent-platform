package com.liujianan.agentdemo.chat;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.knowledge.KnowledgeService;
import com.liujianan.agentdemo.harness.ChatSession;
import com.liujianan.agentdemo.harness.SessionService;
import com.liujianan.agentdemo.harness.TraceRecorder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final KnowledgeService knowledgeService;
    private final SessionService sessionService;
    private final TraceRecorder traceRecorder;

    public ChatService(KnowledgeService knowledgeService, SessionService sessionService, TraceRecorder traceRecorder) {
        this.knowledgeService = knowledgeService;
        this.sessionService = sessionService;
        this.traceRecorder = traceRecorder;
    }

    public ChatResponse answer(ChatRequest request) {
        long start = System.currentTimeMillis();
        ChatSession session = sessionService.append(request.sessionId(), "user", request.question());
        traceRecorder.record(session.id(), "USER_INPUT", "received user question", Map.of("questionLength", request.question().length()));

        List<DocumentChunk> sources = knowledgeService.search(request.question(), 3);
        traceRecorder.record(session.id(), "RETRIEVAL", "retrieved knowledge chunks", Map.of("topK", 3, "hitCount", sources.size()));

        String answer = sources.isEmpty()
                ? "No matching knowledge chunk found. Please add more documents or refine the question."
                : "Retrieved " + sources.size() + " related source chunk(s). The answer is composed from the returned context and can be replaced by a real LLM adapter.";
        sessionService.append(session.id(), "assistant", answer);
        long latency = System.currentTimeMillis() - start;
        int promptTokens = estimateTokens(request.question(), sources);
        traceRecorder.record(session.id(), "ANSWER", "composed answer", Map.of("latencyMs", latency, "promptTokens", promptTokens));
        return new ChatResponse(session.id(), answer, sources, promptTokens, latency);
    }

    private int estimateTokens(String question, List<DocumentChunk> sources) {
        int chars = question.length() + sources.stream().mapToInt(source -> source.content().length()).sum();
        return Math.max(1, chars / 4);
    }
}
