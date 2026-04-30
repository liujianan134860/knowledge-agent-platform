package com.liujianan.agentdemo.harness;

import com.liujianan.agentdemo.knowledge.DocumentChunk;
import com.liujianan.agentdemo.tool.ToolResult;

import java.util.List;

public record HarnessRun(
        String runId,
        String sessionId,
        String userQuestion,
        List<DocumentChunk> sources,
        List<ToolResult> toolResults,
        String finalAnswer,
        List<TraceEvent> traces
) {
}
