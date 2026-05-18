package com.liujianan.agentdemo.harness;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserIdAndSessionIdOrderByCreatedAtAsc(String userId, String sessionId);
    Page<ChatMessage> findByUserIdAndSessionId(String userId, String sessionId, Pageable pageable);
    long countByUserIdAndSessionId(String userId, String sessionId);
    void deleteByUserIdAndSessionId(String userId, String sessionId);
}
