package com.liujianan.agentdemo.llm;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.harness.SessionMessage;

import java.util.List;
import java.util.function.Consumer;

public interface ModelClient {
    boolean isConfigured();

    String answer(String question, List<DocumentChunk> sources, List<SessionMessage> history);

    void answerStream(String question, List<DocumentChunk> sources, List<SessionMessage> history,
                      Consumer<String> onDelta, Consumer<String> onDone, Consumer<String> onError);
}
