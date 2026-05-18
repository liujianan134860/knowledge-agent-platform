package com.liujianan.agentdemo.harness;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public SessionService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
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
        LocalDateTime now = LocalDateTime.now();
        messageRepository.save(new ChatMessage(null, current.getId(), userId, role, content, now));
        current.setUpdatedAt(now);
        ChatSession saved = sessionRepository.save(current);
        saved.setMessages(messages(current.getId(), userId));
        return saved;
    }

    public List<ChatSession> list(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .peek(session -> session.setMessages(messages(session.getId(), userId)))
                .toList();
    }

    public Page<ChatSession> list(String userId, Pageable pageable) {
        return sessionRepository.findByUserId(userId, pageable)
                .map(session -> {
                    session.setMessages(messages(session.getId(), userId));
                    return session;
                });
    }

    @Transactional
    public boolean delete(String sessionId, String userId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null || !session.getUserId().equals(userId)) return false;
        messageRepository.deleteByUserIdAndSessionId(userId, sessionId);
        sessionRepository.delete(session);
        return true;
    }

    public List<SessionMessage> messages(String sessionId, String userId) {
        return messageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId).stream()
                .map(ChatMessage::toSessionMessage)
                .toList();
    }
}
