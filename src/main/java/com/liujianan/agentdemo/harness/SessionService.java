package com.liujianan.agentdemo.harness;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {
    private final ChatSessionRepository sessionRepository;

    public SessionService(ChatSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public ChatSession create(String userId) {
        String id = UUID.randomUUID().toString();
        ChatSession session = new ChatSession(id, new ArrayList<>(), LocalDateTime.now(), LocalDateTime.now(), userId);
        return sessionRepository.save(session);
    }

    @Transactional
    public ChatSession append(String sessionId, String role, String content, String userId) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        ChatSession current = sessionRepository.findById(sessionId).orElse(null);
        if (current == null) {
            current = new ChatSession(sessionId, new ArrayList<>(), LocalDateTime.now(), LocalDateTime.now(), userId);
        }
        if (!current.getUserId().equals(userId)) {
            throw new IllegalArgumentException("session not found: " + sessionId);
        }
        List<SessionMessage> messages = new ArrayList<>(current.getMessages());
        messages.add(new SessionMessage(role, content, LocalDateTime.now()));
        current.setMessages(messages);
        current.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(current);
    }

    public List<ChatSession> list(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public boolean delete(String sessionId, String userId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getUserId().equals(userId)) return false;
        sessionRepository.delete(session);
        return true;
    }
}
