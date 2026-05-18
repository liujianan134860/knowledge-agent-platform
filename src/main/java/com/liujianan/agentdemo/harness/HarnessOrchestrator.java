package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.chat.ChatRequest;
import com.liujianan.agentdemo.chat.ChatResponse;
import com.liujianan.agentdemo.common.AiPlatformProperties;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class HarnessOrchestrator {
    private final SessionService sessionService;
    private final RetrievalAgent retrievalAgent;
    private final AnswerComposer answerComposer;
    private final TraceAgent traceAgent;
    private final AiPlatformProperties aiPlatformProperties;

    public HarnessOrchestrator(SessionService sessionService, RetrievalAgent retrievalAgent,
                               AnswerComposer answerComposer, TraceAgent traceAgent,
                               AiPlatformProperties aiPlatformProperties) {
        this.sessionService = sessionService;
        this.retrievalAgent = retrievalAgent;
        this.answerComposer = answerComposer;
        this.traceAgent = traceAgent;
        this.aiPlatformProperties = aiPlatformProperties;
    }

    public ChatResponse answer(ChatRequest request, String userId) {
        long start = System.currentTimeMillis();
        ChatSession session = sessionService.append(request.sessionId(), "user", request.question(), userId);
        traceAgent.record(session.id(), "USER_INPUT", "received user question",
                Map.of("questionLength", request.question().length()), userId);

        List<DocumentChunk> sources = buildSources(session.id(), request.question(), userId);
        traceAgent.record(session.id(), "CONTEXT_BUILD", "built prompt context",
                Map.of("sourceCount", sources.size(),
                        "promptVersion", aiPlatformProperties.getPromptVersion(),
                        "retrievalVersion", aiPlatformProperties.getRetrievalVersion()), userId);

        String answer = answerComposer.compose(session.id(), request.question(), sources, session.messages(), start, userId);
        sessionService.append(session.id(), "assistant", answer, userId);
        performQaReview(session.id(), request.question(), answer, sources, userId);

        long latency = System.currentTimeMillis() - start;
        int promptTokens = Math.max(1, (request.question().length()
                + sources.stream().mapToInt(s -> s.content().length()).sum()) / 4);

        return new ChatResponse(session.id(), answer, sources, promptTokens, latency);
    }

    public void answerStream(ChatRequest request, String userId,
                             Consumer<String> onSession,
                             Consumer<List<DocumentChunk>> onSources,
                             Consumer<String> onDelta,
                             Consumer<Long> onDone) {
        long start = System.currentTimeMillis();
        ChatSession session = sessionService.append(request.sessionId(), "user", request.question(), userId);
        traceAgent.record(session.id(), "USER_INPUT", "received user question",
                Map.of("questionLength", request.question().length()), userId);
        onSession.accept(session.id());

        List<DocumentChunk> sources = buildSources(session.id(), request.question(), userId);
        traceAgent.record(session.id(), "CONTEXT_BUILD", "built prompt context",
                Map.of("sourceCount", sources.size(),
                        "promptVersion", aiPlatformProperties.getPromptVersion(),
                        "retrievalVersion", aiPlatformProperties.getRetrievalVersion()), userId);
        onSources.accept(sources);

        StringBuilder answerBuilder = new StringBuilder();
        answerComposer.composeStream(session.id(), request.question(), sources, session.messages(), start, userId,
                delta -> {
                    answerBuilder.append(delta);
                    onDelta.accept(delta);
                },
                full -> {
                    String answer = (full == null || full.isBlank()) ? answerBuilder.toString() : full;
                    sessionService.append(session.id(), "assistant", answer, userId);
                    performQaReview(session.id(), request.question(), answer, sources, userId);
                    onDone.accept(System.currentTimeMillis() - start);
                },
                error -> {
                    String fallback = "流式回答失败：" + safeError(error);
                    sessionService.append(session.id(), "assistant", fallback, userId);
                    onDelta.accept("\n\n[" + fallback + "]");
                    onDone.accept(System.currentTimeMillis() - start);
                }
        );
    }

    private void performQaReview(String sessionId, String question, String answer,
                                 List<DocumentChunk> sources, String userId) {
        try {
            boolean retrievalHit = sources != null && !sources.isEmpty();
            boolean citationPresent = answer != null && answer.matches(".*\\[\\d+].*");
            double score = 0.0;
            score += retrievalHit ? 0.3 : 0.0;
            score += citationPresent ? 0.3 : 0.0;
            score += 0.4;
            score = Math.max(0.0, Math.min(1.0, Math.round(score * 100.0) / 100.0));
            traceAgent.record(sessionId, "QA_REVIEW", "answer quality review",
                    Map.of("retrievalHit", retrievalHit, "citationPresent", citationPresent,
                            "score", String.format("%.2f", score)), userId);
        } catch (Exception e) {
            String safeError = safeError(e);
            traceAgent.record(sessionId, "QA_REVIEW", "qa review skipped: " + safeError,
                    Map.of("error", safeError), userId);
        }
    }

    private List<DocumentChunk> buildSources(String sessionId, String question, String userId) {
        int topK = Math.max(1, Math.min(10, aiPlatformProperties.getRetrievalTopK()));
        return retrievalAgent.search(sessionId, question, topK, userId);
    }

    private String safeError(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        String message = throwable.getMessage();
        return safeError(message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message);
    }

    private String safeError(String error) {
        return error == null || error.isBlank() ? "unknown error" : error;
    }
}
