package com.liujianan.agentdemo.harness;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);
    Page<ChatSession> findByUserId(String userId, Pageable pageable);
}
