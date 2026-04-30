package com.liujianan.agentdemo.harness;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TraceAgent {
    private final TraceRecorder traceRecorder;

    public TraceAgent(TraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    public TraceEvent record(String sessionId, String stage, String message, Map<String, Object> attributes, String userId) {
        return traceRecorder.record(sessionId, stage, message, attributes, userId);
    }

    public List<TraceEvent> list(String sessionId, String userId) {
        return traceRecorder.list(sessionId, userId);
    }

    public List<TraceEvent> listAll(String userId) {
        return traceRecorder.list(null, userId);
    }
}
