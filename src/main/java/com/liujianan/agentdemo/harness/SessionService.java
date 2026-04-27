package com.liujianan.agentdemo.harness;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    private final ConcurrentHashMap<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public ChatSession create() {
        String id = UUID.randomUUID().toString();
        ChatSession session = new ChatSession(id, new ArrayList<>(), LocalDateTime.now(), LocalDateTime.now());
        sessions.put(id, session);
        return session;
    }

    public ChatSession append(String sessionId, String role, String content) {
        ChatSession current = sessions.computeIfAbsent(sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId,
                id -> new ChatSession(id, new ArrayList<>(), LocalDateTime.now(), LocalDateTime.now()));
        List<SessionMessage> messages = new ArrayList<>(current.messages());
        messages.add(new SessionMessage(role, content, LocalDateTime.now()));
        ChatSession updated = new ChatSession(current.id(), messages, current.createdAt(), LocalDateTime.now());
        sessions.put(updated.id(), updated);
        return updated;
    }

    public List<ChatSession> list() {
        return sessions.values().stream().sorted((a, b) -> b.updatedAt().compareTo(a.updatedAt())).toList();
    }
}
