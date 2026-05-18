package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.knowledge.DocumentChunk;

import java.util.List;

public record HarnessRun(
        String runId,
        String sessionId,
        String userQuestion,
        List<DocumentChunk> sources,
        String finalAnswer,
        List<TraceEvent> traces
) {
}
