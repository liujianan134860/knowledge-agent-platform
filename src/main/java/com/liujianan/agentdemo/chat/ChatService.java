package com.liujianan.agentdemo.chat;

import com.liujianan.agentdemo.harness.HarnessOrchestrator;
import com.liujianan.agentdemo.knowledge.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class ChatService {
    private final HarnessOrchestrator orchestrator;

    public ChatService(HarnessOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public ChatResponse answer(ChatRequest request, String userId) {
        return orchestrator.answer(request, userId);
    }

    public void answerStream(ChatRequest request, String userId,
                             Consumer<String> onSession,
                             Consumer<List<DocumentChunk>> onSources,
                             Consumer<String> onDelta,
                             Consumer<Long> onDone) {
        orchestrator.answerStream(request, userId, onSession, onSources, onDelta, onDone);
    }
}
