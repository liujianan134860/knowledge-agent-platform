package com.liujianan.agentdemo.chat;

import com.liujianan.agentdemo.knowledge.DocumentChunk;

import java.util.List;

public record ChatResponse(
        String sessionId,
        String answer,
        List<DocumentChunk> sources,
        int promptTokens,
        long latencyMs
) {
}
