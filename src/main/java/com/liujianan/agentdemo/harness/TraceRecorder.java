package com.liujianan.agentdemo.harness;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TraceRecorder {
    private final TraceEventRepository traceRepository;

    public TraceRecorder(TraceEventRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    @Transactional
    public TraceEvent record(String sessionId, String stage, String message, Map<String, Object> attributes, String userId) {
        TraceEvent event = new TraceEvent(null, sessionId, stage, message, attributes, LocalDateTime.now(), userId);
        return traceRepository.save(event);
    }

    public List<TraceEvent> list(String sessionId, String userId) {
        if (sessionId != null) {
            return traceRepository.findByUserIdAndSessionIdOrderByCreatedAtDesc(userId, sessionId);
        }
        return traceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
