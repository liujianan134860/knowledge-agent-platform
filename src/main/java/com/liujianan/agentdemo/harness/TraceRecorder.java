package com.liujianan.agentdemo.harness;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TraceRecorder {
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final List<TraceEvent> events = new CopyOnWriteArrayList<>();

    public TraceEvent record(String sessionId, String stage, String message, Map<String, Object> attributes) {
        TraceEvent event = new TraceEvent(idGenerator.incrementAndGet(), sessionId, stage, message, attributes, LocalDateTime.now());
        events.add(event);
        return event;
    }

    public List<TraceEvent> list(String sessionId) {
        return events.stream()
                .filter(event -> sessionId == null || event.sessionId().equals(sessionId))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }
}
